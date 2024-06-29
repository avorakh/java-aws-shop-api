package dev.avorakh.shop.dao;

import dev.avorakh.shop.function.model.ProductInputResource;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DynamoDbTransactionalProductDao implements TransactionalProductDao {

    DynamoDbClient dynamoDbClient;
    String productTableName;
    String stockTableName;

    public String create(String id, ProductInputResource productInput) {

        var productItem = Map.of(
                "id", AttributeValue.builder().s(id).build(),
                "title", AttributeValue.builder().s(productInput.getTitle()).build(),
                "description",
                        AttributeValue.builder()
                                .s(productInput.getDescription())
                                .build(),
                "price",
                        AttributeValue.builder()
                                .n(String.valueOf(productInput.getPrice()))
                                .build());

        var stockItem = Map.of(
                "product_id", AttributeValue.builder().s(id).build(),
                "count",
                        AttributeValue.builder()
                                .n(String.valueOf(productInput.getCount()))
                                .build());

        var productWriteItem = TransactWriteItem.builder()
                .put(Put.builder().tableName(productTableName).item(productItem).build())
                .build();

        var stockWriteItem = TransactWriteItem.builder()
                .put(Put.builder().tableName(stockTableName).item(stockItem).build())
                .build();

        var transactWriteItemsRequest = TransactWriteItemsRequest.builder()
                .transactItems(productWriteItem, stockWriteItem)
                .build();

        dynamoDbClient.transactWriteItems(transactWriteItemsRequest);

        return id;
    }
}
