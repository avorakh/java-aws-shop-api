# Automated Deployment For Authorization Service

This is a project for CDK development with Java.
It is a [Maven](https://maven.apache.org/) based project.
## CDK Toolkit

The [`cdk.json`](./cdk.json) file includes instructions for the CDK toolkit on how to execute this project.

## Useful commands

* `mvn package`     compile and run tests
* `cdk ls`          list all stacks in the app
* `cdk synth`       emits the synthesized CloudFormation template
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk docs`        open CDK documentation

## .env file 

The '.env' file should be located in the 'asset' folder.
```
.
├── asset
│   ├── .env
├── cdk.json
├── pom.xml
├── README.md
└── src
    └── main
        └── java
            └── dev
                └── avorakh
                    └── shop
                        └── cdk
                            ├── CdkAuthorizationSvcApp.java
                            └── CdkAuthorizationSvcStack.java
```

Please see the '.env' file example:
```.env
johndoe=TEST_PASSWORD
```

