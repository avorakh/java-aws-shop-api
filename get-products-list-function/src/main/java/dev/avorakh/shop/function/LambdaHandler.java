package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.avorakh.shop.function.model.CommonUtils;
import dev.avorakh.shop.function.model.MockData;

public class LambdaHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        var logger = context.getLogger();
        try {
            var inputEvent = objectMapper.writeValueAsString(input);

            logger.log("LambdaHandler call input - [%s]".formatted(inputEvent), LogLevel.INFO);
            var body = objectMapper.writeValueAsString(MockData.mockProductMap.values());

            return CommonUtils.toAPIGatewayV2HTTPResponse(200, body);
        } catch (JsonProcessingException e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
            return CommonUtils.toErrorAPIGatewayV2HTTPResponse(500, "INTERNAL_SERVER_ERROR", 1000);
        }
    }
}
