WITH ranked_phones AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY btrim(phone)
               ORDER BY id
           ) AS rn
    FROM customer
    WHERE deleted_at IS NULL
      AND phone IS NOT NULL
      AND btrim(phone) <> ''
)
UPDATE customer c
SET phone = NULL
FROM ranked_phones rp
WHERE c.id = rp.id
  AND rp.rn > 1;

WITH ranked_emails AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY lower(btrim(email))
               ORDER BY id
           ) AS rn
    FROM customer
    WHERE deleted_at IS NULL
      AND email IS NOT NULL
      AND btrim(email) <> ''
)
UPDATE customer c
SET email = NULL
FROM ranked_emails re
WHERE c.id = re.id
  AND re.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_phone_active
    ON customer ((btrim(phone)))
    WHERE deleted_at IS NULL
      AND phone IS NOT NULL
      AND btrim(phone) <> '';

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_email_active
    ON customer ((lower(btrim(email))))
    WHERE deleted_at IS NULL
      AND email IS NOT NULL
      AND btrim(email) <> '';
