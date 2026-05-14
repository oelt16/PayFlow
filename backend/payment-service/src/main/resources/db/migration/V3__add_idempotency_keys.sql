-- Idempotency keys table for ensuring safe retry of payment requests
CREATE TABLE payments.idempotency_keys (
    key          VARCHAR(64)    PRIMARY KEY,
    merchant_id  VARCHAR(36)    NOT NULL,
    request_hash VARCHAR(64)    NOT NULL,
    response_body JSONB         NOT NULL,
    http_status  INT            NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_idempotency_merchant_expires
    ON payments.idempotency_keys (merchant_id, expires_at);