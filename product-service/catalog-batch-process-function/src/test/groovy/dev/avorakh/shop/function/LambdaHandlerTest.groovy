package dev.avorakh.shop.function

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import dev.avorakh.shop.dao.TransactionalProductDao
import dev.avorakh.shop.function.test.TestContext
import dev.avorakh.shop.function.test.TestUtils
import dev.avorakh.shop.localstack.LocalstackAwsSdkV2TestUtil
import dev.avorakh.shop.test.spock.AbstractLocalstackSpecification
import groovy.json.JsonSlurper
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.policybuilder.iam.IamConditionOperator
import software.amazon.awssdk.policybuilder.iam.IamEffect
import software.amazon.awssdk.policybuilder.iam.IamPolicy
import software.amazon.awssdk.policybuilder.iam.IamPrincipalType
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sts.StsClient
import spock.lang.Shared

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*

class LambdaHandlerTest extends AbstractLocalstackSpecification {

    public static final String expectedMesssage = '{"id":"63b94d8a-878f-436d-bf89-f565d123baac","title":"Product Title","description":"Short Product Description","price":10,"count":2}'
    @Shared
    def classLoader = LambdaHandlerTest.class.getClassLoader()
    @Shared
    def objectMapper = new ObjectMapper()
    @Shared
    TransactionalProductDao productDao
    @Shared
    SnsClient snsClient

    @Shared
    StsClient stsClient
    @Shared
    SqsClient sqsClient

    LambdaHandler handler

    def setup() {
        productDao = Mock(TransactionalProductDao)

        snsClient = SnsClient.builder()
                .endpointOverride(LocalstackAwsSdkV2TestUtil.toEndpointURI(this))
                .credentialsProvider(LocalstackAwsSdkV2TestUtil.toCredentialsProvider(this))
                .region(LocalstackAwsSdkV2TestUtil.toRegion(this))
                .build()

        stsClient = StsClient.builder()
                .endpointOverride(LocalstackAwsSdkV2TestUtil.toEndpointURI(this))
                .credentialsProvider(LocalstackAwsSdkV2TestUtil.toCredentialsProvider(this))
                .region(LocalstackAwsSdkV2TestUtil.toRegion(this))
                .build()

        sqsClient = SqsClient.builder()
                .endpointOverride(LocalstackAwsSdkV2TestUtil.toEndpointURI(this))
                .credentialsProvider(LocalstackAwsSdkV2TestUtil.toCredentialsProvider(this))
                .region(LocalstackAwsSdkV2TestUtil.toRegion(this))
                .build()
    }

    def 'should successfully handle SQS event and save product'() {
        given: "Create a topic"
        def topicName = "createProductTopic1"

        and: 'Create queue to receive messages'
        def queueName = 'test-msg-queue1'

        def localstackQueueURL = createQueue(queueName)
        def queueARN = getQueueARN(localstackQueueURL)
        and: "Create a topic"
        def actualCreateTopicResponse = snsClient.createTopic({ b -> b.name(topicName) })
        def testTopicArn = actualCreateTopicResponse.topicArn()
        and: 'Subscribe the queue to the topic.'

        subscribeQueue(testTopicArn, queueARN)
        addAccessPolicyToQueuesFINAL(testTopicArn, queueARN, localstackQueueURL)

        handler = new LambdaHandler(productDao, snsClient, testTopicArn)
        and:
        def eventJson = TestUtils.readFile(classLoader, "event_with_single_message.json")
        def event = objectMapper.readValue(eventJson, SQSEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        1 * productDao.create(_, _)
        then:
        noExceptionThrown()
        actual == null

        when: 'Receive the messages from SQS'
        def actualReceiveMessageResponse = sqsClient.receiveMessage({ b ->
            b.queueUrl(localstackQueueURL)
                    .waitTimeSeconds(10)
                    .maxNumberOfMessages(5)
        })
        then:
        def actualMessages = actualReceiveMessageResponse.messages()
        with(actualMessages) {
            !it.isEmpty()
            it.size() == 1
        }
        then:
        def actualMessage = actualMessages.get(0)
        def jsonSlurper = new JsonSlurper()
        def actualMap = jsonSlurper.parseText(actualMessage.body()) as Map
        println actualMap
        actualMap.Subject == "New product"
        actualMap.MessageAttributes != null
        with(actualMap.MessageAttributes as Map) {
            it.id == ['Type': 'String', 'Value': '63b94d8a-878f-436d-bf89-f565d123baac']
            it.title == ['Type': 'String', 'Value': 'Product Title']
            it.description == ['Type': 'String', 'Value': 'Short Product Description']
            it.price == ['Type': 'Number', 'Value': '10']
            it.count == ['Type': 'Number', 'Value': '2']
        }
        actualMap.MessageAttributes.id == ['Type': 'String', 'Value': '63b94d8a-878f-436d-bf89-f565d123baac']
        actualMap.Message == expectedMesssage
    }

    def 'should successfully handle SQS event if exception was thrown on saving product'() {
        given: "Create a topic"
        def fifoTopicName = "createProductTopic2"

        and: 'Create queue to receive messages'
        def queueName = 'test-msg-queue2'

        def localstackQueueURL = createQueue(queueName)
        def queueARN = getQueueARN(localstackQueueURL)
        and: "Create a topic"
        def actualCreateTopicResponse = snsClient.createTopic({ b -> b.name(fifoTopicName) })
        def testTopicArn = actualCreateTopicResponse.topicArn()
        and: 'Subscribe the queue to the topic.'

        subscribeQueue(testTopicArn, queueARN)
        addAccessPolicyToQueuesFINAL(testTopicArn, queueARN, localstackQueueURL)

        handler = new LambdaHandler(productDao, snsClient, testTopicArn)
        and:
        def eventJson = TestUtils.readFile(classLoader, "event_with_single_message.json")
        def event = objectMapper.readValue(eventJson, SQSEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        1 * productDao.create(_, _) >> { throw new RuntimeException('Test') }

        then:
        noExceptionThrown()
        actual == null

        when: 'Receive the messages from SQS'
        def actualReceiveMessageResponse = sqsClient.receiveMessage({ b ->
            b.queueUrl(localstackQueueURL)
                    .waitTimeSeconds(10)
                    .maxNumberOfMessages(5)
        })
        then:
        def actualMessages = actualReceiveMessageResponse.messages()
        with(actualMessages) {
            it.isEmpty()
        }
    }

    def 'should successfully handle SQS event with 5 messages and save 4 products'() {
        given: "Create a topic"
        def fifoTopicName = "createProductTopic3"

        and: 'Create queue to receive messages'
        def queueName = 'test-msg-queue3'

        def localstackQueueURL = createQueue(queueName)
        def queueARN = getQueueARN(localstackQueueURL)
        and: "Create a topic"
        def actualCreateTopicResponse = snsClient.createTopic({ b -> b.name(fifoTopicName) })
        def testTopicArn = actualCreateTopicResponse.topicArn()
        and: 'Subscribe the queue to the topic.'

        subscribeQueue(testTopicArn, queueARN)
        addAccessPolicyToQueuesFINAL(testTopicArn, queueARN, localstackQueueURL)

        handler = new LambdaHandler(productDao, snsClient, testTopicArn)
        and:
        def eventJson = TestUtils.readFile(classLoader, "event_with_multi_messages.json")
        def event = objectMapper.readValue(eventJson, SQSEvent.class)

        when:
        def actual = handler.handleRequest(event, new TestContext())

        then:
        4 * productDao.create(_, _)

        then:
        noExceptionThrown()
        actual == null


        when: 'Receive the messages from SQS'
        def actualReceiveMessageResponse = sqsClient.receiveMessage({ b ->
            b.queueUrl(localstackQueueURL)
                    .waitTimeSeconds(10)
                    .maxNumberOfMessages(5)
        })
        then:
        def actualMessages = actualReceiveMessageResponse.messages()
        with(actualMessages) {
            !it.isEmpty()
            it.size() == 4
        }
    }


    def 'should successfully handle SQS event and save product'() {
        given:
        handler = new LambdaHandler(productDao, snsClient, 'someArn')
        and:
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

    @Override
    LocalStackContainer.Service[] getLocalstackServices() {
        return new LocalStackContainer.Service[]{
                SNS, SQS, STS
        }
    }

    def toAwsAccount() {
        stsClient.getCallerIdentity().account()
    }

    def createQueue(String queueName) {
        def queueAttributes = Map.of(
                QueueAttributeName.CONTENT_BASED_DEDUPLICATION, 'true'
        )

        def response = sqsClient.createQueue({ b ->
            b.queueName(queueName).attributes(queueAttributes)
        })
        return "${localstack.getEndpoint().toString()}/${toAwsAccount()}/$queueName" as String
    }

    def getQueueARN(String queueURL) {
        return sqsClient.getQueueAttributes({ b ->
            b.queueUrl(queueURL).attributeNames(QueueAttributeName.QUEUE_ARN)
        }).attributes().get(QueueAttributeName.QUEUE_ARN) as String
    }

    void addAccessPolicyToQueuesFINAL(String topicARN, String queueARN, String queueURL) {
        def account = toAwsAccount()
        def policy = IamPolicy.builder()
        // 1. Allow account user to send messages to the queue.
                .addStatement({ b ->
                    b.effect(IamEffect.ALLOW)
                            .addPrincipal(IamPrincipalType.AWS, account)
                            .addAction("SQS:*")
                            .addResource(queueARN)
                })
        // 2.  Allow the SNS FIFO topic to send messages to the queue.
                .addStatement({ b ->
                    b.effect(IamEffect.ALLOW)
                            .addPrincipal(IamPrincipalType.AWS, "*")
                            .addAction("SQS:SendMessage")
                            .addResource(queueARN)
                            .addCondition({ b1 ->
                                b1
                                        .operator(IamConditionOperator.ARN_LIKE)
                                        .key("aws:SourceArn").value(topicARN)
                            })
                })
                .build()
        def policyAttributes = Map.of(QueueAttributeName.POLICY, policy.toJson())
        sqsClient.setQueueAttributes({ b -> b.queueUrl(queueURL).attributes(policyAttributes) })
    }

    void subscribeQueue(String topicARN, String queueARN) {
        def subscribeResponse = snsClient.subscribe({ b -> b.topicArn(topicARN).endpoint(queueARN).protocol("sqs") })
    }
}
