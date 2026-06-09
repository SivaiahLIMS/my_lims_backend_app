package com.sivayahealth.lims.dto.worksheet;

import com.sivayahealth.lims.entity.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionBoardView {

    private Sample sample;
    private List<SampleTestView> tests;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SampleTestView {
        private SampleTest sampleTest;
        private TestAssignment assignment;
        private WorksheetMaster worksheet;
        private List<WorksheetTestCaseResult> results;
        private List<WorksheetValidationEvent> validationEvents;
    }
}
