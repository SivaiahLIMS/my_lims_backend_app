/*
  # V25 - Execution Engine Fixes
  Adds test_case_id to worksheet_validation_event for per-test-case OOS queries,
  and adds worksheet_master_id to eln_entry for normalized model linkage.
*/

-- Add test_case_id to worksheet_validation_event
ALTER TABLE worksheet_validation_event
    ADD COLUMN IF NOT EXISTS test_case_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_wve_test_case_id
    ON worksheet_validation_event(worksheet_id, test_case_id);

-- Add worksheet_master_id to eln_entry
ALTER TABLE eln_entry
    ADD COLUMN IF NOT EXISTS worksheet_master_id BIGINT
        REFERENCES worksheet_master(worksheet_id);

CREATE INDEX IF NOT EXISTS idx_eln_entry_worksheet_master
    ON eln_entry(worksheet_master_id);
