package dev.avorakh.shop.function

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.avorakh.shop.function.model.ProductOutputResource
import dev.avorakh.shop.function.test.TestContext
import dev.avorakh.shop.function.test.TestUtils
import dev.avorakh.shop.svc.ProductService
import spock.lang.Shared
import spock.lang.Specification

class LambdaHandlerTest extends Specification {

    @Shared
    def productId = 'someProductId'
    @Shared
    def productTitle = 'someTitle'
    @Shared
    def productDescription = 'someDescription'
    @Shared
    def productPrice = 12
    @Shared
    def productCount = 10
    @Shared
    def classLoader = LambdaHandlerTest.class.getClassLoader()
    @Shared
    def objectMapper = new ObjectMapper()

    @Shared
    ProductService productService
    LambdaHandler handler

    def setup() {
        productService = Mock()
        handler = new LambdaHandler(productService)
    }

    def "should successfully get all found products if products no exist"() {
        given:

        def eventJson = TestUtils.readFile(classLoader, "event.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        1 * productService.getAll() >> []

        then:
        actual != null
        actual.statusCode == 200
        actual.body == '[]'
    }

    def "should successfully get all found products if products exist"() {
        given:
        def eventJson = TestUtils.readFile(classLoader, "event.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        and:
        def product = new ProductOutputResource(
                id: productId,
                title: productTitle,
                description: productDescription,
                price: productPrice,
                count: productCount
        )

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        1 * productService.getAll() >> [product]

        then:
        actual != null
        actual.statusCode == 200
        and:
        actual.body != null
        with(actual.body) {
            !it.empty
            var actualProducts = objectMapper.readValue(
                    it, new TypeReference<List<ProductOutputResource>>() {})
            actualProducts.contains(product)
        }
    }

    def 'should return INTERNAL_SERVER_ERROR with 500 status code if exception is thrown'() {

        given:
        def eventJson = TestUtils.readFile(classLoader, "event.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        1 * productService.getAll() >> { throw new RuntimeException() }

        then:
        actual != null
        actual.statusCode == 500
        actual.body == '{"message":"INTERNAL_SERVER_ERROR", "errorCode":1000}'
    }

}
