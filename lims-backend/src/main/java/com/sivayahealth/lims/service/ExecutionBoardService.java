package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.worksheet.ExecutionBoardView;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExecutionBoardService {

    private final SampleRepository                      sampleRepository;
    private final SampleTestRepository                  sampleTestRepository;
    private final TestAssignmentRepository              testAssignmentRepository;
    private final WorksheetMasterRepository             worksheetRepository;
    private final WorksheetTestCaseResultRepository     resultRepository;
    private final WorksheetValidationEventRepository    validationEventRepository;

    @Transactional(readOnly = true)
    public ExecutionBoardView getBoard(Long tenantId, Long branchId, Long sampleId) {
        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> LimsException.notFound("Sample not found: " + sampleId));

        if (!sample.getTenant().getId().equals(tenantId)
                || !sample.getBranch().getId().equals(branchId)) {
            throw LimsException.notFound("Sample not found");
        }

        List<SampleTest> tests = sampleTestRepository.findBySampleId(sampleId);
        List<ExecutionBoardView.SampleTestView> testViews = new ArrayList<>();

        for (SampleTest test : tests) {
            TestAssignment assignment = testAssignmentRepository
                    .findBySampleTestId(test.getId()).orElse(null);

            WorksheetMaster worksheet = null;
            List<WorksheetTestCaseResult> results = List.of();
            List<WorksheetValidationEvent> events = List.of();

            if (assignment != null && assignment.getWorksheet() != null) {
                worksheet = assignment.getWorksheet();
                results = resultRepository
                        .findByWorksheet_WorksheetIdOrderByTestCase_TestCaseIndexAsc(
                                worksheet.getWorksheetId());
                events = validationEventRepository
                        .findByWorksheetIdOrderByValidatedAtDesc(worksheet.getWorksheetId());
            }

            testViews.add(new ExecutionBoardView.SampleTestView(
                    test, assignment, worksheet, results, events));
        }

        return new ExecutionBoardView(sample, testViews);
    }
}
