package dev.avorakh.shop.dao

import dev.avorakh.shop.function.model.Product
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter
import spock.lang.Shared

class DynamoDbProductDaoTest extends AwsSdkV2DynamoDbLocalstackSpecification {
    @Shared
    DynamoDbEnhancedClient dynamoDbEnhancedClient

    @Override
    def setup() {
        dynamoDbEnhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build()
    }

    def "should successfully create table and verify CRUD operations"() {

        given:
        def tableName = "Product"
        def productId = "SomeId"
        and:
        def table = dynamoDbEnhancedClient.table(tableName, DynamoDbSchemas.PRODUCT_TABLE_SCHEMA)
        createTable(table, tableName)
        and:
        def dao = new DynamoDbProductDao(dynamoDbClient, tableName)

        when: 'Scan a table if the table is empty'
        def actualProducts = dao.getAll()
        then:
        actualProducts != null
        actualProducts.isEmpty()

        when: 'Get the item by using the key if the table is empty'
        def actual = dao.get(productId)
        then:
        actual != null
        actual.isEmpty()

        and:
        def newProduct = new Product(id: productId, title: "SomeTitle",
                description: "some description", price: 10)

        when: 'Put the product data into an Amazon DynamoDB table.'
        dao.create(newProduct)
        then:
        noExceptionThrown()

        when: 'Scan a table after adding new item'
        def actualProductsAfterAdding = dao.getAll()
        then:
        actualProductsAfterAdding.size() == 1
        when: 'Get the item by using the key after adding new item'
        def actualProduct = dao.get(productId)
        then:
        actualProduct != null
        actualProduct.isPresent()
        actualProduct.get() == newProduct
    }

    def createTable(DynamoDbTable<Product> productTable, String tableName) {
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
