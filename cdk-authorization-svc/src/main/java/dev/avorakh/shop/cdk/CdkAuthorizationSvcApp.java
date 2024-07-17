package dev.avorakh.shop.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class CdkAuthorizationSvcApp {
    public static void main(final String[] args) {
        var app = new App();

        var pr = StackProps.builder().build();

        new CdkAuthorizationSvcStack(app, "cdk-authorization-svc-stack", pr);

        app.synth();
    }
}

