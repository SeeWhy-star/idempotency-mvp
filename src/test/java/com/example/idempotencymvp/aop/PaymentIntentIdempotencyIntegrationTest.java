package com.example.idempotencymvp.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.idempotencymvp.channel.FakeChannelClient;
import com.example.idempotencymvp.repository.PaymentIntentRepository;
import com.example.idempotencymvp.service.PaymentCallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIntentIdempotencyIntegrationTest {

    private static final String REQUEST = "{"
            + "\"merchantOrderNo\":\"order-web-1\","
            + "\"amount\":12.30,"
            + "\"currency\":\"USD\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentIntentRepository repository;

    @Autowired
    private FakeChannelClient channelClient;

    @MockBean
    private PaymentCallbackService paymentCallbackService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        channelClient.reset();
    }

    @Test
    void firstRequestSucceedsAndExactReplayDoesNotCallBusinessOrChannelAgain()
            throws Exception {
        MvcResult first = perform("web-key-1", REQUEST)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantOrderNo").value("order-web-1"))
                .andReturn();

        MvcResult replay = perform("web-key-1", REQUEST)
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(replay.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());
        assertThat(repository.count()).isEqualTo(1);
        assertThat(channelClient.getInvocationCount()).isEqualTo(1);
    }

    @Test
    void sameKeyWithDifferentBodyReturnsParameterMismatch() throws Exception {
        perform("web-key-2", REQUEST).andExpect(status().isCreated());

        perform(
                "web-key-2",
                "{\"merchantOrderNo\":\"order-web-1\","
                        + "\"amount\":99.99,\"currency\":\"USD\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("idempotency_error"))
                .andExpect(jsonPath("$.code").value("parameter_mismatch"));

        assertThat(channelClient.getInvocationCount()).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions perform(
            String idempotencyKey, String body) throws Exception {
        return mockMvc.perform(post("/v1/payment_intents")
                .header("X-Account-Id", "acct-web-1")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
