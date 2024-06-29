package dev.avorakh.shop.dao

import dev.avorakh.shop.function.model.Stock
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter
import spock.lang.Shared

class DynamoDbStockDaoTest extends AwsSdkV2DynamoDbLocalstackSpecification {
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
        def tableName = "Stock"
        def productId = "SomeId"
        and:
        def table = dynamoDbEnhancedClient.table(tableName, DynamoDbSchemas.STOCK_TABLE_SCHEMA)
        createTable(table, tableName)
        and:
        def dao = new DynamoDbStockDao(dynamoDbClient, tableName)

        when: 'Scan a table if the table is empty'
        def actualStocks = dao.getAll()
        then:
        actualStocks != null
        actualStocks.isEmpty()

        when: 'Get the item by using the key if the table is empty'
        def actual = dao.get(productId)
        then:
        actual != null
        actual.isEmpty()

        and:
        def newStock = new Stock(productId: productId, count: 5)

        when: 'Put the stock data into an Amazon DynamoDB table.'
        dao.create(newStock)
        then:
        noExceptionThrown()

        when: 'Scan a table after adding new item'
        def actualStocksAfterAdding = dao.getAll()
        then:
        actualStocksAfterAdding.size() == 1
        when: 'Get the item by using the key after adding new item'
        def actualStock = dao.get(productId)
        then:
        actualStock != null
        actualStock.isPresent()
        actualStock.get() == newStock
    }

    def createTable(DynamoDbTable<Stock> stockTable, String tableName) {
        stockTable.createTable({
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
