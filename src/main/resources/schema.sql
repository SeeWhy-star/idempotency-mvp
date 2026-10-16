CREATE TABLE IF NOT EXISTS payment_intent (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    merchant_order_no VARCHAR(100) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    channel_transaction_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_payment_intent_account_order UNIQUE (account_id, merchant_order_no)
);
