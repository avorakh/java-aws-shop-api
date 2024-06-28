package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.avorakh.shop.function.model.CommonUtils;
import dev.avorakh.shop.function.model.ErrorDetail;
import dev.avorakh.shop.function.model.ErrorResource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LambdaHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    S3Presigner s3Presigner;
    String bucketName;
    String uploadFolder;
    Long expirationSeconds;

    public LambdaHandler() {
        this.s3Presigner = S3Presigner.builder().build();
        this.bucketName = System.getenv("UPLOAD_BUCKET");
        this.uploadFolder = System.getenv("UPLOAD_FOLDER");
        this.expirationSeconds = Long.parseLong(System.getenv("EXPIRATION_SECONDS"));
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        var logger = context.getLogger();
        try {
            var inputEvent = objectMapper.writeValueAsString(input);

            logger.log("LambdaHandler call input - [%s]".formatted(inputEvent), LogLevel.INFO);

            String name = input.getQueryStringParameters().get("name");
            logger.log("name - [%s]".formatted(name), LogLevel.INFO);

            if (StringUtils.isBlank(name)) {
                var validationError = new ErrorDetail("the 'name' query parameter should be present");
                var errorResource = new ErrorResource("VALIDATION_ERROR", 1002, List.of(validationError));
                var body = objectMapper.writeValueAsString(errorResource);
                return CommonUtils.toAPIGatewayV2HTTPResponse(400, body);
            }

            String key = uploadFolder + "/" + name;
            String uploadUrl = createPresignedUrl(key);

            logger.log("uploadUrl - [%s]".formatted(uploadUrl), LogLevel.INFO);

            var body = objectMapper.writeValueAsString(Map.of("uploadUrl", uploadUrl));

            return CommonUtils.toAPIGatewayV2HTTPResponse(200, body);
        } catch (Exception e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
            return CommonUtils.toErrorAPIGatewayV2HTTPResponse(500, "INTERNAL_SERVER_ERROR", 1000);
        }
    }

    private String createPresignedUrl(String key) {
        var objectRequest =
                PutObjectRequest.builder().bucket(bucketName).key(key).build();

        var presignedRequest = s3Presigner.presignPutObject(
                r -> r.putObjectRequest(objectRequest).signatureDuration(Duration.ofSeconds(expirationSeconds)));

        return presignedRequest.url().toString();
    }
}
