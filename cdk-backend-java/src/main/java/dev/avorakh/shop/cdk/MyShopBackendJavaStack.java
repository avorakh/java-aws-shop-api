package dev.avorakh.shop.cdk;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.sns.NumericConditions;
import software.amazon.awscdk.services.sns.StringConditions;
import software.amazon.awscdk.services.sns.SubscriptionFilter;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

import static software.amazon.awscdk.services.sns.SubscriptionFilter.stringFilter;

public class MyShopBackendJavaStack extends Stack {

    public static final String GET = "GET";
    public static final String PRODUCT_TABLE_NAME = "PRODUCT_TABLE_NAME";
    public static final String PRODUCT = "Product";
    public static final String STOCK_TABLE_NAME = "STOCK_TABLE_NAME";
    public static final String STOCK = "Stock";
    public static final String JSON_P_TITLE = "title";
    public static final String JSON_P_DESCRIPTION = "description";
    public static final String JSON_P_PRICE = "price";
    public static final String JSON_P_COUNT = "count";

    public MyShopBackendJavaStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public MyShopBackendJavaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Create DynamoDB tables
        var productTable = createDynamoDBTable("ProductTable", PRODUCT, "id");
        var stockTable = createDynamoDBTable("StockTable", STOCK, "product_id");


        var getProductsListFunctionName = "getProductsList";
        var getProductsListFunction = new Function(
                this,
                getProductsListFunctionName,
                getLambdaFunctionProps(
                        getProductsListFunctionName,
                        "./../asset/get-products-list-function-aws.jar",
                        "dev.avorakh.shop.function.LambdaHandler::handleRequest"));

        // Add Environment Variables to 'getProductsList' Lambda function
        getProductsListFunction.addEnvironment(PRODUCT_TABLE_NAME, PRODUCT);
        getProductsListFunction.addEnvironment(STOCK_TABLE_NAME, STOCK);

        // Grant the 'getProductsList' Lambda function read access to the Product and Stock DynamoDB tables
        productTable.grantReadData(getProductsListFunction);
        stockTable.grantReadData(getProductsListFunction);

        var getProductsByIdFunctionName = "getProductsById";
        var getProductsByIdFunction = new Function(
                this,
                getProductsByIdFunctionName,
                getLambdaFunctionProps(
                        getProductsByIdFunctionName,
                        "./../asset/get-products-by-id-function-aws.jar",
                        "dev.avorakh.shop.function.LambdaHandler::handleRequest"));

        // Add Environment Variables to 'getProductsById' Lambda function
        getProductsByIdFunction.addEnvironment(PRODUCT_TABLE_NAME, PRODUCT);
        getProductsByIdFunction.addEnvironment(STOCK_TABLE_NAME, STOCK);

        // Grant the 'getProductsById' Lambda function read access to the Product and Stock DynamoDB tables
        productTable.grantReadData(getProductsByIdFunction);
        stockTable.grantReadData(getProductsByIdFunction);

        var createProductFunctionName = "createProduct";
        var createProductFunction = new Function(
                this,
                createProductFunctionName,
                getLambdaFunctionProps(
                        createProductFunctionName,
                        "./../asset/create-product-function-aws.jar",
                        "dev.avorakh.shop.function.LambdaHandler::handleRequest"));

        // Add Environment Variables to 'createProduct' Lambda function
        createProductFunction.addEnvironment(PRODUCT_TABLE_NAME, PRODUCT);
        createProductFunction.addEnvironment(STOCK_TABLE_NAME, STOCK);

        // Grant the 'createProduct' Lambda function read access to the Product and Stock DynamoDB tables
        productTable.grantReadWriteData(createProductFunction);
        stockTable.grantReadWriteData(createProductFunction);

        createProductAPIs(getProductsListFunction, createProductFunction, getProductsByIdFunction);

        var catalogItemsQueue = createCatalogItemsQueue();

        var snsDeadLetterQueue = Queue.Builder.create(this, "SnsDeadLetterQueue")
                .queueName("snsDeadLetterQueue")
                .build();

        var catalogBatchProcessFunctionName = "catalogBatchProcess";

        // Create an SNS topic
        var createProductTopic = Topic.Builder.create(this, "CreateProductTopic")
                .topicName("createProductTopic2")
                .build();

        // Add an email subscription to the SNS topic
        createProductTopic.addSubscription(EmailSubscription.Builder
                .create("avorakh.my.shop@yopmail.com")
                .deadLetterQueue(snsDeadLetterQueue)
                .build());

        var priceGrTh10filter = Map.of("price", SubscriptionFilter.numericFilter(
                NumericConditions.builder().greaterThan(10).build()));
        createProductTopic.addSubscription(EmailSubscription.Builder
                .create("avorakh.my.shop.price@yopmail.com")
                        .filterPolicy(priceGrTh10filter)
                .deadLetterQueue(snsDeadLetterQueue)
                .build());

        var countLess5filter = Map.of("count", SubscriptionFilter.numericFilter(
                NumericConditions.builder().lessThan(5).build()));
        createProductTopic.addSubscription(EmailSubscription.Builder
                .create("avorakh.my.shop.count@yopmail.com")
                .filterPolicy(countLess5filter)
                .deadLetterQueue(snsDeadLetterQueue)
                .build());


        var catalogBatchProcessFunction = new Function(
                this,
                catalogBatchProcessFunctionName,
                getLambdaFunctionProps(
                        catalogBatchProcessFunctionName,
                        "./../asset/catalog-batch-process-function-aws.jar",
                        "dev.avorakh.shop.function.LambdaHandler::handleRequest"));

        // Add Environment Variables to 'catalogBatchProcess' Lambda function
        catalogBatchProcessFunction.addEnvironment(PRODUCT_TABLE_NAME, PRODUCT);
        catalogBatchProcessFunction.addEnvironment(STOCK_TABLE_NAME, STOCK);
        catalogBatchProcessFunction.addEnvironment("SNS_TOPIC_ARN", createProductTopic.getTopicArn());

        // Grant the 'catalogBatchProcess' Lambda function read access to the Product and Stock DynamoDB tables
        productTable.grantReadWriteData(catalogBatchProcessFunction);
        stockTable.grantReadWriteData(catalogBatchProcessFunction);

        // Configure the SQS event source to trigger the Lambda function
        var sqsEventSource = SqsEventSource.Builder.create(catalogItemsQueue)
                .batchSize(5)
                .build();

        catalogBatchProcessFunction.addEventSource(sqsEventSource);

        createProductTopic.grantPublish(catalogBatchProcessFunction);
    }

    private @NotNull Queue createCatalogItemsQueue() {
        // Create the dead-letter queue
        var deadLetterQueue = Queue.Builder.create(this, "CatalogItemsDeadLetterQueue")
                .queueName("catalogItemsDeadLetterQueue")
                .build();

        // Create the main queue with the dead-letter queue
        return Queue.Builder.create(this, "CatalogItemsQueue")
                .queueName("catalogItemsQueue")
                .visibilityTimeout(Duration.seconds(300))
                .deadLetterQueue(DeadLetterQueue.builder()
                        .queue(deadLetterQueue)
                        .maxReceiveCount(1)
                        .build())
                .build();
    }

    private void createProductAPIs(Function getProductsListFunction, Function createProductFunction, Function getProductsByIdFunction) {
        var api = createApiGateway();

        var products = api.getRoot().addResource("products");

        var getAllProducts = new LambdaIntegration(getProductsListFunction);
        products.addMethod(GET, getAllProducts);

        var newNewProductProperties = Map.of(
                JSON_P_TITLE, JsonSchema.builder().type(JsonSchemaType.STRING).build(),
                JSON_P_DESCRIPTION, JsonSchema.builder().type(JsonSchemaType.STRING).build(),
                JSON_P_PRICE, JsonSchema.builder().type(JsonSchemaType.INTEGER).build(),
                JSON_P_COUNT, JsonSchema.builder().type(JsonSchemaType.INTEGER).build()
        );

        var userModel = api.addModel("NewProductModel",
                ModelOptions.builder().schema(JsonSchema.builder().type(JsonSchemaType.OBJECT)
                        .properties(newNewProductProperties)
                        .required(List.of(JSON_P_TITLE, JSON_P_DESCRIPTION, JSON_P_PRICE, JSON_P_COUNT))
                        .build()).build());

        var createProduct = new LambdaIntegration(createProductFunction);
        products.addMethod(
                "POST",
                createProduct,
                MethodOptions.builder().requestModels(Map.of("application/json", userModel)).build()
        );

        var productById = products.addResource("{productId}");
        var getOneIntegration = new LambdaIntegration(getProductsByIdFunction);
        productById.addMethod(GET, getOneIntegration);

        doDeployment(api);
    }

    private void doDeployment(RestApi api) {
        Deployment.Builder.create(this, "ProductServiceDevDeployment")
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
                .timeout(Duration.seconds(20))
                .memorySize(256)
                .ephemeralStorageSize(Size.mebibytes(512))
                .build();
    }

    private Table createDynamoDBTable(String tableId, String tableName, String partitionKey) {
        return Table.Builder.create(this, tableId)
                .tableName(tableName)
                .partitionKey(Attribute.builder()
                        .name(partitionKey)
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PROVISIONED)
                .readCapacity(5)
                .writeCapacity(5)
                .build();
    }
}
