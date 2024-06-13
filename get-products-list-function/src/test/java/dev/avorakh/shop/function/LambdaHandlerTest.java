package dev.avorakh.shop.function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.avorakh.shop.function.model.MockData;
import dev.avorakh.shop.function.model.Product;
import dev.avorakh.shop.function.test.TestContext;
import dev.avorakh.shop.function.test.TestUtils;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class LambdaHandlerTest {

    @Test
    public void testGetProductsList() throws IOException {
        // Given
        var classLoader = LambdaHandlerTest.class.getClassLoader();
        var objectMapper = new ObjectMapper();
        var eventJson = TestUtils.readFile(classLoader, "event.json");
        var event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class);
        var handler = new LambdaHandler();

        // And
        int expectedStatusCode = 200;

        // When
        var actualResponse = handler.handleRequest(event, new TestContext());

        // Then
        assertNotNull(actualResponse);
        assertEquals(expectedStatusCode, actualResponse.getStatusCode());
        var actualProducts = objectMapper.readValue(actualResponse.getBody(), new TypeReference<List<Product>>() {});
        assertThat(List.copyOf(MockData.mockProductMap.values()), equalTo(actualProducts));
    }
}
