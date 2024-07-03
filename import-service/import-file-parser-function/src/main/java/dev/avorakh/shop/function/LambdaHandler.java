package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LambdaHandler implements RequestHandler<S3Event, String> {

    public static final String STRING = "String";
    public static final String PRODUCT = "Product";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String[] CSV_HEADER = new String[] {"id", "title", "description", "price", "count"};
    private static final Map<Integer, String> INDEX_FIELD_MAP = Map.of(
            0, "id",
            1, "title",
            2, "description",
            3, "price",
            4, "count");

    static {
        objectMapper.registerModule(new JodaModule());
    }

    S3Client s3Client;
    SqsClient sqsClient;
    String uploadFolder;
    String parsedFolder;
    String catalogItemsQueueUrl;

    public LambdaHandler() {
        this.s3Client = S3Client.builder().build();
        this.sqsClient = SqsClient.builder().build();
        this.uploadFolder = System.getenv("UPLOAD_FOLDER");
        this.parsedFolder = System.getenv("PARSED_FOLDER");
        this.catalogItemsQueueUrl = System.getenv("CATALOG_ITEMS_QUEUE_URL");
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

            processFile(s3Object, logger);
            moveFile(bucketName, key, key.replace(uploadFolder, parsedFolder), logger);
            return "OK";

        } catch (CsvException e) {
            logger.log("Error parsing CSV: " + e.getMessage(), LogLevel.ERROR);
            return "FAILED";
        } catch (Exception e) {
            logger.log("Error: " + e.getMessage(), LogLevel.ERROR);
            return "FAILED";
        }
    }

    private void processFile(ResponseInputStream<GetObjectResponse> s3Object, LambdaLogger logger)
            throws IOException, CsvException {
        try (var reader = new BufferedReader(new InputStreamReader(s3Object));
                var csvReader = new CSVReader(reader)) {
            logger.log("Start CSV parsing.", LogLevel.INFO);
            String[] record;
            boolean isFirstLine = true;
            while ((record = csvReader.readNext()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    if (Arrays.equals(CSV_HEADER, record)) {
                        logger.log("Skip CSV header - [%s]".formatted(String.join(",", record)), LogLevel.DEBUG);
                        continue;
                    }
                }

                try {
                    var productMap = toProduct(record);
                    var productJson = objectMapper.writeValueAsString(productMap);
                    var productId = (String) productMap.getOrDefault("id", "Unknown");
                    sendRecordToSqs(productJson, productId);
                    logger.log("Product - [%s] ".formatted(productJson), LogLevel.DEBUG);
                } catch (Exception e) {
                    logger.log(
                            "Error during record [%s] processing: %s"
                                    .formatted(Arrays.toString(record), e.getMessage()),
                            LogLevel.ERROR);
                }
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

    private void sendRecordToSqs(String messageBody, String productId) {

        var productMessageAttributeValue = MessageAttributeValue.builder()
                .stringValue(productId)
                .dataType(STRING)
                .build();

        var sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(catalogItemsQueueUrl)
                .messageBody(messageBody)
                .messageAttributes(Map.of(PRODUCT, productMessageAttributeValue))
                .build();
        sqsClient.sendMessage(sendMessageRequest);
    }

    private Map<String, Object> toProduct(String[] record) {

        Map<String, Object> productMap = new HashMap<>();

        for (int i = 0; i < record.length; i++) {
            productMap.put(INDEX_FIELD_MAP.get(i), (i == 3 || i == 4) ? toInt(record[i]) : record[i]);
        }

        return productMap;
    }

    public int toInt(String param) {
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
