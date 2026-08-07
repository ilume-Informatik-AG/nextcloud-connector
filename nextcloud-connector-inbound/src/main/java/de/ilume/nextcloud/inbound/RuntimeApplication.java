package de.ilume.nextcloud.inbound;


import io.camunda.connector.api.secret.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ServiceLoader;

@SpringBootApplication
public class RuntimeApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RuntimeApplication.class, args);

        LOGGER.info("Nextcloud webhook connector started!");
        ServiceLoader<SecretProvider> loader =
                ServiceLoader.load(io.camunda.connector.api.secret.SecretProvider.class,
                        Thread.currentThread().getContextClassLoader());
        loader.forEach(provider -> LOGGER.info("Found Secret Provider: {}", provider.getClass().getName()));
    }
}
