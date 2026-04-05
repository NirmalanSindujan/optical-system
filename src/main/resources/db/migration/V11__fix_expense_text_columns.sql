DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'expense_category'
          AND column_name = 'name'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE expense_category
            ALTER COLUMN name TYPE VARCHAR(120)
            USING CASE
                WHEN name IS NULL THEN NULL
                ELSE convert_from(name, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'expense_category'
          AND column_name = 'description'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE expense_category
            ALTER COLUMN description TYPE TEXT
            USING CASE
                WHEN description IS NULL THEN NULL
                ELSE convert_from(description, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'expense'
          AND column_name = 'description'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE expense
            ALTER COLUMN description TYPE TEXT
            USING CASE
                WHEN description IS NULL THEN NULL
                ELSE convert_from(description, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'expense'
          AND column_name = 'reference'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE expense
            ALTER COLUMN reference TYPE VARCHAR(150)
            USING CASE
                WHEN reference IS NULL THEN NULL
                ELSE convert_from(reference, 'UTF8')
            END;
    END IF;
END $$;
