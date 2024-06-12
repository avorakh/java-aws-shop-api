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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

class LambdaHandlerTest {

    @Test
    public void testLoadEventBridgeEvent() throws IOException {
        // Given
        var classLoader = LambdaHandlerTest.class.getClassLoader();
        var objectMapper = new ObjectMapper();
        var eventJson = readFile(classLoader, "event.json");
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

    private String readFile(ClassLoader classLoader, String file) throws IOException {
        String path = classLoader.getResource(file).getPath();
        return Files.readString(Paths.get(path));
    }
}
