package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;
import java.util.List;

@Data
public class GenerateWorksheetsRequest {
    /** Map each test assignment to a document version. Key = testAssignmentId, value = documentVersionId */
    private List<WorksheetAssignment> assignments;

    @Data
    public static class WorksheetAssignment {
        private Long testAssignmentId;
        private Long documentVersionId;
        private Long templateId;
    }
}
