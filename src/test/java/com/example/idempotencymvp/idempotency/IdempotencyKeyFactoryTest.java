package com.example.idempotencymvp.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class IdempotencyKeyFactoryTest {

    private final IdempotencyKeyFactory factory = new IdempotencyKeyFactory();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void fingerprintIsStableAcrossNestedObjectFieldOrder() {
        Map<String, Object> firstNested = new LinkedHashMap<>();
        firstNested.put("z", 2);
        firstNested.put("a", 1);
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("nested", firstNested);
        first.put("a", 1);

        Map<String, Object> secondNested = new LinkedHashMap<>();
        secondNested.put("a", 1);
        secondNested.put("z", 2);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("a", 1);
        second.put("nested", secondNested);

        String expected = "1275ca4277a203fbaa6b905ebccb650d28bbd24c52f947c412eab6272af54532";
        assertThat(factory.fingerprint(first, mapper)).isEqualTo(expected);
        assertThat(factory.fingerprint(second, mapper)).isEqualTo(expected);
    }

    @Test
    void fingerprintPreservesArrayOrder() {
        assertThat(factory.fingerprint(Arrays.asList("first", "second"), mapper))
                .isNotEqualTo(factory.fingerprint(Arrays.asList("second", "first"), mapper));
    }
}
