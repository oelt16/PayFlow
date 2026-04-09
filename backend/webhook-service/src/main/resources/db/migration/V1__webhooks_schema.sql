CREATE SCHEMA IF NOT EXISTS webhooks;

CREATE TABLE webhooks.webhook_endpoints (
    id           VARCHAR(64) PRIMARY KEY,
    merchant_id  VARCHAR(64) NOT NULL,
    url          TEXT        NOT NULL,
    secret       VARCHAR(256) NOT NULL,
    event_types  JSONB       NOT NULL DEFAULT '[]',
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_merchant ON webhooks.webhook_endpoints (merchant_id);

CREATE TABLE webhooks.webhook_deliveries (
    id              VARCHAR(64) PRIMARY KEY,
    webhook_id      VARCHAR(64) NOT NULL REFERENCES webhooks.webhook_endpoints (id),
    event_type      VARCHAR(100) NOT NULL,
    event_payload   TEXT        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts        INT         NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    next_retry_at   TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_webhook ON webhooks.webhook_deliveries (webhook_id);
CREATE INDEX idx_delivery_pending ON webhooks.webhook_deliveries (status, next_retry_at);
