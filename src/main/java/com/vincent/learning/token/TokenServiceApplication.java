package com.vincent.learning.token;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TokenServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TokenServiceApplication.class, args);
        startLog();
    }

    private static void startLog() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.start();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
