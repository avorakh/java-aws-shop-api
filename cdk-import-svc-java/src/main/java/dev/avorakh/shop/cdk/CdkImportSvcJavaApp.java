package dev.avorakh.shop.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class CdkImportSvcJavaApp {

    public static final String MY_SHOP_IMPORT_SVC_CDK_JAVA_APP = "my-shop-import-svc-cdk-java-app";

    public static void main(final String[] args) {
        var app = new App();

        var pr = StackProps.builder().build();

        new MyShopImportServiceBackendJavaStack(app, MY_SHOP_IMPORT_SVC_CDK_JAVA_APP, pr);

        app.synth();
    }
}

