package com.example.idempotencymvp.exception;

public record ApiError(String type, String code, String message) {
}
