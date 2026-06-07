package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetTestCaseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorksheetTestCaseAssignmentRepository
        extends JpaRepository<WorksheetTestCaseAssignment, Long> {

    List<WorksheetTestCaseAssignment> findByWorksheet_WorksheetId(Long worksheetId);

    List<WorksheetTestCaseAssignment> findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
            Long worksheetId, Long testCaseId);

    List<WorksheetTestCaseAssignment> findByAnalyst_IdAndStatusIn(Long analystId, List<String> statuses);

    Optional<WorksheetTestCaseAssignment> findByWorksheet_WorksheetIdAndTestCase_TestCaseIdAndAnalyst_Id(
            Long worksheetId, Long testCaseId, Long analystId);
}
