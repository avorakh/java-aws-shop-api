package dev.avorakh.shop.dao;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import dev.avorakh.shop.function.model.Product;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public interface DynamoDbSchemas {

    TableSchema<Product> PRODUCT_TABLE_SCHEMA = TableSchema.builder(Product.class)
            .newItemSupplier(Product::new)
            .addAttribute(String.class, a -> a.name("id")
                    .getter(Product::getId)
                    .setter(Product::setId)
                    .tags(primaryPartitionKey()))
            .addAttribute(
                    String.class, a -> a.name("title").getter(Product::getTitle).setter(Product::setTitle))
            .addAttribute(
                    String.class,
                    a -> a.name("description").getter(Product::getDescription).setter(Product::setDescription))
            .addAttribute(
                    Integer.class,
                    a -> a.name("price").getter(Product::getPrice).setter(Product::setPrice))
            .build();
}
