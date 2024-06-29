package dev.avorakh.shop.function

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.avorakh.shop.common.utils.response.ErrorResource
import dev.avorakh.shop.localstack.LocalstackAwsSdkV2TestUtil
import dev.avorakh.shop.function.test.TestContext
import dev.avorakh.shop.function.test.TestUtils
import dev.avorakh.shop.test.spock.AbstractLocalstackSpecification
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import spock.lang.Shared

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3

class LambdaHandlerTest extends AbstractLocalstackSpecification {

    @Shared
    def bucketName = 'test-bucket'
    @Shared
    def uploadFolder = 'upload'
    @Shared
    def expirationSeconds = 100L
    @Shared
    def classLoader = LambdaHandlerTest.class.getClassLoader()
    @Shared
    def objectMapper = new ObjectMapper()

    @Shared
    S3Presigner s3Presigner

    LambdaHandler handler

    @Override
    LocalStackContainer.Service[] getLocalstackServices() {
        return new LocalStackContainer.Service[]{
                S3
        }
    }

    def setup() {
        s3Presigner = S3Presigner.builder()
                .endpointOverride(LocalstackAwsSdkV2TestUtil.toEndpointURI(this))
                .credentialsProvider(LocalstackAwsSdkV2TestUtil.toCredentialsProvider(this))
                .region(LocalstackAwsSdkV2TestUtil.toRegion(this))
                .build()

        handler = new LambdaHandler(s3Presigner, bucketName, uploadFolder, expirationSeconds)
    }

    def cleanup() {
        s3Presigner.close()
    }

    def "should successfully return Signed URL if bucket exist and name is present"() {
        given:
        def eventJson = TestUtils.readFile(classLoader, "event.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        actual != null
        actual.statusCode == 200
        actual.body != null
        with(actual.body) {
            !it.empty
            var actualBody = objectMapper.readValue(
                    it, new TypeReference<Map<String, String>>() {})
            def actualUrl = actualBody.get("uploadUrl")
            actualUrl != null
            with(actualUrl.toURL()) {
                it.getPath().endsWith "/$uploadFolder/my-product.csv"
            }
        }

    }

    def "should return VALIDATION_ERROR with 400 status code if name is missed"() {
        given:
        def eventJson = TestUtils.readFile(classLoader, "invalid_event.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

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
                it.errorCode == 1002
                it.message == 'VALIDATION_ERROR'
                it.errors != null
                it.errors.size() == 1
                with(it.errors[0]) {
                    it.errorMessage == "the 'name' query parameter should be present"
                }
            }
        }
    }

    def 'should return INTERNAL_SERVER_ERROR with 500 status code if event is bad'() {

        given:
        def eventJson = TestUtils.readFile(classLoader, "bad_event.json")
        def event = objectMapper.readValue(eventJson, APIGatewayV2HTTPEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        actual != null
        actual.statusCode == 500
        actual.body == '{"message":"INTERNAL_SERVER_ERROR", "errorCode":1000}'
    }
}
