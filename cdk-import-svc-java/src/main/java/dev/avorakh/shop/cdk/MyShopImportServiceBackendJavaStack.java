package dev.avorakh.shop.cdk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.s3.notifications.LambdaDestination;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueAttributes;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
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


        var authFn = Function.fromFunctionName(this, "basicAuthorizerLambda", "basicAuthorizer");

        var basicTokenAuthorizer = TokenAuthorizer.Builder.create(this, "basicTokenAuthorizer")
                .handler(authFn)
                .build();

        var api = createApiGateway(basicTokenAuthorizer);


        var importResource = api.getRoot().addResource("import");


        var importProductsFile = new LambdaIntegration(importProductsFileFunction);

        var preSignedURLResponse = getPreSignedURLResponse(api);

        importResource.addMethod("GET", importProductsFile, MethodOptions.builder()
                .methodResponses(getMethodResponses(preSignedURLResponse))
                .requestParameters(Map.of("method.request.querystring.name", true))
                .requestValidatorOptions(RequestValidatorOptions.builder()
                        .validateRequestParameters(true)
                        .build())
                .build());

        doDeployment(api);

        var catalogItemsQueueName = "catalogItemsQueue";
        var queueArn = getQueueArn(catalogItemsQueueName);

        // Import the existing SQS queue by its name
        var catalogItemsQueue = Queue.fromQueueAttributes(this, "ImportedQueue_catalogItemsQueue", QueueAttributes.builder()
                .queueArn(queueArn)
                .queueName(catalogItemsQueueName)
                .build());

        // The 'importFileParser' Lambda function
        String importFileParserFunctionName = "importFileParser";
        var importFileParserFunction = new Function(
                this,
                importFileParserFunctionName,
                getLambdaFunctionProps(
                        importFileParserFunctionName,
                        "./../asset/import-file-parser-function-aws.jar",
                        "dev.avorakh.shop.function.LambdaHandler::handleRequest"));

        // Add Environment Variables to 'importFileParser' Lambda function
        importFileParserFunction.addEnvironment("UPLOAD_FOLDER", "uploaded");
        importFileParserFunction.addEnvironment("PARSED_FOLDER", "parsed");
        importFileParserFunction.addEnvironment("CATALOG_ITEMS_QUEUE_URL", catalogItemsQueue.getQueueUrl());

        catalogItemsQueue.grantSendMessages(importFileParserFunction);


        // Grant the Lambda function read permissions on the bucket
        s3Bucket.grantReadWrite(importFileParserFunction);

        // Configure the S3 event to trigger the Lambda function
        s3Bucket.addEventNotification(EventType.OBJECT_CREATED_PUT, new LambdaDestination(importFileParserFunction),
                NotificationKeyFilter.builder()
                        .prefix("uploaded/")
                        .suffix(".csv")
                        .build());

    }

    private static String getQueueArn(String queueName) {
        try (var sqsClient = SqsClient.builder().build()) {
            var queueUrl = sqsClient.getQueueUrl(builder -> builder.queueName(queueName)).queueUrl();

            var response = sqsClient.getQueueAttributes(builder -> builder
                    .queueUrl(queueUrl)
                    .attributeNames(List.of(QueueAttributeName.QUEUE_ARN)));
            return response.attributes().get(QueueAttributeName.QUEUE_ARN);
        }
    }

    private @NotNull Model getPreSignedURLResponse(RestApi api) {
        return Model.Builder.create(this, "PreSignedURLResponse")
                .restApi(api)
                .schema(JsonSchema.builder()
                        .schema(JsonSchemaVersion.DRAFT4)
                        .type(JsonSchemaType.STRING)
                        .build())
                .description("Returns Pre-Signed URL to upload CSV")
                .contentType(APPLICATION_JSON)
                .build();
    }

    private @NotNull List<MethodResponse> getMethodResponses(Model preSignedURLResponse) {
        return List.of(
                MethodResponse.builder()
                        .statusCode("200")
                        .responseModels(Map.of(APPLICATION_JSON, preSignedURLResponse))
                        .build(),
                MethodResponse.builder()
                        .statusCode("401")
                        .responseModels(Map.of(APPLICATION_JSON, Model.ERROR_MODEL))
                        .build(),
                MethodResponse.builder()
                        .statusCode("403")
                        .responseModels(Map.of(APPLICATION_JSON, Model.ERROR_MODEL))
                        .build(),

                MethodResponse.builder()
                        .statusCode("500")
                        .responseModels(Map.of(APPLICATION_JSON, Model.ERROR_MODEL))
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

    private @NotNull RestApi createApiGateway(IAuthorizer authorizer) {

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
                        .defaultMethodOptions(MethodOptions.builder().authorizer(authorizer).build())
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
