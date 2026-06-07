package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;
import java.util.List;

/**
 * Assigns one or more analysts to test cases within a worksheet.
 * When testCaseIds is null or empty the assignment covers the entire worksheet.
 */
@Data
public class AssignTestCasesRequest {
    private Long worksheetId;
    /** NULL / empty = assign full worksheet; otherwise only the listed test cases */
    private List<Long> testCaseIds;
    private List<Long> analystIds;
}
