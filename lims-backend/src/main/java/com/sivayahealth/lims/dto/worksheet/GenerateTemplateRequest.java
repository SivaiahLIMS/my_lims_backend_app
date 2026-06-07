package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Request to auto-generate a worksheet template via LLM. */
@Data
public class GenerateTemplateRequest {
    private Long branchId;
    /** Product name or description used as context for the LLM */
    private String productDescription;
    /** Specific test types the user wants included, e.g. "assay, dissolution, pH" */
    private String requestedTests;
    /** Additional free-text instructions or context */
    private String additionalContext;
}
