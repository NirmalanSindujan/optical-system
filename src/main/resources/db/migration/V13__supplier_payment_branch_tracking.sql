ALTER TABLE supplier_credit_ledger
    ADD COLUMN branch_id BIGINT REFERENCES branch(id);

WITH allocated_branch AS (
    SELECT
        a.supplier_credit_ledger_id AS ledger_id,
        CASE
            WHEN COUNT(DISTINCT p.branch_id) = 1 THEN MIN(p.branch_id)
            ELSE NULL
        END AS resolved_branch_id
    FROM supplier_payment_allocation a
    JOIN stock_purchase p ON p.id = a.stock_purchase_id
    WHERE a.deleted_at IS NULL
      AND p.deleted_at IS NULL
    GROUP BY a.supplier_credit_ledger_id
)
UPDATE supplier_credit_ledger l
SET branch_id = allocated_branch.resolved_branch_id
FROM allocated_branch
WHERE l.id = allocated_branch.ledger_id
  AND l.entry_type = 'PAYMENT'
  AND l.branch_id IS NULL
  AND allocated_branch.resolved_branch_id IS NOT NULL;

UPDATE supplier_credit_ledger
SET branch_id = (
    SELECT b.id
    FROM branch b
    WHERE b.is_main = TRUE
      AND b.deleted_at IS NULL
    ORDER BY b.id
    LIMIT 1
)
WHERE entry_type = 'PAYMENT'
  AND branch_id IS NULL;

ALTER TABLE supplier_credit_ledger
    ADD CONSTRAINT chk_supplier_credit_ledger_payment_branch
        CHECK (entry_type <> 'PAYMENT' OR branch_id IS NOT NULL);

CREATE INDEX idx_supplier_credit_ledger_branch_id
    ON supplier_credit_ledger(branch_id);
