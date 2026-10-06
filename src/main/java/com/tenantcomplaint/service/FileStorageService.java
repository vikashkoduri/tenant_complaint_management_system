package com.tenantcomplaint.service;

import com.tenantcomplaint.exception.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Handles file upload/download operations with validation.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadPath;
    private final List<String> allowedTypes;
    private final long maxSizeMB;

    public FileStorageService(
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${app.upload.allowed-types}") List<String> allowedTypes,
            @Value("${app.upload.max-size-mb}") long maxSizeMB) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.allowedTypes = allowedTypes;
        this.maxSizeMB = maxSizeMB;

        try {
            Files.createDirectories(this.uploadPath);
            log.info("Upload directory initialized: {}", this.uploadPath);
        } catch (IOException e) {
            throw new FileUploadException("Could not create upload directory: " + e.getMessage());
        }
    }

    /**
     * Stores a file and returns the generated filename.
     */
    public String storeFile(MultipartFile file) {
        validateFile(file);

        // Generate safe unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = this.uploadPath.resolve(storedFilename).normalize();

            // Prevent path traversal
            if (!targetLocation.startsWith(this.uploadPath)) {
                throw new FileUploadException("Invalid file path detected");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored successfully: {}", storedFilename);
            return storedFilename;
        } catch (IOException e) {
            throw new FileUploadException("Failed to store file: " + e.getMessage());
        }
    }

    /**
     * Validates file type, size, and name.
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new FileUploadException(
                    "Invalid file type. Allowed types: PDF, JPEG, PNG, GIF, DOC, DOCX");
        }

        // Validate file size
        long maxBytes = maxSizeMB * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new FileUploadException("File size exceeds maximum limit of " + maxSizeMB + "MB");
        }

        // Validate filename safety
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.contains("..") || filename.contains("/") || filename.contains("\\"))) {
            throw new FileUploadException("Invalid filename");
        }
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    public Path getUploadPath() {
        return uploadPath;
    }
}
