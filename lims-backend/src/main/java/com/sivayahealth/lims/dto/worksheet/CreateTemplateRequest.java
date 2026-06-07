package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Request body for creating a new worksheet template (any mode). */
@Data
public class CreateTemplateRequest {

    private Long branchId;
    private String templateName;

    /**
     * UPLOAD  – templateJson produced by DocxParserService (sent by frontend after file parse)
     * MANUAL  – templateJson typed/pasted directly
     * AUTO    – templateJson produced by LLM; optionally corrected before submit
     */
    private String mode;

    /** Already-serialised template JSON; the backend stores it verbatim. */
    private String templateJson;

    /** When mode=UPLOAD, the ID of the uploaded DocumentMaster (already stored by file upload endpoint). */
    private Long sourceDocumentId;
}
