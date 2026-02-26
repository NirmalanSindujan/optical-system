CREATE TABLE supplier (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(150),
    address VARCHAR(255),
    notes TEXT,
    pending_amount NUMERIC(12,2) DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

WITH ranked_supplier_phones AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY btrim(phone)
               ORDER BY id
           ) AS rn
    FROM supplier
    WHERE deleted_at IS NULL
      AND phone IS NOT NULL
      AND btrim(phone) <> ''
)
UPDATE supplier s
SET phone = NULL
FROM ranked_supplier_phones rsp
WHERE s.id = rsp.id
  AND rsp.rn > 1;

WITH ranked_supplier_emails AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY lower(btrim(email))
               ORDER BY id
           ) AS rn
    FROM supplier
    WHERE deleted_at IS NULL
      AND email IS NOT NULL
      AND btrim(email) <> ''
)
UPDATE supplier s
SET email = NULL
FROM ranked_supplier_emails rse
WHERE s.id = rse.id
  AND rse.rn > 1;

CREATE UNIQUE INDEX uq_supplier_phone_active
    ON supplier ((btrim(phone)))
    WHERE deleted_at IS NULL
      AND phone IS NOT NULL
      AND btrim(phone) <> '';

CREATE UNIQUE INDEX uq_supplier_email_active
    ON supplier ((lower(btrim(email))))
    WHERE deleted_at IS NULL
      AND email IS NOT NULL
      AND btrim(email) <> '';

CREATE TABLE supplier_product (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES supplier(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX uq_supplier_product_active
    ON supplier_product (supplier_id, product_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_supplier_product_supplier_id
    ON supplier_product(supplier_id);

CREATE INDEX idx_supplier_product_product_id
    ON supplier_product(product_id);
