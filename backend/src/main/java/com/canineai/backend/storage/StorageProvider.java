package com.canineai.backend.storage;

import java.io.InputStream;

public interface StorageProvider {

    /**
     * Streams incoming file parts directly to storage without loading fully to RAM.
     */
    void saveStream(String path, InputStream stream);

    /**
     * Saves small binary payloads.
     */
    void saveFile(String path, byte[] data);

    /**
     * Retrieves file from storage provider.
     */
    InputStream getFileStream(String path);

    /**
     * Clears file or folder path.
     */
    void deleteFile(String path);

    /**
     * Moves a directory from one path to another.
     */
    void moveDirectory(String sourcePath, String targetPath);
}
