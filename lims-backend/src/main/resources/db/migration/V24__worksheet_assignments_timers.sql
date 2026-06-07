-- V24: Worksheet test-case assignments, timer log, worksheet_master extensions

CREATE TABLE worksheet_test_case_assignment (
    assignment_id           BIGSERIAL PRIMARY KEY,
    worksheet_id            BIGINT  NOT NULL REFERENCES worksheet_master(worksheet_id),
    test_case_id            BIGINT  REFERENCES document_test_case(test_case_id),  -- NULL = entire worksheet
    analyst_id              BIGINT  NOT NULL REFERENCES app_user(user_id),
    assigned_by             BIGINT  NOT NULL REFERENCES app_user(user_id),
    assigned_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    status                  VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',  -- ASSIGNED | IN_PROGRESS | COMPLETED | REASSIGNED
    retest_of_assignment_id BIGINT  REFERENCES worksheet_test_case_assignment(assignment_id),
    UNIQUE(worksheet_id, test_case_id, analyst_id)
);

CREATE TABLE worksheet_timer_log (
    timer_log_id    BIGSERIAL PRIMARY KEY,
    worksheet_id    BIGINT NOT NULL REFERENCES worksheet_master(worksheet_id),
    test_case_id    BIGINT NOT NULL REFERENCES document_test_case(test_case_id),
    slot_group_id   BIGINT REFERENCES document_slot_group(slot_group_id),
    timer_id        VARCHAR(100),
    started_by      BIGINT REFERENCES app_user(user_id),
    started_at      TIMESTAMP,
    stopped_by      BIGINT REFERENCES app_user(user_id),
    stopped_at      TIMESTAMP,
    paused_at       TIMESTAMP,
    resumed_at      TIMESTAMP,
    elapsed_ms      BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'RUNNING'  -- RUNNING | PAUSED | STOPPED
);

ALTER TABLE worksheet_master
    ADD COLUMN IF NOT EXISTS reviewed_by           BIGINT REFERENCES app_user(user_id),
    ADD COLUMN IF NOT EXISTS reviewed_at           TIMESTAMP,
    ADD COLUMN IF NOT EXISTS review_note           TEXT,
    ADD COLUMN IF NOT EXISTS approved_by           BIGINT REFERENCES app_user(user_id),
    ADD COLUMN IF NOT EXISTS approved_at           TIMESTAMP,
    ADD COLUMN IF NOT EXISTS worksheet_template_id BIGINT REFERENCES worksheet_template(template_id),
    ADD COLUMN IF NOT EXISTS retest_of_worksheet_id BIGINT REFERENCES worksheet_master(worksheet_id);
