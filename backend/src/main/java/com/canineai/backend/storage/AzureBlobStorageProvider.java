package com.canineai.backend.storage;

import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
public class AzureBlobStorageProvider implements StorageProvider {

    @Override
    public void saveStream(String path, InputStream stream) {
        log.info("[Azure Blob Storage] Uploading incoming stream to container path: {}", path);
        // In the future, this will use blobClient.upload(...)
    }

    @Override
    public void saveFile(String path, byte[] data) {
        log.info("[Azure Blob Storage] Saving binary payload to path: {}", path);
    }

    @Override
    public InputStream getFileStream(String path) {
        log.info("[Azure Blob Storage] Downloading file stream from path: {}", path);
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public void deleteFile(String path) {
        log.info("[Azure Blob Storage] Deleting blob at path: {}", path);
    }

    @Override
    public void moveDirectory(String sourcePath, String targetPath) {
        log.info("[Azure Blob Storage] Moving directory from {} to {}", sourcePath, targetPath);
    }
}
