package dev.avorakh.shop.dao;

import java.net.URI;
import lombok.experimental.UtilityClass;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

@UtilityClass
public class LocalstackAwsSdkV2TestUtil {

    public URI toEndpointURI(LocalstackConfigurable localstack) {
        return localstack.getLocalstackEndpoint();
    }

    public AwsCredentialsProvider toCredentialsProvider(LocalstackConfigurable localstack) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getLocalstackAccessKey(), localstack.getLocalstackSecretKey()));
    }

    public Region toRegion(LocalstackConfigurable localstack) {
        return Region.of(localstack.getLocalstackRegion());
    }
}
