CREATE TABLE stock_purchase (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES supplier(id),
    branch_id BIGINT NOT NULL REFERENCES branch(id),
    bill_number VARCHAR(100),
    purchase_date DATE NOT NULL,
    payment_mode VARCHAR(20) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    balance_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'LKR',
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_stock_purchase_amounts_non_negative
        CHECK (total_amount >= 0 AND paid_amount >= 0 AND balance_amount >= 0),
    CONSTRAINT chk_stock_purchase_balance_matches
        CHECK (balance_amount = total_amount - paid_amount)
);

CREATE INDEX idx_stock_purchase_supplier_id
    ON stock_purchase(supplier_id);

CREATE INDEX idx_stock_purchase_branch_id
    ON stock_purchase(branch_id);

CREATE INDEX idx_stock_purchase_purchase_date
    ON stock_purchase(purchase_date);

CREATE TABLE stock_purchase_item (
    id BIGSERIAL PRIMARY KEY,
    stock_purchase_id BIGINT NOT NULL REFERENCES stock_purchase(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variant(id),
    quantity NUMERIC(12,2) NOT NULL,
    purchase_price NUMERIC(12,2) NOT NULL,
    line_total NUMERIC(12,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_stock_purchase_item_non_negative
        CHECK (quantity > 0 AND purchase_price >= 0 AND line_total >= 0)
);

CREATE INDEX idx_stock_purchase_item_purchase_id
    ON stock_purchase_item(stock_purchase_id);

CREATE INDEX idx_stock_purchase_item_variant_id
    ON stock_purchase_item(variant_id);

CREATE TABLE supplier_credit_ledger (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES supplier(id),
    stock_purchase_id BIGINT REFERENCES stock_purchase(id),
    entry_date DATE NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    payment_mode VARCHAR(20),
    reference VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_supplier_credit_ledger_amount_non_zero
        CHECK (amount <> 0)
);

CREATE INDEX idx_supplier_credit_ledger_supplier_id
    ON supplier_credit_ledger(supplier_id);

CREATE INDEX idx_supplier_credit_ledger_entry_date
    ON supplier_credit_ledger(entry_date);
