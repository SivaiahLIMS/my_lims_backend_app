/*
  # V6 - Monitoring and Observability Tables

  Adds app_event_log and app_metric_snapshot for observability and compliance audit.
  These tables are written by the Spring backend. Foreign key constraints are omitted
  here since tenant/app_user tables are managed by Flyway in the application database.
*/

CREATE TABLE IF NOT EXISTS app_event_log (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT,
    event_type    VARCHAR(50)  NOT NULL,
    action        VARCHAR(80)  NOT NULL,
    entity_type   VARCHAR(100),
    entity_id     VARCHAR(50),
    user_id       BIGINT,
    username      VARCHAR(100),
    severity      VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    message       TEXT,
    metadata      JSONB,
    trace_id      VARCHAR(64),
    occurred_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ael_tenant_occurred  ON app_event_log(tenant_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ael_event_type       ON app_event_log(event_type, action);
CREATE INDEX IF NOT EXISTS idx_ael_user             ON app_event_log(user_id);
CREATE INDEX IF NOT EXISTS idx_ael_trace            ON app_event_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_ael_severity         ON app_event_log(severity) WHERE severity IN ('WARN','ERROR','CRITICAL');

ALTER TABLE app_event_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Service role can insert event logs"
  ON app_event_log FOR INSERT
  TO service_role
  WITH CHECK (true);

CREATE POLICY "Service role can read event logs"
  ON app_event_log FOR SELECT
  TO service_role
  USING (true);

CREATE TABLE IF NOT EXISTS app_metric_snapshot (
    id             BIGSERIAL PRIMARY KEY,
    metric_name    VARCHAR(150) NOT NULL,
    metric_value   DOUBLE PRECISION NOT NULL,
    tags           JSONB,
    snapshotted_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ams_name_time ON app_metric_snapshot(metric_name, snapshotted_at DESC);

ALTER TABLE app_metric_snapshot ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Service role can insert metric snapshots"
  ON app_metric_snapshot FOR INSERT
  TO service_role
  WITH CHECK (true);

CREATE POLICY "Service role can read metric snapshots"
  ON app_metric_snapshot FOR SELECT
  TO service_role
  USING (true);
