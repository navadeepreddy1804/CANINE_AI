package com.canineai.backend.storage;

import com.canineai.backend.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class LocalStorageProvider implements StorageProvider {

    private final Path rootPath;

    public LocalStorageProvider(@Value("${canineai.storage.local-dir:uploads}") String uploadDir) {
        this.rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
            log.info("Initialized local upload storage directory path: {}", this.rootPath);
        } catch (IOException e) {
            log.error("Failed to initialize root storage directories: {}", uploadDir, e);
            throw new RuntimeException("Could not initialize local storage paths: " + uploadDir);
        }
    }

    @Override
    public void saveStream(String relativePath, InputStream stream) {
        Path targetPath = resolvePath(relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetPath.toFile()))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = stream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }
            log.debug("Successfully saved file stream: {}", targetPath);
        } catch (IOException e) {
            log.error("Failed to save streamed file to path: {}", relativePath, e);
            throw new RuntimeException("Storage failure writing target file stream: " + relativePath, e);
        }
    }

    @Override
    public void saveFile(String relativePath, byte[] data) {
        Path targetPath = resolvePath(relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, data);
            log.debug("Successfully saved file data: {}", targetPath);
        } catch (IOException e) {
            log.error("Failed to save file binary to path: {}", relativePath, e);
            throw new RuntimeException("Storage failure writing target file payload: " + relativePath, e);
        }
    }

    @Override
    public InputStream getFileStream(String relativePath) {
        Path targetPath = resolvePath(relativePath);
        try {
            return new BufferedInputStream(new FileInputStream(targetPath.toFile()));
        } catch (FileNotFoundException e) {
            log.error("Target storage file not found at: {}", relativePath, e);
            throw new BusinessException.ResourceNotFoundException("File not found: " + relativePath);
        }
    }

    @Override
    public void deleteFile(String relativePath) {
        Path targetPath = resolvePath(relativePath);
        try {
            Files.deleteIfExists(targetPath);
            log.info("Deleted file from storage: {}", targetPath);
        } catch (IOException e) {
            log.error("Failed to delete storage file at: {}", relativePath, e);
        }
    }

    /**
     * Resolves and validates target paths, blocking directory traversal.
     */
    private Path resolvePath(String relativePath) {
        Path resolved = this.rootPath.resolve(relativePath).normalize();
        if (!resolved.startsWith(this.rootPath)) {
            log.warn("Directory traversal attempt blocked! Relative path: {}", relativePath);
            throw new BusinessException.ForbiddenException("Access denied: invalid file path requested.");
        }
        return resolved;
    }

    @Override
    public void moveDirectory(String sourcePath, String targetPath) {
        Path source = resolvePath(sourcePath);
        Path target = resolvePath(targetPath);
        try {
            if (Files.exists(source)) {
                Files.createDirectories(target.getParent());
                Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                log.info("Moved directory from {} to {}", source, target);
            } else {
                log.warn("Source directory does not exist for move: {}", source);
            }
        } catch (IOException e) {
            log.error("Failed to move directory from {} to {}", sourcePath, targetPath, e);
            throw new RuntimeException("Storage failure moving directory", e);
        }
    }
}
