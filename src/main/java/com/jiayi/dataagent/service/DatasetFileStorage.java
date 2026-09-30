package com.jiayi.dataagent.service;

import com.jiayi.dataagent.exception.DatasetException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile("mysql")
public class DatasetFileStorage {

    private final Path storageRoot;

    public DatasetFileStorage(@Value("${dataagent.storage.root}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) {
        String storedFilename = UUID.randomUUID() + ".csv";
        Path destination = storageRoot.resolve(storedFilename).normalize();
        try {
            Files.createDirectories(storageRoot);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(storedFilename, destination.toString());
        } catch (IOException exception) {
            throw new DatasetException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store dataset file", exception);
        }
    }

    public InputStream open(String filePath) {
        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(storageRoot)) {
            throw new DatasetException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid dataset file path");
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException exception) {
            throw new DatasetException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Stored dataset file is unavailable", exception);
        }
    }

    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException ignored) {
            // The database error remains primary; stale files can be cleaned up separately.
        }
    }

    public void deleteRequired(String filePath) {
        Path path = resolveStoredPath(filePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new DatasetException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete dataset file", exception);
        }
    }

    private Path resolveStoredPath(String filePath) {
        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(storageRoot)) {
            throw new DatasetException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid dataset file path");
        }
        return path;
    }

    public record StoredFile(String storedFilename, String filePath) {
    }
}
