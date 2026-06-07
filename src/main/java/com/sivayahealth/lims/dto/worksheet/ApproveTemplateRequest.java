package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Manager/reviewer approves an IN_REVIEW template. */
@Data
public class ApproveTemplateRequest {
    private Long templateId;
    private String note;
}
