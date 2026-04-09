CREATE TABLE payments.refunds (
    id          VARCHAR(64)    PRIMARY KEY,
    payment_id  VARCHAR(64)    NOT NULL REFERENCES payments.payments(id),
    amount      NUMERIC(19, 2) NOT NULL,
    reason      TEXT,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refunds_payment ON payments.refunds (payment_id);
