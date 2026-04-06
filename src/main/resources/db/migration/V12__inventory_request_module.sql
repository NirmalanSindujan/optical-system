CREATE TABLE inventory_request (
    id BIGSERIAL PRIMARY KEY,
    requesting_branch_id BIGINT NOT NULL REFERENCES branch(id),
    supplying_branch_id BIGINT NOT NULL REFERENCES branch(id),
    requested_by_user_id BIGINT NOT NULL REFERENCES users(id),
    processed_by_user_id BIGINT REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    request_note TEXT,
    decision_note TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_inventory_request_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT chk_inventory_request_branches_distinct
        CHECK (requesting_branch_id <> supplying_branch_id)
);

CREATE INDEX idx_inventory_request_requesting_branch
    ON inventory_request(requesting_branch_id);

CREATE INDEX idx_inventory_request_supplying_branch
    ON inventory_request(supplying_branch_id);

CREATE INDEX idx_inventory_request_status
    ON inventory_request(status);

CREATE INDEX idx_inventory_request_created_at
    ON inventory_request(created_at);

CREATE TABLE inventory_request_item (
    id BIGSERIAL PRIMARY KEY,
    inventory_request_id BIGINT NOT NULL REFERENCES inventory_request(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variant(id),
    requested_quantity NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_inventory_request_item_qty_positive
        CHECK (requested_quantity > 0)
);

CREATE INDEX idx_inventory_request_item_request_id
    ON inventory_request_item(inventory_request_id);

CREATE INDEX idx_inventory_request_item_variant_id
    ON inventory_request_item(variant_id);
