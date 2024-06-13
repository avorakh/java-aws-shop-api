package dev.avorakh.shop.function.model;

import java.util.Map;

public interface MockData {
    String id1 = "03f42ea7-cf14-4ac4-a93b-66f3fdc60c69";
    Product product1 = Product.builder()
            .id(id1)
            .title("Some Product 1")
            .description("Some Product Description 1")
            .price(10)
            .build();

    String id2 = "47922284-ce7d-4916-87e9-8a10762cf169";
    Product product2 = Product.builder()
            .id(id2)
            .title("Some Product 2")
            .description("Some Product Description 2")
            .price(8)
            .build();
    String id3 = "1fd9b760-7cb9-4e43-b517-b7e68e191bc1";
    Product product3 = Product.builder()
            .id(id3)
            .title("Some Product 3")
            .description("Some Product Description 3")
            .price(25)
            .build();

    String id4 = "a706d7b6-d811-4e7a-8f78-059d2b025382";
    Product product4 = Product.builder()
            .id(id4)
            .title("Some Product 4")
            .description("Some Product Description 4")
            .price(10)
            .build();

    Map<String, Product> mockProductMap = Map.of(
            id1, product1,
            id2, product2,
            id3, product3,
            id4, product4);
}
