CREATE TABLE expense_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    recurring_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    reminder_date DATE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_expense_category_reminder
        CHECK (
            (recurring_type = 'NONE' AND reminder_date IS NULL)
            OR (recurring_type <> 'NONE' AND reminder_date IS NOT NULL)
        )
);

CREATE UNIQUE INDEX uq_expense_category_name_active
    ON expense_category(LOWER(name))
    WHERE deleted_at IS NULL;

CREATE TABLE expense (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branch(id),
    category_id BIGINT NOT NULL REFERENCES expense_category(id),
    amount NUMERIC(12,2) NOT NULL,
    description TEXT,
    source VARCHAR(20) NOT NULL,
    expense_date DATE NOT NULL,
    reference VARCHAR(150),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_expense_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT chk_expense_source CHECK (source IN ('CASH', 'BANK'))
);

CREATE INDEX idx_expense_branch_id
    ON expense(branch_id);

CREATE INDEX idx_expense_category_id
    ON expense(category_id);

CREATE INDEX idx_expense_source
    ON expense(source);

CREATE INDEX idx_expense_date
    ON expense(expense_date);
