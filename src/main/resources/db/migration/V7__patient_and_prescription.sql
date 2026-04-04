CREATE TABLE patient (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    name VARCHAR(150) NOT NULL,
    gender VARCHAR(20),
    dob DATE,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_patient_customer_id
    ON patient(customer_id);

CREATE INDEX idx_patient_name
    ON patient(name);

ALTER TABLE customer_bill
    ADD COLUMN patient_id BIGINT REFERENCES patient(id);

CREATE INDEX idx_customer_bill_patient_id
    ON customer_bill(patient_id);

CREATE TABLE prescription (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    customer_bill_id BIGINT UNIQUE REFERENCES customer_bill(id) ON DELETE SET NULL,
    prescription_date DATE NOT NULL,
    values JSONB NOT NULL DEFAULT '{}'::jsonb,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_prescription_patient_id
    ON prescription(patient_id);

CREATE INDEX idx_prescription_date
    ON prescription(prescription_date);
