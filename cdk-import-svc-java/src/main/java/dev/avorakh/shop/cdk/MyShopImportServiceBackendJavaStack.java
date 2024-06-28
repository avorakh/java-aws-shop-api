package dev.avorakh.shop.cdk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.CorsRule;
import software.amazon.awscdk.services.s3.HttpMethods;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class MyShopImportServiceBackendJavaStack extends Stack {

    public static final String APPLICATION_JSON = "application/json";

    public MyShopImportServiceBackendJavaStack(@Nullable Construct scope, @Nullable String id) {
        this(scope, id, null);
    }

    public MyShopImportServiceBackendJavaStack(@Nullable Construct scope, @Nullable String id, @Nullable StackProps props) {
        super(scope, id, props);

        String bucketName = "avorakh-my-shop-import-svc-cdk";

        var s3Bucket = createBucket(bucketName);

        String importProductsFileFunctionName = "importProductsFile";

        var importProductsFileFunction = new Function(
                this,
                importProductsFileFunctionName,
                getLambdaFunctionProps(
                        importProductsFileFunctionName,
                        "./../asset/import-resource-file-function-aws.jar",
                        "dev.avorakh.shop.function.LambdaHandler::handleRequest"));

        // Add Environment Variables to 'importProductsFile' Lambda function
        importProductsFileFunction.addEnvironment("UPLOAD_BUCKET", bucketName);
        importProductsFileFunction.addEnvironment("UPLOAD_FOLDER", "uploaded");
        importProductsFileFunction.addEnvironment("EXPIRATION_SECONDS", "600");

        s3Bucket.grantPut(importProductsFileFunction);

        var api = createApiGateway();

        var importResource = api.getRoot().addResource("import");

        var importProductsFile = new LambdaIntegration(importProductsFileFunction);

        var preSignedURLResponse = Model.Builder.create(this, "PreSignedURLResponse")
                .restApi(api)
                .schema(JsonSchema.builder()
                        .schema(JsonSchemaVersion.DRAFT4)
                        .type(JsonSchemaType.STRING)
                        .build())
                .description("Returns Pre-Signed URL to upload CSV")
                .contentType(APPLICATION_JSON)
                .build();

        importResource.addMethod("GET", importProductsFile, MethodOptions.builder()
                .methodResponses(List.of(
                        MethodResponse.builder()
                                .statusCode("200")
                                .responseModels(Map.of(APPLICATION_JSON, preSignedURLResponse))
                                .build(),
                        MethodResponse.builder()
                                .statusCode("500")
                                .responseModels(Map.of(APPLICATION_JSON, Model.ERROR_MODEL))
                                .build()))
                .requestParameters(Map.of("method.request.querystring.name", true))
                .requestValidatorOptions(RequestValidatorOptions.builder()
                        .validateRequestParameters(true)
                        .build())
                .build());
    }

    private @NotNull Bucket createBucket(String bucketName) {
        return Bucket.Builder.create(this, "CsvImportServiceBucket")
                .bucketName(bucketName)
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
