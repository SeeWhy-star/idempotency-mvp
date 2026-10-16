package com.example.idempotencymvp.service;

import com.example.idempotencymvp.channel.ChannelPaymentResult;
import com.example.idempotencymvp.channel.FakeChannelClient;
import com.example.idempotencymvp.domain.PaymentIntent;
import org.springframework.stereotype.Service;

@Service
public class ChannelSubmissionService {

    private final FakeChannelClient channelClient;

    public ChannelSubmissionService(FakeChannelClient channelClient) {
        this.channelClient = channelClient;
    }

    public ChannelPaymentResult submit(PaymentIntent paymentIntent) {
        // 渠道幂等键来自已落库的支付意图 ID；重试时绝不重新生成随机键。
        return channelClient.createPayment(
                paymentIntent.getId(), paymentIntent.getAmount(), paymentIntent.getCurrency());
    }
}
