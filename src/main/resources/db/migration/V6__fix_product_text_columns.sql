DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'product'
          AND column_name = 'name'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE product
            ALTER COLUMN name TYPE VARCHAR(200)
            USING CASE
                WHEN name IS NULL THEN NULL
                ELSE convert_from(name, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'product'
          AND column_name = 'brand_name'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE product
            ALTER COLUMN brand_name TYPE VARCHAR(150)
            USING CASE
                WHEN brand_name IS NULL THEN NULL
                ELSE convert_from(brand_name, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'product'
          AND column_name = 'description'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE product
            ALTER COLUMN description TYPE TEXT
            USING CASE
                WHEN description IS NULL THEN NULL
                ELSE convert_from(description, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'product_variant'
          AND column_name = 'sku'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE product_variant
            ALTER COLUMN sku TYPE VARCHAR(100)
            USING CASE
                WHEN sku IS NULL THEN NULL
                ELSE convert_from(sku, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'product_variant'
          AND column_name = 'barcode'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE product_variant
            ALTER COLUMN barcode TYPE VARCHAR(100)
            USING CASE
                WHEN barcode IS NULL THEN NULL
                ELSE convert_from(barcode, 'UTF8')
            END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'product_variant'
          AND column_name = 'notes'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE product_variant
            ALTER COLUMN notes TYPE TEXT
            USING CASE
                WHEN notes IS NULL THEN NULL
                ELSE convert_from(notes, 'UTF8')
            END;
    END IF;
END $$;
