ALTER TABLE customer_bill_payment
    ADD COLUMN cheque_status VARCHAR(20),
    ADD COLUMN cheque_status_notes TEXT,
    ADD COLUMN cheque_status_changed_at TIMESTAMP;

UPDATE customer_bill_payment
SET cheque_status = 'PENDING',
    cheque_status_changed_at = COALESCE(updated_at, created_at, NOW())
WHERE payment_mode = 'CHEQUE'
  AND cheque_status IS NULL;

CREATE INDEX idx_customer_bill_payment_cheque_status
    ON customer_bill_payment(cheque_status);

ALTER TABLE supplier_credit_ledger
    ADD COLUMN cheque_number VARCHAR(100),
    ADD COLUMN cheque_date DATE,
    ADD COLUMN cheque_bank_name VARCHAR(150),
    ADD COLUMN cheque_branch_name VARCHAR(150),
    ADD COLUMN cheque_account_holder VARCHAR(150),
    ADD COLUMN cheque_status VARCHAR(20),
    ADD COLUMN cheque_status_notes TEXT,
    ADD COLUMN cheque_status_changed_at TIMESTAMP;

UPDATE supplier_credit_ledger
SET cheque_status = 'PENDING',
    cheque_status_changed_at = COALESCE(updated_at, created_at, NOW())
WHERE payment_mode = 'CHEQUE'
  AND cheque_status IS NULL;

CREATE INDEX idx_supplier_credit_ledger_cheque_status
    ON supplier_credit_ledger(cheque_status);
