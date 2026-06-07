/*
  # V13 - Worksheet Validation Event Log

  Creates the worksheet_validation_event table to record every on-the-fly
  OOS/OOT validation call made during worksheet execution, and the
  worksheet_field_validation_rule table for OOS/OOT limits per field slot.
*/

CREATE TABLE IF NOT EXISTS worksheet_validation_event (
    id               BIGSERIAL    PRIMARY KEY,
    worksheet_id     BIGINT       NOT NULL,
    slot_id          BIGINT       NOT NULL,
    value            NUMERIC(20, 6),
    unit             VARCHAR(50),
    status           VARCHAR(20)  NOT NULL DEFAULT 'NO_RULE',
    severity         VARCHAR(10)  NOT NULL DEFAULT 'NONE',
    message          TEXT,
    requires_comment BOOLEAN      NOT NULL DEFAULT FALSE,
    requires_review  BOOLEAN      NOT NULL DEFAULT FALSE,
    requires_investigation BOOLEAN NOT NULL DEFAULT FALSE,
    requires_capa    BOOLEAN      NOT NULL DEFAULT FALSE,
    source           VARCHAR(30)  NOT NULL DEFAULT 'FIELD_BLUR',
    validated_by     BIGINT,
    validated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wve_worksheet_id ON worksheet_validation_event(worksheet_id);
CREATE INDEX IF NOT EXISTS idx_wve_slot_id      ON worksheet_validation_event(slot_id);
CREATE INDEX IF NOT EXISTS idx_wve_validated_at ON worksheet_validation_event(validated_at DESC);
CREATE INDEX IF NOT EXISTS idx_wve_source       ON worksheet_validation_event(source);
