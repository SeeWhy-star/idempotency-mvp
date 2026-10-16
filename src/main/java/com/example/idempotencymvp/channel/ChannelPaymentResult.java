package com.example.idempotencymvp.channel;

public class ChannelPaymentResult {

    private final String channelTransactionId;

    public ChannelPaymentResult(String channelTransactionId) {
        this.channelTransactionId = channelTransactionId;
    }

    public String getChannelTransactionId() {
        return channelTransactionId;
    }
}
