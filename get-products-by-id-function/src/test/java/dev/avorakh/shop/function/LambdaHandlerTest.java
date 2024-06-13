package dev.avorakh.shop.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.avorakh.shop.function.model.MockData;
import dev.avorakh.shop.function.model.Product;
import dev.avorakh.shop.function.test.TestContext;
import dev.avorakh.shop.function.test.TestUtils;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class LambdaHandlerTest {
    @Test
    public void testGetProductById_if_product_exists_with_id() throws IOException {
        // Given
        var classLoader = LambdaHandlerTest.class.getClassLoader();
        var objectMapper = new ObjectMapper();
        var eventJson = TestUtils.readFile(classLoader, "event_product_exist.json");
        var event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class);
        var handler = new LambdaHandler();

        // And
        int expectedStatusCode = 200;

        // When
        var actualResponse = handler.handleRequest(event, new TestContext());

        // Then
        assertNotNull(actualResponse);
        assertEquals(expectedStatusCode, actualResponse.getStatusCode());
        var actualProduct = objectMapper.readValue(actualResponse.getBody(), Product.class);
        assertEquals(MockData.product1, actualProduct);
    }

    @Test
    public void testGetProductById_if_product_not_found_by_id() throws IOException {
        // Given
        var classLoader = LambdaHandlerTest.class.getClassLoader();
        var objectMapper = new ObjectMapper();
        var eventJson = TestUtils.readFile(classLoader, "event_product_not_found.json");
        var event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class);
        var handler = new LambdaHandler();

        // And
        int expectedStatusCode = 404;
        var expectedBody = "{\"message\":\"PRODUCT_NOT_EXIST\", \"errorCode\":1100}";
        // When
        var actualResponse = handler.handleRequest(event, new TestContext());

        // Then
        assertNotNull(actualResponse);
        assertEquals(expectedStatusCode, actualResponse.getStatusCode());
        assertEquals(expectedBody, actualResponse.getBody());
        System.out.println(actualResponse.getBody());
    }
}
