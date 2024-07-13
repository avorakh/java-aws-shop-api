package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.amazonaws.services.lambda.runtime.events.IamPolicyResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class LambdaHandler implements RequestHandler<APIGatewayCustomAuthorizerEvent, IamPolicyResponse> {

    @Override
    public IamPolicyResponse handleRequest(APIGatewayCustomAuthorizerEvent event, Context context) {

        var logger = context.getLogger();

        logger.log(event.toString(), LogLevel.INFO);

        var authorizationHeader = Optional.ofNullable(event)
                .map(APIGatewayCustomAuthorizerEvent::getAuthorizationToken)
                .filter(authorizationValue -> authorizationValue.startsWith("Basic "));

        if (authorizationHeader.isEmpty()) {
            throw new RuntimeException("unauthorized");
        } else {
            String base64Credentials =
                    authorizationHeader.get().substring("Basic ".length()).trim();

            String credentials;

            try {
                credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("unauthorized");
            }

            // Split the credentials into username and password
            final String[] values = credentials.split(":", 2);
            if (values.length != 2) {
                return generatePolicy("user", IamPolicyResponse.DENY, event.getMethodArn());
            }

            String username = values[0];
            String password = values[1];

            String expectedPassword = System.getenv(username);

            if (expectedPassword != null && expectedPassword.equals(password)) {
                return generatePolicy("user", IamPolicyResponse.ALLOW, event.getMethodArn());
            } else {
                return generatePolicy("user", IamPolicyResponse.DENY, event.getMethodArn());
            }
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
