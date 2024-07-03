package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LambdaHandler implements RequestHandler<S3Event, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JodaModule());
    }

    S3Client s3Client;
    String uploadFolder;
    String parsedFolder;

    public LambdaHandler() {
        this.s3Client = S3Client.builder().build();
        this.uploadFolder = System.getenv("UPLOAD_FOLDER");
        this.parsedFolder = System.getenv("PARSED_FOLDER");
    }

    @Override
    public String handleRequest(S3Event event, Context context) {

        var logger = context.getLogger();
        try {
            var inputEvent = objectMapper.writeValueAsString(event);

            logger.log("S3 Event - [%s]".formatted(inputEvent), LogLevel.INFO);

            var bucketName = event.getRecords().get(0).getS3().getBucket().getName();
            var key = event.getRecords().get(0).getS3().getObject().getKey();

            logger.log("Bucket: " + bucketName + ", Key: " + key, LogLevel.INFO);
            var getObjectRequest =
                    GetObjectRequest.builder().bucket(bucketName).key(key).build();

            var s3Object = s3Client.getObject(getObjectRequest);

            readCsvFile(s3Object, logger);
            moveFile(bucketName, key, key.replace(uploadFolder, parsedFolder), logger);
            return "OK";

        } catch (CsvValidationException e) {
            logger.log("Error parsing CSV: " + e.getMessage(), LogLevel.ERROR);
            return "FAILED";
        } catch (Exception e) {
            logger.log("Error: " + e.getMessage(), LogLevel.ERROR);
            return "FAILED";
        }
    }

    private void readCsvFile(ResponseInputStream<GetObjectResponse> s3Object, LambdaLogger logger)
            throws IOException, CsvValidationException {
        try (var reader = new BufferedReader(new InputStreamReader(s3Object));
                var csvReader = new CSVReader(reader)) {
            logger.log("Start CSV parsing.", LogLevel.INFO);
            String[] record;
            while ((record = csvReader.readNext()) != null) {
                logger.log("Product: " + String.join(", ", record), LogLevel.INFO);
            }
            logger.log("CSV parsing complete. Moving file to parsed directory.", LogLevel.INFO);
        }
    }

    private void moveFile(String bucketName, String sourceKey, String destinationKey, LambdaLogger logger) {
        var copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(destinationKey)
                .build();

        s3Client.copyObject(copyObjectRequest);

        logger.log("Object copied from [%s] to [%s]".formatted(sourceKey, destinationKey), LogLevel.INFO);

        var deleteObjectRequest =
                DeleteObjectRequest.builder().bucket(bucketName).key(sourceKey).build();

        s3Client.deleteObject(deleteObjectRequest);
        logger.log("Object deleted from [%s]".formatted(sourceKey), LogLevel.INFO);
    }
}
