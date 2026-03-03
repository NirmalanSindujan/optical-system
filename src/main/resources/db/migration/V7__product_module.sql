CREATE TABLE category (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL
);

CREATE TABLE product_type (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category_code VARCHAR(50) NOT NULL REFERENCES category(code)
);

CREATE INDEX idx_product_type_category_code
    ON product_type(category_code);

CREATE TABLE uom (
    code VARCHAR(30) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

INSERT INTO category (code, name)
VALUES ('GENERAL', 'General')
ON CONFLICT (code) DO NOTHING;

INSERT INTO product_type (code, name, category_code)
VALUES ('GENERAL', 'General', 'GENERAL')
ON CONFLICT (code) DO NOTHING;

INSERT INTO uom (code, name)
VALUES ('EA', 'Each')
ON CONFLICT (code) DO NOTHING;

ALTER TABLE product
    DROP COLUMN IF EXISTS sku,
    DROP COLUMN IF EXISTS selling_price,
    DROP COLUMN IF EXISTS status;

ALTER TABLE product
    ALTER COLUMN name TYPE VARCHAR(200),
    ADD COLUMN IF NOT EXISTS product_type_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS brand_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE product
SET product_type_code = 'GENERAL'
WHERE product_type_code IS NULL;

ALTER TABLE product
    ALTER COLUMN product_type_code SET NOT NULL;

ALTER TABLE product
    ADD CONSTRAINT fk_product_product_type
    FOREIGN KEY (product_type_code) REFERENCES product_type(code);

CREATE INDEX idx_product_name
    ON product(name);

CREATE INDEX idx_product_product_type_code
    ON product(product_type_code);

CREATE TABLE product_variant (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id),
    sku VARCHAR(100) NOT NULL,
    barcode VARCHAR(100),
    uom_code VARCHAR(30) NOT NULL REFERENCES uom(code),
    notes TEXT,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX uq_product_variant_sku_active
    ON product_variant (sku)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_product_variant_barcode_active
    ON product_variant (barcode)
    WHERE deleted_at IS NULL
      AND barcode IS NOT NULL;

CREATE INDEX idx_product_variant_product_id
    ON product_variant(product_id);

CREATE INDEX idx_product_variant_uom_code
    ON product_variant(uom_code);

CREATE INDEX idx_product_variant_attributes_gin
    ON product_variant USING GIN (attributes);

CREATE TABLE lens_variant_details (
    variant_id BIGINT PRIMARY KEY REFERENCES product_variant(id) ON DELETE CASCADE,
    material VARCHAR(100),
    lens_index NUMERIC(4,2),
    coating_code VARCHAR(100),
    sph NUMERIC(5,2),
    cyl NUMERIC(5,2),
    add_power NUMERIC(4,2),
    CONSTRAINT chk_lens_variant_sph_range
        CHECK (sph IS NULL OR (sph >= -24.00 AND sph <= 24.00)),
    CONSTRAINT chk_lens_variant_cyl_range
        CHECK (cyl IS NULL OR (cyl >= -12.00 AND cyl <= 12.00)),
    CONSTRAINT chk_lens_variant_add_power_range
        CHECK (add_power IS NULL OR (add_power >= -4.00 AND add_power <= 4.00)),
    CONSTRAINT chk_lens_variant_sph_step
        CHECK (sph IS NULL OR mod(sph * 100, 25) = 0),
    CONSTRAINT chk_lens_variant_cyl_step
        CHECK (cyl IS NULL OR mod(cyl * 100, 25) = 0),
    CONSTRAINT chk_lens_variant_add_power_step
        CHECK (add_power IS NULL OR mod(add_power * 100, 25) = 0)
);

CREATE TABLE frame_variant_details (
    variant_id BIGINT PRIMARY KEY REFERENCES product_variant(id) ON DELETE CASCADE,
    frame_code VARCHAR(100),
    frame_type VARCHAR(100),
    color VARCHAR(100),
    size VARCHAR(50)
);

CREATE TABLE sunglasses_variant_details (
    variant_id BIGINT PRIMARY KEY REFERENCES product_variant(id) ON DELETE CASCADE,
    description TEXT
);

CREATE TABLE accessory_variant_details (
    variant_id BIGINT PRIMARY KEY REFERENCES product_variant(id) ON DELETE CASCADE,
    item_type VARCHAR(50)
);

CREATE TABLE branch_inventory (
    branch_id BIGINT NOT NULL REFERENCES branch(id),
    variant_id BIGINT NOT NULL REFERENCES product_variant(id),
    on_hand NUMERIC(12,2) NOT NULL DEFAULT 0,
    reserved NUMERIC(12,2) NOT NULL DEFAULT 0,
    reorder_level NUMERIC(12,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (branch_id, variant_id),
    CONSTRAINT chk_branch_inventory_non_negative
        CHECK (on_hand >= 0 AND reserved >= 0 AND reorder_level >= 0)
);

CREATE INDEX idx_branch_inventory_variant_id
    ON branch_inventory(variant_id);

CREATE TABLE inventory_lot (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL REFERENCES product_variant(id),
    supplier_id BIGINT,
    purchased_at TIMESTAMP NOT NULL,
    purchase_cost NUMERIC(12,2),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'LKR',
    qty_received NUMERIC(12,2) NOT NULL,
    qty_remaining NUMERIC(12,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_inventory_lot_qty_non_negative
        CHECK (qty_received >= 0 AND qty_remaining >= 0),
    CONSTRAINT chk_inventory_lot_qty_remaining
        CHECK (qty_remaining <= qty_received)
);

CREATE INDEX idx_inventory_lot_variant_id
    ON inventory_lot(variant_id);

CREATE INDEX idx_inventory_lot_purchased_at
    ON inventory_lot(purchased_at);
