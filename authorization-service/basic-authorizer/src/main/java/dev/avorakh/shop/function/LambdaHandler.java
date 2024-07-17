package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.amazonaws.services.lambda.runtime.events.IamPolicyResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class LambdaHandler implements RequestHandler<APIGatewayCustomAuthorizerEvent, IamPolicyResponse> {

    public static final String BASIC = "Basic ";
    public static final String UNAUTHORIZED = "unauthorized";
    public static final String USER = "user";

    @Override
    public IamPolicyResponse handleRequest(APIGatewayCustomAuthorizerEvent event, Context context) {

        var logger = context.getLogger();

        logger.log(event.toString(), LogLevel.INFO);

        return Optional.ofNullable(event)
                .map(APIGatewayCustomAuthorizerEvent::getAuthorizationToken)
                .filter(authorizationValue -> authorizationValue.startsWith(BASIC))
                .map(str -> str.substring(BASIC.length()).trim())
                .map(encodedToken -> decodeToken(encodedToken, logger))
                .map(credentials -> credentials.split(":", 2))
                .map(credentials -> generatePolicy(event, credentials))
                .orElseThrow(() -> new RuntimeException(UNAUTHORIZED));
    }

    private IamPolicyResponse generatePolicy(APIGatewayCustomAuthorizerEvent event, String[] credentials) {
        if (credentials.length != 2) {
            return generatePolicy(USER, IamPolicyResponse.DENY, event.getMethodArn());
        }
        var expectedPassword = System.getenv(credentials[0]);
        var effect = (expectedPassword != null && expectedPassword.equals(credentials[1]))
                ? IamPolicyResponse.ALLOW
                : IamPolicyResponse.DENY;
        return generatePolicy(USER, effect, event.getMethodArn());
    }

    private String decodeToken(String encodedToken, LambdaLogger logger) {
        try {
            return new String(Base64.getDecoder().decode(encodedToken), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
            throw new RuntimeException(UNAUTHORIZED);
        }
    }

    IamPolicyResponse generatePolicy(String principalId, String effect, String resource) {
        return IamPolicyResponse.builder()
                .withPrincipalId(principalId)
                .withPolicyDocument(IamPolicyResponse.PolicyDocument.builder()
                        .withVersion(IamPolicyResponse.VERSION_2012_10_17)
                        .withStatement(List.of(IamPolicyResponse.Statement.builder()
                                .withEffect(effect)
                                .withAction(IamPolicyResponse.EXECUTE_API_INVOKE)
                                .withResource(List.of(resource))
                                .build()))
                        .build())
                .build();
    }
}
