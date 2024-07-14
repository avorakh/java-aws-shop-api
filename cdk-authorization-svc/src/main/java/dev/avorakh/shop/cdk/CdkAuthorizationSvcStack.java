package dev.avorakh.shop.cdk;

import io.github.cdimascio.dotenv.Dotenv;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

public class CdkAuthorizationSvcStack extends Stack {
    public CdkAuthorizationSvcStack(@Nullable Construct scope, @Nullable String id) {
        this(scope, id, null);
    }

    public CdkAuthorizationSvcStack(@Nullable Construct scope, @Nullable String id, @Nullable StackProps props) {
        super(scope, id, props);


        String basicAuthorizerFunctionName = "basicAuthorizer";

        var basicAuthorizerFunction = new Function(
                this,
                basicAuthorizerFunctionName,
                getLambdaFunctionProps(
                        basicAuthorizerFunctionName,
                        "./../asset/basic-authorizer-aws.jar",
                        "dev.avorakh.shop.function.LambdaHandler::handleRequest"));

        // Add Environment Variables to 'importProductsFile' Lambda function
        var dotenv = Dotenv.configure()
                .systemProperties()
                .directory("./asset")
                .load();

        dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)
                .forEach(entry -> basicAuthorizerFunction.addEnvironment(entry.getKey(), entry.getValue()));

        basicAuthorizerFunction.grantInvoke(new ServicePrincipal("apigateway.amazonaws.com"));
    }

    private FunctionProps getLambdaFunctionProps(String functionName, String lambdaCodePath, String handler) {
        return FunctionProps.builder()
                .functionName(functionName)
                .code(Code.fromAsset(lambdaCodePath))
                .handler(handler)
                .runtime(Runtime.JAVA_21)
                .timeout(Duration.seconds(20))
                .memorySize(256)
                .ephemeralStorageSize(Size.mebibytes(512))
                .build();
    }
}
