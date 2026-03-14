CREATE TABLE supplier_payment_allocation (
    id BIGSERIAL PRIMARY KEY,
    supplier_credit_ledger_id BIGINT NOT NULL REFERENCES supplier_credit_ledger(id) ON DELETE CASCADE,
    stock_purchase_id BIGINT NOT NULL REFERENCES stock_purchase(id),
    amount NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_supplier_payment_allocation_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_supplier_payment_allocation_ledger_id
    ON supplier_payment_allocation(supplier_credit_ledger_id);

CREATE INDEX idx_supplier_payment_allocation_purchase_id
    ON supplier_payment_allocation(stock_purchase_id);
