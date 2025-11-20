package com.colorcraze.auth.exceptions;

public class FirebaseLoginException extends RuntimeException {
    public FirebaseLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
