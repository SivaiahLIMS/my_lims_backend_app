-- V22: Worksheet Template + Document Slot Group

CREATE TABLE worksheet_template (
    template_id    BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenant(tenant_id),
    branch_id      BIGINT NOT NULL REFERENCES branch(branch_id),
    template_name  VARCHAR(500) NOT NULL,
    mode           VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',  -- UPLOAD | MANUAL | AUTO
    version        INTEGER      NOT NULL DEFAULT 1,
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',   -- DRAFT | IN_REVIEW | APPROVED | REJECTED
    template_json  TEXT,
    source_document_id BIGINT REFERENCES document_master(document_id),
    review_note    TEXT,
    reviewed_by    BIGINT REFERENCES app_user(user_id),
    reviewed_at    TIMESTAMP,
    approved_by    BIGINT REFERENCES app_user(user_id),
    approved_at    TIMESTAMP,
    created_by     BIGINT REFERENCES app_user(user_id),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_by    BIGINT REFERENCES app_user(user_id),
    modified_at    TIMESTAMP
);

CREATE TABLE document_slot_group (
    slot_group_id       BIGSERIAL PRIMARY KEY,
    test_case_id        BIGINT  NOT NULL REFERENCES document_test_case(test_case_id),
    document_version_id BIGINT  NOT NULL REFERENCES document_version(id),
    block_id            BIGINT  REFERENCES document_template_block(block_id),
    tenant_id           BIGINT  NOT NULL REFERENCES tenant(tenant_id),
    branch_id           BIGINT  NOT NULL REFERENCES branch(branch_id),
    group_index         INTEGER NOT NULL,
    label               TEXT,
    is_remark           BOOLEAN NOT NULL DEFAULT FALSE,
    is_timer_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(test_case_id, group_index)
);

ALTER TABLE document_field_slot
    ADD COLUMN IF NOT EXISTS slot_group_id BIGINT REFERENCES document_slot_group(slot_group_id);

ALTER TABLE document_test_case
    ADD COLUMN IF NOT EXISTS display_order NUMERIC(10,4) NOT NULL DEFAULT 0;

UPDATE document_test_case SET display_order = test_case_index WHERE display_order = 0;
