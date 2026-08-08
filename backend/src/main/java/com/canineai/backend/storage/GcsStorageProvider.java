package com.canineai.backend.storage;

import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
public class GcsStorageProvider implements StorageProvider {

    @Override
    public void saveStream(String path, InputStream stream) {
        log.info("[Google Cloud Storage] Uploading incoming stream to bucket path: {}", path);
        // In the future, this will use storage.create(...)
    }

    @Override
    public void saveFile(String path, byte[] data) {
        log.info("[Google Cloud Storage] Saving binary payload to bucket path: {}", path);
    }

    @Override
    public InputStream getFileStream(String path) {
        log.info("[Google Cloud Storage] Downloading file stream from path: {}", path);
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public void deleteFile(String path) {
        log.info("[Google Cloud Storage] Deleting target bucket object path: {}", path);
    }

    @Override
    public void moveDirectory(String sourcePath, String targetPath) {
        log.info("[Google Cloud Storage] Moving directory from {} to {}", sourcePath, targetPath);
    }
}
