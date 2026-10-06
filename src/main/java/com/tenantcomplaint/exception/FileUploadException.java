package com.tenantcomplaint.exception;

/**
 * Thrown for invalid file uploads.
 */
public class FileUploadException extends RuntimeException {
    public FileUploadException(String message) {
        super(message);
    }
}
