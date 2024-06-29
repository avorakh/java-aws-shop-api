package dev.avorakh.shop.function

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.fasterxml.jackson.databind.ObjectMapper
import dev.avorakh.shop.common.utils.response.ErrorResource
import dev.avorakh.shop.dao.TransactionalProductDao

import dev.avorakh.shop.function.model.IdResource
import dev.avorakh.shop.function.model.ProductInputResource
import dev.avorakh.shop.function.test.TestContext
import dev.avorakh.shop.function.test.TestUtils
import spock.lang.Shared
import spock.lang.Specification

class LambdaHandlerTest extends Specification {

    @Shared
    def classLoader = LambdaHandlerTest.class.getClassLoader()
    @Shared
    def objectMapper = new ObjectMapper()

    @Shared
    TransactionalProductDao productDao
    LambdaHandler handler

    def setup() {
        productDao = Mock()
        handler = new LambdaHandler(productDao)
    }

    def "should successfully create product"() {
        given:
        def eventJson = TestUtils.readFile(classLoader, "event.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        1 * productDao.create(_ as String, _ as ProductInputResource)

        then:
        actual != null
        actual.statusCode == 201
        and:
        actual.body != null
        with(actual.body) {
            !it.empty
            var actualId = objectMapper.readValue(
                    it, IdResource)
            !actualId.id.empty
        }
    }

    def "should return VALIDATION_ERROR with 400 status code if request body is missed"() {
        given:
        def eventJson = TestUtils.readFile(classLoader, "event_without_body.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        0 * productDao.create(_ as String, _ as ProductInputResource)

        then:
        actual != null
        actual.statusCode == 400
        and:
        actual.body != null
        with(actual.body) {
            !it.empty
            var actualError = objectMapper.readValue(
                    it, ErrorResource)
            actualError != null
            with(actualError) {
                it.errorCode == 1001
                it.message == 'VALIDATION_ERROR'
                it.errors != null
                it.errors.size() == 1
                with(it.errors[0]) {
                    it.errorMessage == 'the request body should be present'
                }
            }
        }
    }

    def "should return VALIDATION_ERROR with 400 status code if request body is invalid"() {
        given:
        def eventJson = TestUtils.readFile(classLoader, "event_invalid.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        0 * productDao.create(_ as String, _ as ProductInputResource)

        then:
        actual != null
        actual.statusCode == 400
        and:
        actual.body != null
        with(actual.body) {
            !it.empty
            var actualError = objectMapper.readValue(
                    it, ErrorResource)
            actualError != null
            println(actualError)
            with(actualError) {
                it.errorCode == 1001
                it.message == 'VALIDATION_ERROR'
                it.errors != null
                it.errors.size() == 1
            }
        }
    }

    def 'should return INTERNAL_SERVER_ERROR with 500 status code if exception is thrown'() {

        given:
        def eventJson = TestUtils.readFile(classLoader, "event.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        1 * productDao.create(_ as String, _ as ProductInputResource) >> { throw new RuntimeException() }

        then:
        actual != null
        actual.statusCode == 500
        actual.body == '{"message":"INTERNAL_SERVER_ERROR", "errorCode":1000}'
    }
}
