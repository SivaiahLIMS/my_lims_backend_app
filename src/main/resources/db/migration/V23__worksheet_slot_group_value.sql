-- V23: Worksheet Slot Group Values + extend worksheet_field_value

CREATE TABLE worksheet_slot_group_value (
    value_id        BIGSERIAL PRIMARY KEY,
    worksheet_id    BIGINT NOT NULL REFERENCES worksheet_master(worksheet_id),
    slot_group_id   BIGINT NOT NULL REFERENCES document_slot_group(slot_group_id),
    test_case_id    BIGINT NOT NULL REFERENCES document_test_case(test_case_id),
    tenant_id       BIGINT NOT NULL REFERENCES tenant(tenant_id),
    branch_id       BIGINT NOT NULL REFERENCES branch(branch_id),
    text_value      TEXT,
    instrument_id   BIGINT REFERENCES instrument_master(instrument_id),
    chemical_lot_id BIGINT REFERENCES inventory_reagent_lot(lot_id),
    eln_ref         TEXT,
    elapsed_ms      BIGINT,
    entered_by      BIGINT REFERENCES app_user(user_id),
    entered_at      TIMESTAMP,
    modified_by     BIGINT REFERENCES app_user(user_id),
    modified_at     TIMESTAMP,
    UNIQUE(worksheet_id, slot_group_id)
);

ALTER TABLE worksheet_field_value
    ADD COLUMN IF NOT EXISTS text_value  TEXT,
    ADD COLUMN IF NOT EXISTS ref_id      BIGINT,
    ADD COLUMN IF NOT EXISTS elapsed_ms  BIGINT;
