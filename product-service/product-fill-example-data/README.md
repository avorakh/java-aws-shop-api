# product-fill-example-data
This application is responsible for to fill tables with test examples. 
Execute it for your DB to fill data.

## Prerequisites
 - Java 17
 - AWS CLI V2
 - The local environment should use AWS SSO token provider configuration.
[Link](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html#getting_started_auth)

## Commands
Build the application
```bash
# Launch in the root directory of the multi-module project
./gradlew clean
./gradlew :product-fill-example-data:jar
```
Launch the application
```bash
java -jar ./product-fill-example-data/build/libs/product-fill-example-data.jar 
```