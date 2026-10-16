package com.example.idempotencymvp.idempotency;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyFactory {

    public static final String PREFIX = "idempotency:mvp:";

    public String create(String accountId, String method, String uri, String idempotencyKey) {
        String scope = accountId + "\n" + method.toUpperCase() + "\n"
                + normalizeUri(uri) + "\n" + idempotencyKey;
        return PREFIX + "request:" + sha256(scope);
    }

    public String fingerprint(Object requestBody, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper canonicalMapper = mapper.copy();
            canonicalMapper.configure(
                    com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY,
                    true);
            canonicalMapper.configure(
                    com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                    true);
            return sha256(canonicalMapper.writeValueAsString(requestBody));
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
            throw new IllegalArgumentException("Unable to fingerprint request", failure);
        }
    }

    private String normalizeUri(String uri) {
        try {
            String normalized = new URI(uri).normalize().getPath();
            if (normalized == null || normalized.isEmpty()) {
                return "/";
            }
            return normalized.length() > 1 && normalized.endsWith("/")
                    ? normalized.substring(0, normalized.length() - 1)
                    : normalized;
        } catch (URISyntaxException invalidUri) {
            throw new IllegalArgumentException("Invalid request URI", invalidUri);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
