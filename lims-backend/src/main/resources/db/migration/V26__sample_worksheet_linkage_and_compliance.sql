/*
  # V26 - Sample-Worksheet Linkage, AR Number, Field Audit, E-Signature
*/

-- AR sequence table (per-tenant counter by year)
CREATE TABLE IF NOT EXISTS ar_sequence (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    year        INT    NOT NULL,
    last_seq    BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, year)
);

-- ar_number on sample
ALTER TABLE sample
    ADD COLUMN IF NOT EXISTS ar_number VARCHAR(60);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sample_ar_number
    ON sample(tenant_id, ar_number)
    WHERE ar_number IS NOT NULL;

-- sample_id FK on worksheet_master
ALTER TABLE worksheet_master
    ADD COLUMN IF NOT EXISTS sample_id BIGINT REFERENCES sample(id);

CREATE INDEX IF NOT EXISTS idx_worksheet_master_sample
    ON worksheet_master(sample_id);

-- worksheet_id FK on test_assignment
ALTER TABLE test_assignment
    ADD COLUMN IF NOT EXISTS worksheet_id BIGINT REFERENCES worksheet_master(worksheet_id);

CREATE INDEX IF NOT EXISTS idx_test_assignment_worksheet
    ON test_assignment(worksheet_id);

-- Immutable field-level audit for 21 CFR Part 11
CREATE TABLE IF NOT EXISTS worksheet_field_value_audit (
    id              BIGSERIAL PRIMARY KEY,
    worksheet_id    BIGINT NOT NULL,
    slot_id         BIGINT NOT NULL,
    test_case_id    BIGINT,
    old_value       TEXT,
    new_value       TEXT,
    changed_by      BIGINT,
    changed_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    change_type     VARCHAR(10) NOT NULL DEFAULT 'UPDATE'
);

CREATE INDEX IF NOT EXISTS idx_wfva_worksheet_slot
    ON worksheet_field_value_audit(worksheet_id, slot_id);

-- Electronic signature table
CREATE TABLE IF NOT EXISTS electronic_signature (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    action          VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       BIGINT,
    reason          TEXT,
    ip_address      VARCHAR(45),
    signed_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_esig_user_action
    ON electronic_signature(user_id, action);

CREATE INDEX IF NOT EXISTS idx_esig_entity
    ON electronic_signature(entity_type, entity_id);
