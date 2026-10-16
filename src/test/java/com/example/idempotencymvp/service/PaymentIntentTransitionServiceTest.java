package com.example.idempotencymvp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.idempotencymvp.controller.PaymentCallbackRequest;
import com.example.idempotencymvp.controller.PaymentCallbackResponse;
import com.example.idempotencymvp.domain.PaymentIntent;
import com.example.idempotencymvp.domain.PaymentIntentStatus;
import com.example.idempotencymvp.repository.PaymentIntentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(PaymentIntentTransitionService.class)
class PaymentIntentTransitionServiceTest {

    @Autowired
    private PaymentIntentRepository repository;

    @Autowired
    private PaymentIntentTransitionService service;

    private PaymentCallbackRequest callback;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.saveAndFlush(new PaymentIntent(
                "pi-callback-1",
                "acct-1",
                "order-callback-1",
                new BigDecimal("15.00"),
                "USD",
                PaymentIntentStatus.PROCESSING,
                Instant.parse("2026-01-01T00:00:00Z")));
        callback = new PaymentCallbackRequest();
        callback.setChannelTransactionId("channel-transaction-1");
        callback.setStatus(PaymentIntentStatus.SUCCEEDED);
    }

    @Test
    void duplicateCallbackTransitionsDatabaseOnlyOnce() {
        PaymentCallbackResponse first = service.transition("pi-callback-1", callback);
        PaymentCallbackResponse duplicate = service.transition("pi-callback-1", callback);

        assertThat(first.isAccepted()).isTrue();
        assertThat(first.isTransitioned()).isTrue();
        assertThat(duplicate.isAccepted()).isTrue();
        assertThat(duplicate.isTransitioned()).isFalse();
        assertThat(repository.findById("pi-callback-1").orElseThrow(AssertionError::new)
                .getStatus()).isEqualTo(PaymentIntentStatus.SUCCEEDED);
    }
}
