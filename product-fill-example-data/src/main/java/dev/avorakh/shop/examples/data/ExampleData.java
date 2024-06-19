package dev.avorakh.shop.examples.data;

import dev.avorakh.shop.function.model.ProductInputResource;
import java.util.Map;

public interface ExampleData {

    String productId1 = "9ec5f3e4-2a33-4b0e-915d-25cb35c7b333";
    ProductInputResource productInputId1 = ProductInputResource.builder()
            .title("Pierogi Ruskie")
            .description(
                    "Traditional Polish dumplings filled with a savory mix of potatoes, farmer's cheese, and onions, served boiled or fried with a side of sour cream.")
            .price(9)
            .count(9)
            .build();

    String productId2 = "6af9f7f2-15b4-48eb-a4c7-1a2b3f2bfcf4";
    ProductInputResource productInputId2 = ProductInputResource.builder()
            .title("Żurek")
            .description(
                    "A sour rye soup made with fermented rye flour, served with sausage, hard-boiled eggs, and often a slice of smoked bacon. Perfect for a hearty meal.")
            .price(8)
            .count(6)
            .build();

    String productId3 = "57d5bb34-9a3e-4bcb-9357-2e8e245b1c89";
    ProductInputResource productInputId3 = ProductInputResource.builder()
            .title("Bigos")
            .description(
                    "Known as hunter's stew, this dish is a flavorful combination of sauerkraut, fresh cabbage, various meats, and mushrooms, slowly simmered to perfection.")
            .price(3)
            .count(20)
            .build();

    String productId4 = "f2e8c3b4-d578-4d12-88ef-c5a5b8e4d4a9";
    ProductInputResource productInputId4 = ProductInputResource.builder()
            .title("Kiełbasa Wiejska")
            .description(
                    "A traditional Polish country sausage made from pork, garlic, and marjoram, smoked to enhance its robust flavor. Ideal for grilling or adding to stews.")
            .price(40)
            .count(2)
            .build();

    String productId5 = "a9c3b6e2-36d8-4e8e-b234-c0d3a1b9b9a1";
    ProductInputResource productInputId5 = ProductInputResource.builder()
            .title("Sernik")
            .description(
                    "A classic Polish cheesecake made with twaróg (Polish farmer's cheese), known for its rich and creamy texture with a hint of vanilla and lemon zest.")
            .price(8)
            .count(10)
            .build();

    String productId6 = "4b8f6c2d-18e4-4d6e-a8a9-c6d5a2b4b4e6";
    ProductInputResource productInputId6 = ProductInputResource.builder()
            .title("Makowiec")
            .description(
                    "A traditional Polish poppy seed roll, featuring a sweet yeast dough filled with a dense, fragrant poppy seed paste, often enjoyed during holidays.")
            .price(15)
            .count(4)
            .build();

    Map<String, ProductInputResource> productExamples = Map.of(
            productId1, productInputId1,
            productId2, productInputId2,
            productId3, productInputId3,
            productId4, productInputId4,
            productId5, productInputId5,
            productId6, productInputId6);
}
