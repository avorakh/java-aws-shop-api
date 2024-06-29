package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.avorakh.shop.common.utils.ResponseCommonUtils;
import dev.avorakh.shop.dao.DynamoDbProductDao;
import dev.avorakh.shop.dao.DynamoDbStockDao;
import dev.avorakh.shop.svc.DefaultProductService;
import dev.avorakh.shop.svc.ProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LambdaHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    ProductService productService;

    public LambdaHandler() {
        var dynamoDbClient = DynamoDbClient.builder().build();
        var productTableName = System.getenv("PRODUCT_TABLE_NAME");
        var stockTableName = System.getenv("STOCK_TABLE_NAME");
        var productDao = new DynamoDbProductDao(dynamoDbClient, productTableName);
        var stockDao = new DynamoDbStockDao(dynamoDbClient, stockTableName);
        this.productService = new DefaultProductService(productDao, stockDao);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        var logger = context.getLogger();
        try {
            var inputEvent = objectMapper.writeValueAsString(input);

            logger.log("LambdaHandler call input - [%s]".formatted(inputEvent), LogLevel.INFO);

            var body = objectMapper.writeValueAsString(productService.getAll());

            return ResponseCommonUtils.toAPIGatewayV2HTTPResponse(200, body);
        } catch (Exception e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
            return ResponseCommonUtils.toErrorAPIGatewayV2HTTPResponse(500, "INTERNAL_SERVER_ERROR", 1000);
        }
    }
}
