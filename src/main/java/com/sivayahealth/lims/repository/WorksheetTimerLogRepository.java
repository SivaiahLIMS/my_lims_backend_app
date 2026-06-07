package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetTimerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorksheetTimerLogRepository extends JpaRepository<WorksheetTimerLog, Long> {

    List<WorksheetTimerLog> findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
            Long worksheetId, Long testCaseId);

    Optional<WorksheetTimerLog> findByWorksheet_WorksheetIdAndTimerIdAndStatusNot(
            Long worksheetId, String timerId, String excludedStatus);

    Optional<WorksheetTimerLog> findByWorksheet_WorksheetIdAndSlotGroup_SlotGroupIdAndStatus(
            Long worksheetId, Long slotGroupId, String status);
}
