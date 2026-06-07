/*
  # Document File Storage & Audit Schema

  This migration supports storing DOCX files in Supabase Storage with a full
  audit trail for every document version uploaded to the LIMS system.

  ## New Tables

  ### document_file_audit
  Tracks every file operation (upload, review, approve, publish, retire) with:
    - who performed the action and when
    - the storage URL of the file
    - the file size and original filename
    - lifecycle state at the time of the action

  ## Storage Setup Notes
  - Bucket: lims-documents (created via Supabase Storage API)
  - Files stored at path: {tenantId}/{documentId}/v{versionNo}/{originalFilename}
  - file_url column in document_version holds the public/signed storage URL

  ## Security
  - RLS enabled on document_file_audit
  - Only authenticated users can read their tenant's records
  - Only the service role (edge function) can insert
*/

-- ============================================================
-- Create Supabase Storage bucket (idempotent via DO block)
-- ============================================================

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'lims-documents',
  'lims-documents',
  false,
  52428800,   -- 50 MB max per file
  ARRAY['application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/msword',
        'application/pdf',
        'application/octet-stream']
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Storage RLS policies
-- ============================================================

CREATE POLICY "Authenticated users can upload documents"
  ON storage.objects FOR INSERT
  TO authenticated
  WITH CHECK (bucket_id = 'lims-documents');

CREATE POLICY "Authenticated users can read their documents"
  ON storage.objects FOR SELECT
  TO authenticated
  USING (bucket_id = 'lims-documents');

-- ============================================================
-- document_file_audit table
-- ============================================================

CREATE TABLE IF NOT EXISTS document_file_audit (
  id                BIGSERIAL PRIMARY KEY,
  tenant_id         BIGINT NOT NULL,
  branch_id         BIGINT NOT NULL,
  document_id       BIGINT NOT NULL,
  version_no        INT NOT NULL,
  action            VARCHAR(30) NOT NULL,   -- UPLOADED, REVIEWED, APPROVED, PUBLISHED, RETIRED
  performed_by_id   BIGINT NOT NULL,
  performed_by_name VARCHAR(200),
  performed_at      TIMESTAMP NOT NULL DEFAULT NOW(),
  file_url          VARCHAR(1000),
  storage_path      VARCHAR(500),
  original_filename VARCHAR(300),
  file_size_bytes   BIGINT,
  mime_type         VARCHAR(100),
  lifecycle_state   VARCHAR(30),
  comment           TEXT
);

ALTER TABLE document_file_audit ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their tenant audit records"
  ON document_file_audit FOR SELECT
  TO authenticated
  USING (tenant_id = (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::BIGINT);

CREATE INDEX IF NOT EXISTS idx_doc_file_audit_doc_version
  ON document_file_audit(document_id, version_no);

CREATE INDEX IF NOT EXISTS idx_doc_file_audit_tenant
  ON document_file_audit(tenant_id);

CREATE INDEX IF NOT EXISTS idx_doc_file_audit_performed_by
  ON document_file_audit(performed_by_id);
