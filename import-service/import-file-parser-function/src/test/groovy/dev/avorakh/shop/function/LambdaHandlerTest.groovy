package dev.avorakh.shop.function

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import dev.avorakh.shop.function.test.TestContext
import dev.avorakh.shop.function.test.TestUtils
import dev.avorakh.shop.localstack.LocalstackAwsSdkV2TestUtil
import dev.avorakh.shop.test.spock.AbstractLocalstackSpecification
import groovy.json.JsonSlurper
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sts.StsClient
import spock.lang.Shared

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*

class LambdaHandlerTest extends AbstractLocalstackSpecification {

    @Shared
    def bucketName = 'test-bucket'
    @Shared
    def uploadFolder = 'uploaded'
    @Shared
    def parsedFolder = 'parsed'
    @Shared
    def classLoader = LambdaHandlerTest.class.getClassLoader()
    @Shared
    ObjectMapper objectMapper

    @Shared
    S3Client s3Client
    @Shared
    SqsClient sqsClient
    @Shared
    StsClient stsClient

    @Override
    LocalStackContainer.Service[] getLocalstackServices() {
        return new LocalStackContainer.Service[]{
                S3, SQS, STS
        }
    }

    def setup() {
        objectMapper = new ObjectMapper()
        objectMapper.registerModule(new JodaModule())

        s3Client = S3Client.builder()
                .endpointOverride(LocalstackAwsSdkV2TestUtil.toEndpointURI(this))
                .credentialsProvider(LocalstackAwsSdkV2TestUtil.toCredentialsProvider(this))
                .region(LocalstackAwsSdkV2TestUtil.toRegion(this))
                .build()

        sqsClient = SqsClient.builder()
                .endpointOverride(LocalstackAwsSdkV2TestUtil.toEndpointURI(this))
                .credentialsProvider(LocalstackAwsSdkV2TestUtil.toCredentialsProvider(this))
                .region(LocalstackAwsSdkV2TestUtil.toRegion(this))
                .build()

        stsClient = StsClient.builder()
                .endpointOverride(LocalstackAwsSdkV2TestUtil.toEndpointURI(this))
                .credentialsProvider(LocalstackAwsSdkV2TestUtil.toCredentialsProvider(this))
                .region(LocalstackAwsSdkV2TestUtil.toRegion(this))
                .build()
    }

    def cleanup() {
        s3Client.close()
    }

    def 'should successfully handle S3 event'() {
        given: '0. Create a standard queue'
        def queueName = 'catalogItemsQueue'
        def catalogItemsQueueUrl = createQueue(queueName)
        def productMsgAttribute = "Product"

        def productId = '6540d4b3-812a-48a2-bba6-a6251cab752a'
        def productMessageAttributeValue = MessageAttributeValue.builder()
                .stringValue(productId)
                .dataType("String")
                .build()
        def productMessageAttributes = ["Product": productMessageAttributeValue]
        and:
        def handler = new LambdaHandler(s3Client, sqsClient, uploadFolder, parsedFolder, catalogItemsQueueUrl)
        and:
        def fileName = 'test.csv'
        def key = "$uploadFolder/$fileName"
        def bucketEntity = new S3EventNotification.S3BucketEntity(
                bucketName, null, "arn:aws:s3:::$bucketName"
        )
        def s3Entity = new S3EventNotification.S3Entity(
                "ZDEwMDUwOTAtMzdmOS00MDAwLWI4Y2YtMDEyNmMyYmNjZjkx", bucketEntity, new S3EventNotification.S3ObjectEntity(key, 85, 'eTag', 'versionId', 'sequencer'),
                "1.0"
        )
        def record = new S3EventNotification.S3EventNotificationRecord("eu-north-1", "ObjectCreated:Put", "aws:s3",
                '2024-06-29T14:12:56.381694', "2.1", null, null, s3Entity, null)

        and: 'O. Create S3 bucket and add the file'
        def csvContent = TestUtils.readFile(classLoader, fileName)
        s3Client.createBucket({
            b -> b.bucket(bucketName)
        })
        s3Client.putObject({
            b -> b.bucket(bucketName).key("$uploadFolder/$fileName")
        }, RequestBody.fromBytes(csvContent.getBytes()))

        when: '1. handle event and send a message to SQS'
        def actual = handler.handleRequest(new S3Event([record]), new TestContext())

        then:
        actual == "OK"

        when: '2. receive a message from SQS'
        def actualReceiveMessageResponse = receiveMessageFromSqs(catalogItemsQueueUrl, productMsgAttribute)
        then:
        actualReceiveMessageResponse != null
        def actualMessages = actualReceiveMessageResponse.messages()
        with(actualMessages) {
            !it.isEmpty()
            it.size() == 1
        }
        then:
        def actualMessage = actualMessages.get(0)
        with(actualMessage) {
            it.messageAttributes() == productMessageAttributes
            with(it.body()) {
                !it.empty
                def actualMap = new JsonSlurper().parseText(it) as Map
                !actualMap.isEmpty()
                actualMap.id == productId
                actualMap.title == "Product Title"
                actualMap.description == "Short Product Description"
                actualMap.price == 10
                actualMap.count == 2
            }
        }

        when: '3. delete a message from SQS'
        sqsClient.deleteMessage({
            b -> b.queueUrl(catalogItemsQueueUrl).receiptHandle(actualMessage.receiptHandle())
        })
        then:
        noExceptionThrown()

        and: '4. Check the moved file'
        def parsedKey = "$parsedFolder/$fileName"
        when:
        def objectContent = s3Client.getObject({
            b -> b.bucket(bucketName).key(parsedKey)
        })
        def reader = new BufferedReader(new InputStreamReader(objectContent))
        def result = reader.readLine()
        then:
        result != null
        result == csvContent

        cleanup:
        reader.close()
        objectContent.close()
        s3Client.deleteObject({
            b -> b.bucket(bucketName).key(parsedKey)
        })
        s3Client.deleteBucket({
            b -> b.bucket(bucketName)
        })
    }

    def 'should return FAILED if event is empty'() {
        given:
        def handler = new LambdaHandler(s3Client, sqsClient, uploadFolder, parsedFolder, "catalogItemsQueueUrl")
        when:
        def actual = handler.handleRequest(new S3Event(), new TestContext())
        then:
        actual == "FAILED"
    }

    def toAwsAccount() {
        stsClient.getCallerIdentity().account()
    }

    def createQueue(String queueName) {
        sqsClient.createQueue({
            b -> b.queueName(queueName)
        })
        "${localstack.getEndpoint().toString()}/${toAwsAccount()}/$queueName" as String
    }

    def receiveMessageFromSqs(String catalogItemsQueueUrl, String productMsgAttribute) {

        println catalogItemsQueueUrl
        sqsClient.receiveMessage({
            b ->
                b.queueUrl(catalogItemsQueueUrl)
                        .messageAttributeNames(productMsgAttribute)
                        .waitTimeSeconds(10)
                        .maxNumberOfMessages(5)
        })
    }

}
