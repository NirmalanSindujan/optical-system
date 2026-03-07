CREATE TABLE branch (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_main BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    branch_id BIGINT REFERENCES branch(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(150),
    address VARCHAR(255),
    gender VARCHAR(20),
    dob DATE,
    notes TEXT,
    pending_amount NUMERIC(12,2) DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX uq_customer_phone_active
    ON customer ((btrim(phone)))
    WHERE deleted_at IS NULL
      AND phone IS NOT NULL
      AND btrim(phone) <> '';

CREATE UNIQUE INDEX uq_customer_email_active
    ON customer ((lower(btrim(email))))
    WHERE deleted_at IS NULL
      AND email IS NOT NULL
      AND btrim(email) <> '';

CREATE TABLE customer_credit_ledger (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    amount NUMERIC(12,2) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    reference VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_customer_credit_ledger_customer_id
    ON customer_credit_ledger(customer_id);

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

CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    product_type_code VARCHAR(50) NOT NULL REFERENCES product_type(code),
    brand_name VARCHAR(150),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

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
    lens_sub_type VARCHAR(30) NOT NULL,
    lens_index NUMERIC(4,2),
    lens_type VARCHAR(30),
    coating_code VARCHAR(100),
    sph NUMERIC(5,2),
    cyl NUMERIC(5,2),
    add_power NUMERIC(4,2),
    color VARCHAR(50),
    base_curve VARCHAR(50),
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
        CHECK (add_power IS NULL OR mod(add_power * 100, 25) = 0),
    CONSTRAINT chk_lens_sub_type_allowed
        CHECK (lens_sub_type IN ('SINGLE_VISION', 'BIFOCAL', 'PROGRESSIVE', 'CONTACT_LENS'))
);

CREATE INDEX idx_lens_variant_sub_type
    ON lens_variant_details(lens_sub_type);

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

INSERT INTO branch (code, name, is_main, created_at)
VALUES ('BR01', 'Main Branch', TRUE, NOW());

INSERT INTO users (username, password_hash, role, branch_id, created_at)
VALUES (
    'superadmin',
    '$2a$10$RojsAqqVSiYq9n1iaL0v/ex8gw2P6zTgN/PoMCxvIEmfUNM6Se2OC',
    'SUPER_ADMIN',
    1,
    NOW()
);

INSERT INTO category (code, name)
VALUES
    ('LENS', 'Lens'),
    ('FRAME', 'Frame'),
    ('SUNGLASSES', 'Sunglasses'),
    ('ACCESSORY', 'Accessory');

INSERT INTO product_type (code, name, category_code)
VALUES
    ('LENS', 'Lens', 'LENS'),
    ('FRAME', 'Frame', 'FRAME'),
    ('SUNGLASSES', 'Sunglasses', 'SUNGLASSES'),
    ('ACCESSORY', 'Accessory', 'ACCESSORY');

INSERT INTO uom (code, name)
VALUES
    ('EA', 'Each'),
    ('PA', 'Pair');
