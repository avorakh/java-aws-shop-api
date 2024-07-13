package dev.avorakh.shop.function

import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent
import com.amazonaws.services.lambda.runtime.events.IamPolicyResponse
import dev.avorakh.shop.function.test.TestContext
import spock.lang.Shared
import spock.lang.Specification

class LambdaHandlerTest extends Specification {

    @Shared
    def username = "johndoe"
    @Shared
    def password = 'TEST_PASSWORD'
    @Shared
    def resourceArn = 'arn:aws:execute-api:eu-north-1:1111111111:abcdf6f/dev/GET/import'

    LambdaHandler handler = new LambdaHandler()

    def 'should successfully return IamPolicyResponse to Allow to resource on handleRequest if token is valid'() {

        given:
        def encodedToken = new String(Base64.encoder.encode("$username:$password".getBytes()))
        def event = APIGatewayCustomAuthorizerEvent.builder()
                .withType("TOKEN")
                .withMethodArn(resourceArn)
                .withAuthorizationToken("Basic $encodedToken")
                .build()

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        actual != null
        with(actual) {
            it.principalId == 'user'
            it.policyDocument != null
            with(it.policyDocument) {
                it.Version == IamPolicyResponse.VERSION_2012_10_17
                it.Statement != null
                it.Statement.size() == 1
                with(it.Statement[0]) {
                    it.Action == IamPolicyResponse.EXECUTE_API_INVOKE
                    it.Effect == IamPolicyResponse.ALLOW
                    it.Resource != null
                    it.Resource.size() == 1
                    with(it.Resource as List<String>) {
                        it[0] == this.getResourceArn()
                    }
                }
            }
        }
    }

    def 'should return IamPolicyResponse to DENY to resource on handleRequest if token is #invalid'(String invalid) {

        given:
        def encodedToken = new String(Base64.encoder.encode(invalid.getBytes()))
        def event = APIGatewayCustomAuthorizerEvent.builder()
                .withType("TOKEN")
                .withMethodArn(resourceArn)
                .withAuthorizationToken("Basic $encodedToken")
                .build()

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        actual != null
        with(actual) {
            it.principalId == 'user'
            it.policyDocument != null
            with(it.policyDocument) {
                it.Version == IamPolicyResponse.VERSION_2012_10_17
                it.Statement != null
                it.Statement.size() == 1
                with(it.Statement[0]) {
                    it.Action == IamPolicyResponse.EXECUTE_API_INVOKE
                    it.Effect == IamPolicyResponse.DENY
                    it.Resource != null
                    it.Resource.size() == 1
                    with(it.Resource as List<String>) {
                        it[0] == this.getResourceArn()
                    }
                }
            }
        }

        where:
        invalid << ["INVALID:INVALID", "$username:INVALID", "INVALID"]
    }


    def 'should thrown exception on handleRequest'(String token) {
        given:
        def event = APIGatewayCustomAuthorizerEvent.builder()
                .withType("TOKEN")
                .withMethodArn(resourceArn)
                .withAuthorizationToken(token)
                .build()

        when:
        handler.handleRequest(event, new TestContext())

        then:
        def ex = thrown(RuntimeException)
        ex.message == 'unauthorized'

        where:
        token << [null, "Bearer encodedToken", "Basic am9obkBleGFtcGxlLmNvbTphYmMxMjM=="]
    }
}
