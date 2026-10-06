package com.tenantcomplaint.exception;

/**
 * Thrown when a complaint is not found.
 */
public class ComplaintNotFoundException extends RuntimeException {
    public ComplaintNotFoundException(String message) {
        super(message);
    }
}
