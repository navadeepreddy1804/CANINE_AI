package com.canineai.backend.storage;

import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
public class S3StorageProvider implements StorageProvider {

    @Override
    public void saveStream(String path, InputStream stream) {
        log.info("[AWS S3 Storage] Uploading incoming stream to S3 path: {}", path);
        // In the future, this will use amazonS3.putObject(...)
    }

    @Override
    public void saveFile(String path, byte[] data) {
        log.info("[AWS S3 Storage] Saving binary payload to S3 path: {}", path);
    }

    @Override
    public InputStream getFileStream(String path) {
        log.info("[AWS S3 Storage] Downloading file stream from S3 path: {}", path);
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public void deleteFile(String path) {
        log.info("[AWS S3 Storage] Deleting target object at S3 path: {}", path);
    }

    @Override
    public void moveDirectory(String sourcePath, String targetPath) {
        log.info("[AWS S3 Storage] Moving directory from {} to {}", sourcePath, targetPath);
    }
}
