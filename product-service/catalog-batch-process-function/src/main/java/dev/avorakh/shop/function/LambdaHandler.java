package dev.avorakh.shop.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.avorakh.shop.dao.DynamoDbTransactionalProductDao;
import dev.avorakh.shop.dao.TransactionalProductDao;
import dev.avorakh.shop.function.model.CommonUtils;
import dev.avorakh.shop.function.model.ProductInputResource;
import dev.avorakh.shop.function.model.ProductOutputResource;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LambdaHandler implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    TransactionalProductDao productDao;
    SnsClient snsClient;
    String snsTopicArn;

    public LambdaHandler() {
        var dynamoDbClient = DynamoDbClient.builder().build();
        var productTableName = System.getenv("PRODUCT_TABLE_NAME");
        var stockTableName = System.getenv("STOCK_TABLE_NAME");
        this.productDao = new DynamoDbTransactionalProductDao(dynamoDbClient, productTableName, stockTableName);
        this.snsClient = SnsClient.builder().build();
        this.snsTopicArn = System.getenv("SNS_TOPIC_ARN");
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {

        var logger = context.getLogger();

        try {
            logger.log("SQS Event - [%s]".formatted(sqsEvent.toString()), LogLevel.INFO);
            for (SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
                processMessage(message, logger);
            }

        } catch (Exception e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
        }
        return null;
    }

    void processMessage(SQSEvent.SQSMessage message, LambdaLogger logger) {

        logger.log("Id - [%s]".formatted(message.getMessageId()), LogLevel.INFO);
        logger.log("Attributes - [%s]".formatted(message.getAttributes()), LogLevel.INFO);
        logger.log("Message Attributes - [%s]".formatted(message.getMessageAttributes()), LogLevel.INFO);
        logger.log("Message - [%s]".formatted(message.getBody()), LogLevel.INFO);

        Optional.ofNullable(message)
                .map(SQSEvent.SQSMessage::getBody)
                .filter(StringUtils::isNotBlank)
                .flatMap(body -> toProductOutputResource(body, logger))
                .filter(this::isValid)
                .ifPresentOrElse(
                        product -> {
                            var isSaved = save(product, logger);
                            if (isSaved) {
                                publishToSns(message, product, logger);
                            }
                        },
                        () -> logger.log("Unable to save product - [%s]".formatted(message.getBody()), LogLevel.INFO));
    }

    Optional<ProductOutputResource> toProductOutputResource(String body, LambdaLogger logger) {
        try {
            return Optional.of(objectMapper.readValue(body, ProductOutputResource.class));
        } catch (JsonProcessingException e) {
            logger.log(
                    "Unable to unmarshal body - [%s] due to error - [%s]".formatted(body, e.getMessage()),
                    LogLevel.ERROR);
            return Optional.empty();
        }
    }

    boolean isValid(ProductOutputResource product) {
        return CommonUtils.validate(product).isEmpty();
    }

    boolean save(ProductOutputResource product, LambdaLogger logger) {
        try {
            productDao.create(product.getId(), toInputResource(product));
            logger.log("Product with id - [%s] was saved.".formatted(product.getId()), LogLevel.INFO);
            return true;
        } catch (Exception e) {
            logger.log(
                    "Unable to save product - [%s] due to error - [%s]".formatted(product, e.getMessage()),
                    LogLevel.ERROR);
            return false;
        }
    }

    void publishToSns(SQSEvent.SQSMessage message, ProductOutputResource product, LambdaLogger logger) {
        try {

            var publishResponse = snsClient.publish(builder -> builder.topicArn(snsTopicArn)
                    .subject("New product")
                    .messageAttributes(toMessageAttributes(product))
                    .message(message.getBody()));
            logger.log("SNS message published with id - [%s]".formatted(publishResponse.messageId()), LogLevel.INFO);
        } catch (Exception e) {
            logger.log(
                    "Unable to publish SNS message for product - [%s] due to error - [%s]"
                            .formatted(message.getBody(), e.getMessage()),
                    LogLevel.ERROR);
        }
    }

    ProductInputResource toInputResource(ProductOutputResource product) {
        return ProductInputResource.builder()
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .count(product.getCount())
                .build();
    }

    Map<String, MessageAttributeValue> toMessageAttributes(ProductOutputResource product) {

        return Map.of(
                "id", toMessageAttributeValue("String", product.getId()),
                "title", toMessageAttributeValue("String", product.getTitle()),
                "description", toMessageAttributeValue("String", product.getDescription()),
                "price", toMessageAttributeValue("Number", product.getPrice().toString()),
                "count", toMessageAttributeValue("Number", product.getCount().toString()));
    }

    MessageAttributeValue toMessageAttributeValue(String type, String value) {
        return MessageAttributeValue.builder().stringValue(value).dataType(type).build();
    }
}
