package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Manager/reviewer rejects an IN_REVIEW template back to DRAFT. */
@Data
public class RejectTemplateRequest {
    private Long templateId;
    private String reviewNote;
}
