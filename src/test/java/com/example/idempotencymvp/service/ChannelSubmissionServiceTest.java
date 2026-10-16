package com.example.idempotencymvp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.idempotencymvp.channel.FakeChannelClient;
import com.example.idempotencymvp.domain.PaymentIntent;
import com.example.idempotencymvp.domain.PaymentIntentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChannelSubmissionServiceTest {

    @Test
    void retryingSamePaymentUsesSameStableChannelIdempotencyKey() {
        FakeChannelClient client = new FakeChannelClient();
        ChannelSubmissionService service = new ChannelSubmissionService(client);
        PaymentIntent paymentIntent = new PaymentIntent(
                "pi-stable-1",
                "acct-1",
                "order-1",
                new BigDecimal("10.00"),
                "USD",
                PaymentIntentStatus.PROCESSING,
                Instant.parse("2026-01-01T00:00:00Z"));

        service.submit(paymentIntent);
        service.submit(paymentIntent);

        assertThat(client.getReceivedIdempotencyKeys())
                .containsExactly("pi-stable-1", "pi-stable-1");
    }
}
