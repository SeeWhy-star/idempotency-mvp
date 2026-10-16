package com.example.idempotencymvp.exception;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleIdempotencyConflict(
            IdempotencyConflictException exception) {
        HttpStatus status = "invalid_idempotency_request".equals(exception.getCode())
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new ApiError(
                "idempotency_error", exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(CallbackBusyException.class)
    public ResponseEntity<ApiError> handleCallbackBusy(CallbackBusyException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                "callback_error", "callback_processing", exception.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "request_error", "not_found", exception.getMessage()));
    }

    @ExceptionHandler(IdempotencyInfrastructureException.class)
    public ResponseEntity<ApiError> handleInfrastructure(
            IdempotencyInfrastructureException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(
                "idempotency_error", "idempotency_unavailable", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ApiError(
                "request_error", "invalid_request", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "request_error", "invalid_request", exception.getMessage()));
    }
}
