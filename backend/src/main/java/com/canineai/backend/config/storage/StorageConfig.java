package com.canineai.backend.config.storage;

import com.canineai.backend.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class StorageConfig {

    @Value("${canineai.storage.active-provider:local}")
    private String activeProvider;

    @Value("${canineai.storage.local-dir:uploads}")
    private String localUploadDir;

    @Bean
    public StorageProvider storageProvider() {
        log.info("Resolving active CanineAI storage provider: {}", activeProvider);
        switch (activeProvider.toLowerCase()) {
            case "s3":
                return new S3StorageProvider();
            case "azure":
                return new AzureBlobStorageProvider();
            case "gcs":
                return new GcsStorageProvider();
            case "local":
            default:
                return new LocalStorageProvider(localUploadDir);
        }
    }
}
