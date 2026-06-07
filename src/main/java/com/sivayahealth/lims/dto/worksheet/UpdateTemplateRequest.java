package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Partial update of a template that is still in DRAFT status. */
@Data
public class UpdateTemplateRequest {
    private String templateName;
    private String templateJson;
    private String mode;
}
