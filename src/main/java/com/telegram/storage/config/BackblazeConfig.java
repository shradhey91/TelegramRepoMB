package com.telegram.storage.config;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackblazeConfig {

    @Value("${b2.key-id}")
    private String keyId;

    @Value("${b2.application-key}")
    private String applicationKey;

    @Bean
    public B2StorageClient storageClient() {

        return B2StorageClientFactory
                .createDefaultFactory()
                .create(keyId, applicationKey, "telegram");
    }
}
