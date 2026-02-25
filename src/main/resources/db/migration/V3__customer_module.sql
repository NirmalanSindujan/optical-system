ALTER TABLE customer
    ADD COLUMN email VARCHAR(150),
    ADD COLUMN address VARCHAR(255),
    ADD COLUMN gender VARCHAR(20),
    ADD COLUMN dob DATE,
    ADD COLUMN notes TEXT,
    ADD COLUMN pending_amount NUMERIC(12,2) DEFAULT 0;

CREATE TABLE customer_credit_ledger (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    amount NUMERIC(12,2) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    reference VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_customer_credit_ledger_customer_id
    ON customer_credit_ledger(customer_id);
