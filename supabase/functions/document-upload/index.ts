import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Client-Info, Apikey",
};

/**
 * document-upload Edge Function
 *
 * Accepts a multipart/form-data POST with:
 *   - file        : the DOCX binary
 *   - tenantId    : tenant identifier
 *   - branchId    : branch identifier
 *   - documentId  : document master ID
 *   - versionNo   : version number
 *   - uploadedById: user ID performing the upload
 *   - uploadedByName: display name of the uploader
 *
 * Stores the file in Supabase Storage bucket "lims-documents" at:
 *   {tenantId}/{documentId}/v{versionNo}/{originalFilename}
 *
 * Inserts an audit record into document_file_audit.
 *
 * Returns: { storagePath, fileUrl, fileSize, mimeType, auditId }
 */
Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: corsHeaders });
  }

  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const formData = await req.formData();

    const file         = formData.get("file") as File;
    const tenantId     = formData.get("tenantId") as string;
    const branchId     = formData.get("branchId") as string;
    const documentId   = formData.get("documentId") as string;
    const versionNo    = formData.get("versionNo") as string;
    const uploadedById = formData.get("uploadedById") as string;
    const uploadedByName = formData.get("uploadedByName") as string | null;

    if (!file || !tenantId || !documentId || !versionNo || !uploadedById) {
      return new Response(
        JSON.stringify({ error: "Missing required fields: file, tenantId, documentId, versionNo, uploadedById" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Build storage path
    const safeFilename = file.name.replace(/[^a-zA-Z0-9._-]/g, "_");
    const storagePath  = `${tenantId}/${documentId}/v${versionNo}/${safeFilename}`;

    // Upload to Supabase Storage
    const fileBytes = await file.arrayBuffer();
    const { error: uploadError } = await supabase.storage
      .from("lims-documents")
      .upload(storagePath, fileBytes, {
        contentType: file.type || "application/octet-stream",
        upsert: true,
      });

    if (uploadError) {
      throw new Error(`Storage upload failed: ${uploadError.message}`);
    }

    // Get a signed URL valid for 10 years (long-lived, for audit/download)
    const { data: signedData, error: signedError } = await supabase.storage
      .from("lims-documents")
      .createSignedUrl(storagePath, 60 * 60 * 24 * 365 * 10);

    if (signedError) {
      throw new Error(`Failed to create signed URL: ${signedError.message}`);
    }

    const fileUrl = signedData.signedUrl;

    // Insert audit record
    const { data: auditRow, error: auditError } = await supabase
      .from("document_file_audit")
      .insert({
        tenant_id:         parseInt(tenantId),
        branch_id:         parseInt(branchId || "0"),
        document_id:       parseInt(documentId),
        version_no:        parseInt(versionNo),
        action:            "UPLOADED",
        performed_by_id:   parseInt(uploadedById),
        performed_by_name: uploadedByName || null,
        performed_at:      new Date().toISOString(),
        file_url:          fileUrl,
        storage_path:      storagePath,
        original_filename: file.name,
        file_size_bytes:   fileBytes.byteLength,
        mime_type:         file.type || "application/octet-stream",
        lifecycle_state:   "DRAFT",
      })
      .select("id")
      .single();

    if (auditError) {
      console.error("Audit insert warning:", auditError.message);
    }

    return new Response(
      JSON.stringify({
        storagePath,
        fileUrl,
        fileSize:  fileBytes.byteLength,
        mimeType:  file.type || "application/octet-stream",
        auditId:   auditRow?.id ?? null,
        versionNo: parseInt(versionNo),
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );

  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    console.error("document-upload error:", message);
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
