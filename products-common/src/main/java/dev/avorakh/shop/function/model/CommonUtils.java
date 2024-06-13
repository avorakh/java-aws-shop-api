package dev.avorakh.shop.function.model;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonUtils {

    public static final Map<String, String> H_CONTENT_TYPE = Map.of("Content-Type", "application/json");

    public static final String ERROR_TEMPLATE = "{\"message\":\"%s\", \"errorCode\":%d}";

    public String toErrorResponseBody(int errorCode, String message) {
        return ERROR_TEMPLATE.formatted(message, errorCode);
    }

    public APIGatewayV2HTTPResponse toAPIGatewayV2HTTPResponse(int statusCode, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(H_CONTENT_TYPE)
                .withBody(body)
                .withIsBase64Encoded(false)
                .build();
    }

    public APIGatewayV2HTTPResponse toErrorAPIGatewayV2HTTPResponse(int statusCode, String message, int errorCode) {
        return toAPIGatewayV2HTTPResponse(statusCode, toErrorResponseBody(errorCode, message));
    }
}
