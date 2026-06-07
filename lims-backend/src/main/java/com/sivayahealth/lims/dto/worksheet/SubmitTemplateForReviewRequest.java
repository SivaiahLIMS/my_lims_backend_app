package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Moves a DRAFT template into IN_REVIEW. */
@Data
public class SubmitTemplateForReviewRequest {
    private Long templateId;
    private String note;
}
