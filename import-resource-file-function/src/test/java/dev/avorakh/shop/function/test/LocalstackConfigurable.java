package dev.avorakh.shop.function.test;

import java.net.URI;
import org.testcontainers.containers.localstack.LocalStackContainer;

public interface LocalstackConfigurable {
    LocalStackContainer.Service[] getLocalstackServices();

    String getLocalstackAccessKey();

    String getLocalstackSecretKey();

    URI getLocalstackEndpoint();

    String getLocalstackRegion();
}
