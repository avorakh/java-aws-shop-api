package dev.avorakh.shop.function

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import dev.avorakh.shop.function.test.TestContext
import dev.avorakh.shop.function.test.TestUtils
import dev.avorakh.shop.localstack.LocalstackAwsSdkV2TestUtil
import dev.avorakh.shop.test.spock.AbstractLocalstackSpecification
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import spock.lang.Shared

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3

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

    LambdaHandler handler

    @Override
    LocalStackContainer.Service[] getLocalstackServices() {
        return new LocalStackContainer.Service[]{
                S3
        }
    }

    def setup() {
        objectMapper = new ObjectMapper()
        objectMapper.registerModule(new JodaModule())

        s3Client = S3Client
                .builder()
                .endpointOverride(LocalstackAwsSdkV2TestUtil.toEndpointURI(this))
                .credentialsProvider(LocalstackAwsSdkV2TestUtil.toCredentialsProvider(this))
                .region(LocalstackAwsSdkV2TestUtil.toRegion(this))
                .build()

        handler = new LambdaHandler(s3Client, uploadFolder, parsedFolder)
    }

    def cleanup() {
        s3Client.close()
    }

    def 'should successfully handle S3 event'() {
        given:
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

        and:
        def csvContent = TestUtils.readFile(classLoader, fileName)
        s3Client.createBucket({
            b -> b.bucket(bucketName)
        })
        s3Client.putObject({
            b -> b.bucket(bucketName).key("$uploadFolder/$fileName")
        }, RequestBody.fromBytes(csvContent.getBytes()))

        when:
        def actual = handler.handleRequest(new S3Event([record]), new TestContext())

        then:
        actual == "OK"

        and:
        def parsedKey = "$parsedFolder/$fileName"
        when:
        def objectContent = s3Client.getObject({
            b -> b.bucket(bucketName).key(parsedKey)
        })
        def reader = new BufferedReader(new InputStreamReader(objectContent))
        def result = reader.readLine()
        then:
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
        when:
        def actual = handler.handleRequest(new S3Event(), new TestContext())
        then:
        actual == "FAILED"
    }
}
