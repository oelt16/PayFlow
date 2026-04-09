CREATE SCHEMA IF NOT EXISTS merchants;

CREATE TABLE merchants.merchants (
    id              VARCHAR(64)  PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(320) NOT NULL,
    key_prefix      VARCHAR(16)  NOT NULL,
    key_hash        VARCHAR(128) NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deactivated_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_merchant_email ON merchants.merchants (email);
CREATE INDEX idx_merchant_key_prefix ON merchants.merchants (key_prefix);

CREATE TABLE merchants.outbox_events (
    id            UUID           PRIMARY KEY,
    aggregate_id  VARCHAR(64)    NOT NULL,
    event_type    VARCHAR(100)   NOT NULL,
    payload       JSONB          NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    published_at  TIMESTAMPTZ,
    published     BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_unpublished ON merchants.outbox_events (published, created_at);
