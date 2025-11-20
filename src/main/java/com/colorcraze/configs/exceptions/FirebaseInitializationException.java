package com.colorcraze.configs.exceptions;

/**
 * Exception thrown when there is a failure during Firebase initialization.
 */
public class FirebaseInitializationException extends RuntimeException {

    /**
     * Constructs a new FirebaseInitializationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause of the exception
     */
    public FirebaseInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
