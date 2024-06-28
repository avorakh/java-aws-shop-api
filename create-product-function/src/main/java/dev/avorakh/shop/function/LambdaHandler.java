package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.avorakh.shop.dao.*;
import dev.avorakh.shop.function.model.*;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LambdaHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    TransactionalProductDao productDao;

    public LambdaHandler() {
        var dynamoDbClient = DynamoDbClient.builder().build();
        var productTableName = System.getenv("PRODUCT_TABLE_NAME");
        var stockTableName = System.getenv("STOCK_TABLE_NAME");
        this.productDao = new DynamoDbTransactionalProductDao(dynamoDbClient, productTableName, stockTableName);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        var logger = context.getLogger();
        try {
            var inputEvent = objectMapper.writeValueAsString(input);
            logger.log("LambdaHandler call input - [%s]".formatted(inputEvent), LogLevel.INFO);

            var requestBody = input.getBody();
            if (StringUtils.isBlank(requestBody)) {
                var validationError = new ErrorDetail("the request body should be present");
                return toValidationErrorResponse(List.of(validationError));
            }

            var newProduct = objectMapper.readValue(requestBody, ProductInputResource.class);

            var validationErrors = CommonUtils.validate(newProduct);
            if (!validationErrors.isEmpty()) {
                return toValidationErrorResponse(validationErrors);
            }

            String productId = UUID.randomUUID().toString();

            productDao.create(productId, newProduct);

            var body = objectMapper.writeValueAsString(new IdResource(productId));

            return CommonUtils.toAPIGatewayV2HTTPResponse(201, body);
        } catch (Exception e) {
            logger.log(e.getMessage(), LogLevel.ERROR);

            return CommonUtils.toErrorAPIGatewayV2HTTPResponse(500, INTERNAL_SERVER_ERROR, 1000);
        }
    }

    private static APIGatewayV2HTTPResponse toValidationErrorResponse(List<ErrorDetail> validationErrors)
            throws JsonProcessingException {
        var errorResource = new ErrorResource(VALIDATION_ERROR, 1001, validationErrors);
        var body = objectMapper.writeValueAsString(errorResource);
        return CommonUtils.toAPIGatewayV2HTTPResponse(400, body);
    }
}
