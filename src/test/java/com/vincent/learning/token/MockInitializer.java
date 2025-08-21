package com.vincent.learning.token;

import java.io.File;
import java.io.IOException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.FileCopyUtils;

public class MockInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            FileCopyUtils.copy(
                    new File("src/test/resources/client-public.crt"),
                    new File("src/test/resources/client-public-test.crt"));
            FileCopyUtils.copy(
                    new File("src/test/resources/client-private.key"),
                    new File("src/test/resources/client-private-test.key"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
