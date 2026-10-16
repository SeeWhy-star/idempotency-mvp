package com.example.idempotencymvp.idempotency;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class IdempotencyKeyFactory {

    public static final String PREFIX = "idempotency:mvp:";

    public String create(String accountId, String method, String uri, String idempotencyKey) {
        String scope = accountId + "\n" + method.toUpperCase() + "\n"
                + normalizeUri(uri) + "\n" + idempotencyKey;
        return PREFIX + "request:" + sha256(scope);
    }

    public String fingerprint(Object requestBody, ObjectMapper mapper) {
        try {
            JsonNode canonicalBody = canonicalize(mapper.valueToTree(requestBody), mapper);
            return sha256(mapper.writeValueAsString(canonicalBody));
        } catch (JacksonException failure) {
            throw new IllegalArgumentException("Unable to fingerprint request", failure);
        }
    }

    private JsonNode canonicalize(JsonNode node, ObjectMapper mapper) {
        if (node.isObject()) {
            ObjectNode source = (ObjectNode) node;
            ObjectNode canonical = mapper.createObjectNode();
            List<String> names = new ArrayList<>(source.propertyNames());
            Collections.sort(names);
            for (String name : names) {
                canonical.set(name, canonicalize(source.get(name), mapper));
            }
            return canonical;
        }
        if (node.isArray()) {
            ArrayNode canonical = mapper.createArrayNode();
            for (JsonNode element : node) {
                canonical.add(canonicalize(element, mapper));
            }
            return canonical;
        }
        return node;
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
