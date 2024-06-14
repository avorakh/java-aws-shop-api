package dev.avorakh.shop.cdk;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

public class MyShopBackendJavaStack extends Stack {

    public static final String GET = "GET";

    public MyShopBackendJavaStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public MyShopBackendJavaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var getProductsListFunctionName = "getProductsList";
        var getProductsListFunction = new Function(
                this,
                getProductsListFunctionName,
                getLambdaFunctionProps(
                        getProductsListFunctionName,
                        "./../asset/get-products-list-function-aws.jar",
                        "dev.avorakh.shop.function.LambdaHandler::handleRequest"));
        var getProductsByIdFunctionName = "getProductsById";
        var getProductsByIdFunction = new Function(
                this,
                getProductsByIdFunctionName,
                getLambdaFunctionProps(
                        getProductsByIdFunctionName,
                        "./../asset/get-products-by-id-function-aws.jar",
                        "dev.avorakh.shop.function.LambdaHandler::handleRequest"));

        var api = createApiGateway();

        var products = api.getRoot().addResource("products");

        var getAllProducts = new LambdaIntegration(getProductsListFunction);
        products.addMethod(GET, getAllProducts);

        var productById = products.addResource("{productId}");
        var getOneIntegration = new LambdaIntegration(getProductsByIdFunction);
        productById.addMethod(GET, getOneIntegration);

        doDeployment(api);
    }

    private void doDeployment(RestApi api) {
        Deployment.Builder.create(this, "ProductServiceDevDeployment")
                .api(api)
                .stageName("dev")
                .build();
    }

    private @NotNull RestApi createApiGateway() {

        CorsOptions corsOptions = CorsOptions.builder()
                .allowOrigins(Cors.ALL_ORIGINS)
                .allowHeaders(Cors.DEFAULT_HEADERS)
                .allowMethods(Cors.ALL_METHODS)
                .statusCode(200)
                .build();

        return new RestApi(
                this,
                "productsApi",
                RestApiProps.builder()
                        .restApiName("Product Service")
                        .deploy(true)
                        .deployOptions(StageOptions.builder().stageName("dev").build())
                        .endpointTypes(List.of(EndpointType.REGIONAL))
                        .defaultCorsPreflightOptions(corsOptions)
                        .build());
    }

    private FunctionProps getLambdaFunctionProps(String functionName, String lambdaCodePath, String handler) {
        return FunctionProps.builder()
                .functionName(functionName)
                .code(Code.fromAsset(lambdaCodePath))
                .handler(handler)
                .runtime(Runtime.JAVA_21)
                .timeout(Duration.seconds(10))
                .memorySize(128)
                .ephemeralStorageSize(Size.mebibytes(512))
                .build();
    }
}
