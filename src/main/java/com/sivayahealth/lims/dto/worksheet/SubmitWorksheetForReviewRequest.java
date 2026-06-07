package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Submits a worksheet for manager review after execution. */
@Data
public class SubmitWorksheetForReviewRequest {
    private Long worksheetId;
    private String note;
}
