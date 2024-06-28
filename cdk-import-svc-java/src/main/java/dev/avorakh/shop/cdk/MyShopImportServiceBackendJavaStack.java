package dev.avorakh.shop.cdk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.CorsRule;
import software.amazon.awscdk.services.s3.HttpMethods;
import software.constructs.Construct;

import java.util.List;

public class MyShopImportServiceBackendJavaStack extends Stack {

    public MyShopImportServiceBackendJavaStack(@Nullable Construct scope, @Nullable String id) {
        this(scope, id, null);
    }


    public MyShopImportServiceBackendJavaStack(@Nullable Construct scope, @Nullable String id, @Nullable StackProps props) {
        super(scope, id, props);
        var s3Bucket = createBucket();

    }

    private @NotNull Bucket createBucket() {
        return Bucket.Builder.create(this, "CsvImportServiceBucket")
                .bucketName("avorakh-my-shop-import-svc-cdk")
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .cors(List.of(CorsRule.builder()
                        .allowedMethods(List.of(HttpMethods.GET, HttpMethods.PUT))
                        .allowedOrigins(List.of("*"))
                        .allowedHeaders(List.of("*"))
                        .build()))
                .removalPolicy(RemovalPolicy.DESTROY)
                .versioned(true)
                .autoDeleteObjects(true)
                .build();
    }

    private void doDeployment(RestApi api) {
        Deployment.Builder.create(this, "ImportServiceDevDeployment")
                .api(api)
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
                "importApi",
                RestApiProps.builder()
                        .restApiName("Import Service")
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
                .timeout(Duration.seconds(20))
                .memorySize(256)
                .ephemeralStorageSize(Size.mebibytes(512))
                .build();
    }

}
