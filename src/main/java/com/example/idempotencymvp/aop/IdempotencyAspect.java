package com.example.idempotencymvp.aop;

import com.example.idempotencymvp.annotation.Idempotent;
import com.example.idempotencymvp.exception.IdempotencyConflictException;
import com.example.idempotencymvp.idempotency.IdempotencyContext;
import com.example.idempotencymvp.idempotency.IdempotencyKeyFactory;
import com.example.idempotencymvp.idempotency.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Aspect
@Component
public class IdempotencyAspect {

    private final HttpServletRequest request;
    private final IdempotencyKeyFactory keyFactory;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(
            HttpServletRequest request,
            IdempotencyKeyFactory keyFactory,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.request = request;
        this.keyFactory = keyFactory;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(idempotent)")
    public Object intercept(ProceedingJoinPoint joinPoint, Idempotent idempotent)
            throws Throwable {
        String accountId = requiredHeader("X-Account-Id", 100);
        String idempotencyKey = requiredHeader("Idempotency-Key", 255);
        Object body = findRequestBody(joinPoint);
        String redisKey = keyFactory.create(
                accountId, request.getMethod(), request.getRequestURI(), idempotencyKey);
        String fingerprint = keyFactory.fingerprint(body, objectMapper);
        IdempotencyContext context =
                new IdempotencyContext(redisKey, fingerprint, UUID.randomUUID().toString());

        return idempotencyService.execute(context, () -> {
            Object result = joinPoint.proceed();
            if (!(result instanceof ResponseEntity)) {
                throw new IllegalStateException("@Idempotent methods must return ResponseEntity");
            }
            return (ResponseEntity<?>) result;
        });
    }

    private String requiredHeader(String name, int maxLength) {
        String value = request.getHeader(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IdempotencyConflictException(
                    "invalid_idempotency_request", name + " header is required.");
        }
        if (value.length() > maxLength) {
            throw new IdempotencyConflictException(
                    "invalid_idempotency_request",
                    name + " header must not exceed " + maxLength + " characters.");
        }
        return value;
    }

    private Object findRequestBody(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Annotation[][] annotations = method.getParameterAnnotations();
        Object[] arguments = joinPoint.getArgs();
        for (int index = 0; index < annotations.length; index++) {
            for (Annotation annotation : annotations[index]) {
                if (annotation.annotationType() == RequestBody.class) {
                    return arguments[index];
                }
            }
        }
        return arguments;
    }
}
