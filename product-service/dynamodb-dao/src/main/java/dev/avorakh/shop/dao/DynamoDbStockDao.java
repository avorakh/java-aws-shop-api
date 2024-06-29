package dev.avorakh.shop.dao;

import dev.avorakh.shop.function.model.Stock;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbStockDao implements StockDao {

    DynamoDbTable<Stock> stockTable;

    public DynamoDbStockDao(DynamoDbClient dynamoDbClient, String tableName) {
        var dynamoDbEnhancedClient =
                DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();

        this.stockTable = dynamoDbEnhancedClient.table(tableName, DynamoDbSchemas.STOCK_TABLE_SCHEMA);
    }

    @Override
    public List<Stock> getAll() {
        return stockTable.scan().items().stream().toList();
    }

    @Override
    public Optional<Stock> get(String productId) {
        return Optional.ofNullable(
                stockTable.getItem(Stock.builder().productId(productId).build()));
    }

    @Override
    public void create(Stock newStock) {
        stockTable.putItem(newStock);
    }
}
