package com.tenantcomplaint.exception;

/**
 * Thrown when an invalid status transition is attempted.
 */
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
