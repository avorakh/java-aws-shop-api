package dev.avorakh.shop.examples.data;

import dev.avorakh.shop.dao.DynamoDbProductDao;
import dev.avorakh.shop.dao.DynamoDbStockDao;
import dev.avorakh.shop.svc.DefaultProductService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class Application {

    public static void main(String[] args) {

        var productTableName = "Product";
        var stockTableName = "Stock";

        try (var dynamoDbClient = DynamoDbClient.builder().build()) {
            var productDao = new DynamoDbProductDao(dynamoDbClient, productTableName);
            var stockDao = new DynamoDbStockDao(dynamoDbClient, stockTableName);

            var productService = new DefaultProductService(productDao, stockDao);

            for (var productEntry : ExampleData.productExamples.entrySet()) {
                productService.create(productEntry.getKey(), productEntry.getValue());
            }
        }
    }
}
