package dev.avorakh.shop.dao

import dev.avorakh.shop.localstack.LocalstackAwsSdkV2TestUtil
import dev.avorakh.shop.test.spock.AbstractLocalstackSpecification

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB

import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import spock.lang.Shared

class AwsSdkV2DynamoDbLocalstackSpecification extends AbstractLocalstackSpecification {

    @Shared
    DynamoDbClient dynamoDbClient

    @Override
    LocalStackContainer.Service[] getLocalstackServices() {
        return new LocalStackContainer.Service[]{
                DYNAMODB
        }
    }

    def setup() {
        dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(LocalstackAwsSdkV2TestUtil.toEndpointURI(this))
                .credentialsProvider(LocalstackAwsSdkV2TestUtil.toCredentialsProvider(this))
                .region(LocalstackAwsSdkV2TestUtil.toRegion(this))
                .build()
    }

    def cleanup() {
        dynamoDbClient.close()
    }
}

