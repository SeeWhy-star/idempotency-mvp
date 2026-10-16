package com.example.idempotencymvp.idempotency;

import org.springframework.http.ResponseEntity;

@FunctionalInterface
public interface IdempotentOperation {

    ResponseEntity<?> execute() throws Throwable;
}
