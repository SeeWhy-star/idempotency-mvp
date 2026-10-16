package com.example.idempotencymvp.domain;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "payment_intent",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_intent_account_order",
                columnNames = {"account_id", "merchant_order_no"}))
public class PaymentIntent {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "account_id", length = 100, nullable = false)
    private String accountId;

    @Column(name = "merchant_order_no", length = 100, nullable = false)
    private String merchantOrderNo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private PaymentIntentStatus status;

    @Column(name = "channel_transaction_id", length = 100)
    private String channelTransactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentIntent() {
    }

    public PaymentIntent(
            String id,
            String accountId,
            String merchantOrderNo,
            BigDecimal amount,
            String currency,
            PaymentIntentStatus status,
            Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.merchantOrderNo = merchantOrderNo;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getMerchantOrderNo() {
        return merchantOrderNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentIntentStatus getStatus() {
        return status;
    }

    public String getChannelTransactionId() {
        return channelTransactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setChannelTransactionId(String channelTransactionId) {
        this.channelTransactionId = channelTransactionId;
        this.updatedAt = Instant.now();
    }
}
