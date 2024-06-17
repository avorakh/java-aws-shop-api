package dev.avorakh.shop.dao

import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

abstract class AbstractLocalstackSpecification  extends Specification implements LocalstackConfigurable{

    @Shared
    def dockerImageName = "localstack/localstack:3.0.2"
    @Shared
    def localstackImage = DockerImageName.parse(dockerImageName)

    @Shared
    LocalStackContainer localstack

    def setupSpec() {
        localstack = new LocalStackContainer(localstackImage).withServices(getLocalstackServices())
        localstack.start()
    }

    def cleanupSpec(){
        localstack.stop()
    }

    @Override
    String getLocalstackAccessKey() {
        return localstack.getAccessKey()
    }

    @Override
    String getLocalstackSecretKey() {
        return localstack.getSecretKey()
    }

    @Override
    URI getLocalstackEndpoint() {
        localstack.getEndpoint()
    }

    @Override
    String getLocalstackRegion() {
        return localstack.getRegion()
    }
}
