package dev.avorakh.shop.function

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import dev.avorakh.shop.dao.TransactionalProductDao
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
        productDao = Mock(TransactionalProductDao)
        handler = new LambdaHandler(productDao)
    }

    def 'should successfully handle SQS event and save product'() {
        given:
        def eventJson = TestUtils.readFile(classLoader, "event_with_single_message.json")
        def event = objectMapper.readValue(eventJson, SQSEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        1 * productDao.create(_, _)

        then:
        noExceptionThrown()
        actual == null
    }

    def 'should successfully handle SQS event if exception was thrown on saving product'() {
        given:
        def eventJson = TestUtils.readFile(classLoader, "event_with_single_message.json")
        def event = objectMapper.readValue(eventJson, SQSEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        1 * productDao.create(_, _) >> { throw new RuntimeException('Test') }

        then:
        noExceptionThrown()
        actual == null
    }

    def 'should successfully handle SQS event with 5 messages and save 4 products'() {
        given:
        def eventJson = TestUtils.readFile(classLoader, "event_with_multi_messages.json")
        def event = objectMapper.readValue(eventJson, SQSEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        4 * productDao.create(_, _)

        then:
        noExceptionThrown()
        actual == null
    }


    def 'should successfully handle SQS event and save product'() {
        given:
        def eventJson = TestUtils.readFile(classLoader, file)
        def event = objectMapper.readValue(eventJson, SQSEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        0 * productDao.create(_, _)

        then:
        noExceptionThrown()
        actual == null

        where:
        file << ['event_with_invalid_message.json', 'event_with_wrong_message.json']
    }
}
