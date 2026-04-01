DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'customer_bill'
          AND column_name = 'bill_number'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE customer_bill
            ALTER COLUMN bill_number TYPE VARCHAR(100)
            USING CASE
                WHEN bill_number IS NULL THEN NULL
                ELSE convert_from(bill_number, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'customer_bill'
          AND column_name = 'currency_code'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE customer_bill
            ALTER COLUMN currency_code TYPE VARCHAR(3)
            USING CASE
                WHEN currency_code IS NULL THEN NULL
                ELSE convert_from(currency_code, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'customer_bill'
          AND column_name = 'notes'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE customer_bill
            ALTER COLUMN notes TYPE TEXT
            USING CASE
                WHEN notes IS NULL THEN NULL
                ELSE convert_from(notes, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'customer_bill_payment'
          AND column_name = 'cheque_number'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE customer_bill_payment
            ALTER COLUMN cheque_number TYPE VARCHAR(100)
            USING CASE
                WHEN cheque_number IS NULL THEN NULL
                ELSE convert_from(cheque_number, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'customer_bill_payment'
          AND column_name = 'cheque_bank_name'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE customer_bill_payment
            ALTER COLUMN cheque_bank_name TYPE VARCHAR(150)
            USING CASE
                WHEN cheque_bank_name IS NULL THEN NULL
                ELSE convert_from(cheque_bank_name, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'customer_bill_payment'
          AND column_name = 'cheque_branch_name'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE customer_bill_payment
            ALTER COLUMN cheque_branch_name TYPE VARCHAR(150)
            USING CASE
                WHEN cheque_branch_name IS NULL THEN NULL
                ELSE convert_from(cheque_branch_name, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'customer_bill_payment'
          AND column_name = 'cheque_account_holder'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE customer_bill_payment
            ALTER COLUMN cheque_account_holder TYPE VARCHAR(150)
            USING CASE
                WHEN cheque_account_holder IS NULL THEN NULL
                ELSE convert_from(cheque_account_holder, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'customer_bill_payment'
          AND column_name = 'reference'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE customer_bill_payment
            ALTER COLUMN reference TYPE VARCHAR(150)
            USING CASE
                WHEN reference IS NULL THEN NULL
                ELSE convert_from(reference, 'UTF8')
            END;
    END IF;
END $$;
