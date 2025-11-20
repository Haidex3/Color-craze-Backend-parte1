package com.colorcraze.auth.exceptions;

/**
 * Exception thrown when a Firebase login operation fails.
 * Wraps the original cause to provide more context for authentication errors.
 */
public class FirebaseLoginException extends RuntimeException {

    /**
     * Creates a new FirebaseLoginException with a message and underlying cause.
     *
     * @param message the error description
     * @param cause the root exception that triggered the failure
     */
    public FirebaseLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}