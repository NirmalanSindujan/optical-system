CREATE TABLE customer_bill (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT REFERENCES customer(id),
    branch_id BIGINT NOT NULL REFERENCES branch(id),
    bill_number VARCHAR(100),
    bill_date DATE NOT NULL,
    subtotal_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    balance_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'LKR',
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_customer_bill_amounts_non_negative
        CHECK (subtotal_amount >= 0 AND discount_amount >= 0 AND total_amount >= 0 AND paid_amount >= 0 AND balance_amount >= 0),
    CONSTRAINT chk_customer_bill_discount_valid
        CHECK (discount_amount <= subtotal_amount),
    CONSTRAINT chk_customer_bill_balance_matches
        CHECK (balance_amount = total_amount - paid_amount)
);

CREATE INDEX idx_customer_bill_customer_id
    ON customer_bill(customer_id);

CREATE INDEX idx_customer_bill_branch_id
    ON customer_bill(branch_id);

CREATE INDEX idx_customer_bill_bill_date
    ON customer_bill(bill_date);

CREATE TABLE customer_bill_item (
    id BIGSERIAL PRIMARY KEY,
    customer_bill_id BIGINT NOT NULL REFERENCES customer_bill(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variant(id),
    quantity NUMERIC(12,2) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    line_total NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_customer_bill_item_non_negative
        CHECK (quantity > 0 AND unit_price >= 0 AND line_total >= 0)
);

CREATE INDEX idx_customer_bill_item_bill_id
    ON customer_bill_item(customer_bill_id);

CREATE INDEX idx_customer_bill_item_variant_id
    ON customer_bill_item(variant_id);

CREATE TABLE customer_bill_payment (
    id BIGSERIAL PRIMARY KEY,
    customer_bill_id BIGINT NOT NULL REFERENCES customer_bill(id) ON DELETE CASCADE,
    payment_mode VARCHAR(20) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    cheque_number VARCHAR(100),
    cheque_date DATE,
    cheque_bank_name VARCHAR(150),
    cheque_branch_name VARCHAR(150),
    cheque_account_holder VARCHAR(150),
    reference VARCHAR(150),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_customer_bill_payment_amount_non_negative
        CHECK (amount >= 0)
);

CREATE INDEX idx_customer_bill_payment_bill_id
    ON customer_bill_payment(customer_bill_id);

CREATE INDEX idx_customer_bill_payment_mode
    ON customer_bill_payment(payment_mode);
