package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.avorakh.shop.function.model.MockData;
import java.util.Map;

public class LambdaHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final Map<String, String> CONTENT_TYPE = Map.of("Content-Type", "application/json");

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        var logger = context.getLogger();
        try {
            var inputEvent = objectMapper.writeValueAsString(input);

            logger.log("LambdaHandler call input - [%s]".formatted(inputEvent), LogLevel.INFO);
            var body = objectMapper.writeValueAsString(MockData.mockProductMap.values());

            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withHeaders(CONTENT_TYPE)
                    .withBody(body)
                    .withIsBase64Encoded(false)
                    .build();
        } catch (JsonProcessingException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(500)
                    .withHeaders(CONTENT_TYPE)
                    .withBody(String.format("{\"message\":\"%s\", \"errorCode\":%d}", "INTERNAL_SERVER_ERROR", 1000))
                    .withIsBase64Encoded(false)
                    .build();
        }
    }
}
