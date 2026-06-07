package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;
import java.time.LocalDateTime;

/** DTO returned for a single worksheet template. */
@Data
public class WorksheetTemplateResponse {
    private Long templateId;
    private String templateName;
    private String mode;
    private Integer version;
    private String status;
    private String templateJson;
    private Long sourceDocumentId;
    private String reviewNote;
    private Long reviewedById;
    private LocalDateTime reviewedAt;
    private Long approvedById;
    private LocalDateTime approvedAt;
    private Long createdById;
    private LocalDateTime createdAt;
}
