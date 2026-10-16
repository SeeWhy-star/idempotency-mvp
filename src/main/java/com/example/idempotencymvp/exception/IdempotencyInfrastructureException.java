package com.example.idempotencymvp.exception;

public class IdempotencyInfrastructureException extends RuntimeException {

    public IdempotencyInfrastructureException(String message) {
        super(message);
    }

    public IdempotencyInfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
