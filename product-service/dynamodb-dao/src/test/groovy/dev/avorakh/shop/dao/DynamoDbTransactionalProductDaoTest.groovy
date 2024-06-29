package dev.avorakh.shop.dao

import dev.avorakh.shop.function.model.Product
import dev.avorakh.shop.function.model.ProductInputResource
import dev.avorakh.shop.function.model.Stock
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter
import spock.lang.Shared

class DynamoDbTransactionalProductDaoTest extends AwsSdkV2DynamoDbLocalstackSpecification {

    @Shared
    def stockTableName = "Stock"
    @Shared
    def productTableName = "Product"
    @Shared
    def productId = 'someProductId'
    @Shared
    def productTitle = 'someTitle'
    @Shared
    def productDescription = 'someDescription'
    @Shared
    def productPrice = 12
    @Shared
    def productCount = 10

    DynamoDbTransactionalProductDao dao

    @Override
    def setup() {
        dao = new DynamoDbTransactionalProductDao(dynamoDbClient, productTableName, stockTableName)
    }

    def 'should successfully create product'() {
        given:
        def input = new ProductInputResource(
                title: productTitle,
                description: productDescription,
                price: productPrice,
                count: productCount
        )
        and:
        def dynamoDbEnhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build()
        def productTable = dynamoDbEnhancedClient.table(productTableName, DynamoDbSchemas.PRODUCT_TABLE_SCHEMA)
        createTable(productTable, productTableName)
        def stockTable = dynamoDbEnhancedClient.table(stockTableName, DynamoDbSchemas.STOCK_TABLE_SCHEMA)
        createTable(stockTable, stockTableName)

        when:
        def actual = dao.create(productId, input)
        then:
        actual != null
        actual == productId
        when:
        def actualProduct = productTable.getItem(Product.builder().id(productId).build())
        then:
        actualProduct != null
        with(actualProduct){
            it.id ==productId
            it.title== productTitle
            it.description == productDescription
            it.price== productPrice
        }
        when:
        def actualStock = stockTable.getItem(Stock.builder().productId(productId).build())
        then:
        actualStock != null
        with(actualStock){
            it.productId == actualProduct.id
            it.count == productCount
        }
    }


    def createTable(DynamoDbTable<?> productTable, String tableName) {
        productTable.createTable({
            builder ->
                builder.provisionedThroughput(b -> b
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(10L)
                        .build())
        })
        try (def waiter = DynamoDbWaiter.builder().client(dynamoDbClient).build()) {
            waiter.waitUntilTableExists({
                builder -> builder.tableName(tableName).build()
            }).matched()
        }
    }
}
