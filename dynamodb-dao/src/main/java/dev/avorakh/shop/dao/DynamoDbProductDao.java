package dev.avorakh.shop.dao;

import dev.avorakh.shop.function.model.Product;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DynamoDbProductDao implements ProductDao {

    DynamoDbTable<Product> productTable;

    public DynamoDbProductDao(DynamoDbClient dynamoDbClient, String tableName) {
        var dynamoDbEnhancedClient =
                DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();

        this.productTable = dynamoDbEnhancedClient.table(tableName, DynamoDbSchemas.PRODUCT_TABLE_SCHEMA);
    }

    @Override
    public List<Product> getAll() {
        return productTable.scan().items().stream().toList();
    }

    @Override
    public Optional<Product> get(String id) {
        return Optional.ofNullable(productTable.getItem(Product.builder().id(id).build()));
    }

    @Override
    public void create(Product newProduct) {
        productTable.putItem(newProduct);
    }
}
