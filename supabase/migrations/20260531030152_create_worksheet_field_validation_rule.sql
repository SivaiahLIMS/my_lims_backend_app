/*
  # V14 - Worksheet Field Validation Rule Table

  Creates the worksheet_field_validation_rule table for OOS/OOT numeric limits
  per worksheet field slot, including rule type and action flags.

  ## New Table: worksheet_field_validation_rule
  - id                        — primary key
  - slot_id                   — FK to document_field_slot(slot_id)
  - field_type                — NUMBER (default)
  - rule_type                 — RANGE | MIN_ONLY | MAX_ONLY | TARGET_VARIANCE | FORMULA_BASED
  - unit                      — unit of measurement
  - oos_lower_limit           — Out of Specification lower bound
  - oos_upper_limit           — Out of Specification upper bound
  - oot_lower_limit           — Out of Trend lower bound
  - oot_upper_limit           — Out of Trend upper bound
  - require_comment_on_oos    — OOS requires justification comment
  - require_comment_on_oot    — OOT requires justification comment
  - require_review_on_oot     — OOT requires supervisor review
  - require_investigation_on_oos — OOS requires investigation record
  - require_capa_on_oos       — OOS requires CAPA record
  - active                    — whether this rule is currently active
  - created_by / created_at   — audit
  - updated_by / updated_at   — audit
*/

CREATE TABLE IF NOT EXISTS worksheet_field_validation_rule (
    id                          BIGSERIAL       PRIMARY KEY,
    slot_id                     BIGINT          REFERENCES document_field_slot(slot_id),
    field_type                  VARCHAR(50)     NOT NULL DEFAULT 'NUMBER',
    rule_type                   VARCHAR(30)     NOT NULL DEFAULT 'RANGE',
    unit                        VARCHAR(50),
    oos_lower_limit             NUMERIC(20, 6),
    oos_upper_limit             NUMERIC(20, 6),
    oot_lower_limit             NUMERIC(20, 6),
    oot_upper_limit             NUMERIC(20, 6),
    require_comment_on_oos      BOOLEAN         NOT NULL DEFAULT TRUE,
    require_comment_on_oot      BOOLEAN         NOT NULL DEFAULT TRUE,
    require_review_on_oot       BOOLEAN         NOT NULL DEFAULT FALSE,
    require_investigation_on_oos BOOLEAN        NOT NULL DEFAULT FALSE,
    require_capa_on_oos         BOOLEAN         NOT NULL DEFAULT FALSE,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by                  BIGINT          REFERENCES app_user(id),
    created_at                  TIMESTAMP,
    updated_by                  BIGINT          REFERENCES app_user(id),
    updated_at                  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wfvr_slot_id ON worksheet_field_validation_rule(slot_id);
CREATE INDEX IF NOT EXISTS idx_wfvr_active  ON worksheet_field_validation_rule(active);
