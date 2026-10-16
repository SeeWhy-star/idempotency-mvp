package com.example.idempotencymvp.idempotency;

import com.example.idempotencymvp.exception.IdempotencyConflictException;
import com.example.idempotencymvp.exception.IdempotencyInfrastructureException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyStore store;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<?> execute(
            IdempotencyContext context, IdempotentOperation operation) throws Throwable {
        AcquireResult acquired;
        try {
            acquired = store.acquire(
                    context.getRedisKey(), context.getFingerprint(), context.getOwnerToken());
        } catch (RuntimeException storeFailure) {
            throw new IdempotencyInfrastructureException(
                    "Idempotency store is unavailable", storeFailure);
        }

        switch (acquired.getDecision()) {
            case PARAMETER_MISMATCH:
                throw new IdempotencyConflictException(
                        "parameter_mismatch",
                        "The idempotency key has already been used with different parameters.");
            case PROCESSING:
                throw new IdempotencyConflictException(
                        "request_processing",
                        "A request with the same idempotency key is processing.");
            case COMPLETED:
                return replay(acquired.getRecord());
            case ACQUIRED:
                return executeAsOwner(context, operation);
            default:
                throw new IllegalStateException(
                        "Unknown acquire decision: " + acquired.getDecision());
        }
    }

    private ResponseEntity<?> executeAsOwner(
            IdempotencyContext context, IdempotentOperation operation) throws Throwable {
        ResponseEntity<?> response;
        try {
            response = operation.execute();
        } catch (Throwable businessFailure) {
            safeAbort(context);
            throw businessFailure;
        }

        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(response.getBody());
        } catch (JsonProcessingException serializationFailure) {
            safeAbort(context);
            throw new IdempotencyInfrastructureException(
                    "Unable to serialize idempotent response", serializationFailure);
        }

        boolean completed;
        try {
            completed = store.complete(
                    context.getRedisKey(),
                    context.getFingerprint(),
                    context.getOwnerToken(),
                    response.getStatusCodeValue(),
                    responseJson);
        } catch (RuntimeException storeFailure) {
            throw new IdempotencyInfrastructureException(
                    "Unable to persist the completed idempotency response", storeFailure);
        }
        if (!completed) {
            throw new IdempotencyInfrastructureException(
                    "Idempotency ownership expired before completion");
        }
        return response;
    }

    private ResponseEntity<?> replay(IdempotencyRecord record) {
        try {
            objectMapper.readTree(record.getResponseBody());
            return ResponseEntity.status(record.getHttpStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(record.getResponseBody());
        } catch (JsonProcessingException invalidCache) {
            throw new IdempotencyInfrastructureException(
                    "Cached idempotency response is invalid", invalidCache);
        }
    }

    private void safeAbort(IdempotencyContext context) {
        try {
            store.abort(context.getRedisKey(), context.getOwnerToken());
        } catch (RuntimeException cleanupFailure) {
            LOGGER.error("Unable to clean the owned PROCESSING idempotency record", cleanupFailure);
        }
    }
}
