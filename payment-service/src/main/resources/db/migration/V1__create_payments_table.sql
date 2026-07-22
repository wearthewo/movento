CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    payment_intent_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    plan_id VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    receipt_url TEXT,
    customer_id VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payment_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_intent_id ON payments(payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);
