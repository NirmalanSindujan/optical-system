ALTER TABLE lens_variant_details
    ADD COLUMN IF NOT EXISTS lens_sub_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS lens_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS color VARCHAR(50),
    ADD COLUMN IF NOT EXISTS base_curve VARCHAR(50);

UPDATE lens_variant_details
SET lens_sub_type = 'SINGLE_VISION'
WHERE lens_sub_type IS NULL;

ALTER TABLE lens_variant_details
    ALTER COLUMN lens_sub_type SET NOT NULL;

ALTER TABLE lens_variant_details
    DROP CONSTRAINT IF EXISTS chk_lens_sub_type_allowed;

ALTER TABLE lens_variant_details
    ADD CONSTRAINT chk_lens_sub_type_allowed
        CHECK (lens_sub_type IN ('SINGLE_VISION', 'BIFOCAL', 'PROGRESSIVE', 'CONTACT_LENS'));

CREATE INDEX IF NOT EXISTS idx_lens_variant_sub_type
    ON lens_variant_details(lens_sub_type);
