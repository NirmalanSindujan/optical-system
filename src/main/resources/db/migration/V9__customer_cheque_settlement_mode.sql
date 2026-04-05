ALTER TABLE customer_bill_payment
    ADD COLUMN cheque_settlement_mode VARCHAR(20);

CREATE INDEX idx_customer_bill_payment_cheque_settlement_mode
    ON customer_bill_payment(cheque_settlement_mode);
