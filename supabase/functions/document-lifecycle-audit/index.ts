import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Client-Info, Apikey",
};

/**
 * document-lifecycle-audit Edge Function
 *
 * Records a lifecycle event for a document version into document_file_audit.
 * Called by the Spring backend on every lifecycle transition:
 *   SUBMITTED_FOR_REVIEW, APPROVED, PUBLISHED, RETIRED
 *
 * Accepts multipart/form-data:
 *   - tenantId, branchId, documentId, versionNo
 *   - action         : lifecycle action string
 *   - performedById  : user ID
 *   - performedByName: display name (optional)
 *   - lifecycleState : current state after transition
 *   - fileUrl        : signed URL (optional)
 *   - storagePath    : storage path (optional)
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

    const tenantId        = formData.get("tenantId") as string;
    const branchId        = formData.get("branchId") as string;
    const documentId      = formData.get("documentId") as string;
    const versionNo       = formData.get("versionNo") as string;
    const action          = formData.get("action") as string;
    const performedById   = formData.get("performedById") as string;
    const performedByName = formData.get("performedByName") as string | null;
    const lifecycleState  = formData.get("lifecycleState") as string | null;
    const fileUrl         = formData.get("fileUrl") as string | null;
    const storagePath     = formData.get("storagePath") as string | null;
    const originalFilename = formData.get("originalFilename") as string | null;
    const fileSizeBytes   = formData.get("fileSizeBytes") as string | null;
    const mimeType        = formData.get("mimeType") as string | null;

    if (!tenantId || !documentId || !versionNo || !action || !performedById) {
      return new Response(
        JSON.stringify({ error: "Missing required fields" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const { data, error } = await supabase
      .from("document_file_audit")
      .insert({
        tenant_id:         parseInt(tenantId),
        branch_id:         parseInt(branchId || "0"),
        document_id:       parseInt(documentId),
        version_no:        parseInt(versionNo),
        action:            action,
        performed_by_id:   parseInt(performedById),
        performed_by_name: performedByName || null,
        performed_at:      new Date().toISOString(),
        file_url:          fileUrl || null,
        storage_path:      storagePath || null,
        original_filename: originalFilename || null,
        file_size_bytes:   fileSizeBytes ? parseInt(fileSizeBytes) : null,
        mime_type:         mimeType || null,
        lifecycle_state:   lifecycleState || null,
      })
      .select("id")
      .single();

    if (error) {
      throw new Error(`Audit insert failed: ${error.message}`);
    }

    return new Response(
      JSON.stringify({ auditId: data?.id }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );

  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    console.error("document-lifecycle-audit error:", message);
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
