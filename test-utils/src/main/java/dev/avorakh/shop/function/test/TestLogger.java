package dev.avorakh.shop.function.test;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogger implements LambdaLogger {
    private static final Logger logger = LoggerFactory.getLogger(TestLogger.class);

    public void log(String message) {
        System.out.println(message);
        logger.info(message);
    }

    public void log(byte[] message) {
        System.out.println(new String(message));
        logger.info(new String(message));
    }
}
