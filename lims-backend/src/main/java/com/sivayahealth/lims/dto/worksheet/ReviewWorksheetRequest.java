package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Manager approves or rejects an executed worksheet. */
@Data
public class ReviewWorksheetRequest {
    private Long worksheetId;
    /** APPROVE | REJECT */
    private String decision;
    private String reviewNote;
}
