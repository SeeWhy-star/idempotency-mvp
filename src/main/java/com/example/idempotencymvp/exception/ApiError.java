package com.example.idempotencymvp.exception;

public class ApiError {

    private final String type;
    private final String code;
    private final String message;

    public ApiError(String type, String code, String message) {
        this.type = type;
        this.code = code;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
