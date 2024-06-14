package dev.avorakh.shop.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class MyShopBackendCdkJavaApp {

    public static final String MY_SHOP_BACKEND_CDK_JAVA_APP = "my-shop-backend-cdk-java-app";

    public static void main(final String[] args) {
        var app = new App();

        var pr = StackProps.builder().build();

        new MyShopBackendJavaStack(app, MY_SHOP_BACKEND_CDK_JAVA_APP, pr);

        app.synth();
    }
}