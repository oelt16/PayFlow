CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE payments.payments (
    id              VARCHAR(64)    PRIMARY KEY,
    merchant_id     VARCHAR(64)    NOT NULL,
    amount          NUMERIC(19, 2) NOT NULL,
    currency        CHAR(3)        NOT NULL,
    status          VARCHAR(32)    NOT NULL,
    description     TEXT,
    card_last4      CHAR(4),
    card_brand      VARCHAR(20),
    card_exp_month  SMALLINT,
    card_exp_year   SMALLINT,
    metadata        JSONB          NOT NULL DEFAULT '{}',
    client_secret   VARCHAR(128)   NOT NULL,
    total_refunded  NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    captured_at     TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_payments_merchant ON payments.payments (merchant_id);
CREATE INDEX idx_payments_status ON payments.payments (status);

CREATE TABLE payments.outbox_events (
    id            UUID           PRIMARY KEY,
    aggregate_id  VARCHAR(64)    NOT NULL,
    event_type    VARCHAR(100)   NOT NULL,
    payload       JSONB          NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    published_at  TIMESTAMPTZ,
    published     BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_unpublished ON payments.outbox_events (published, created_at);
