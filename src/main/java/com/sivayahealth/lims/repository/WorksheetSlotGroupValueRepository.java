package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetSlotGroupValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorksheetSlotGroupValueRepository extends JpaRepository<WorksheetSlotGroupValue, Long> {

    List<WorksheetSlotGroupValue> findByWorksheet_WorksheetId(Long worksheetId);

    List<WorksheetSlotGroupValue> findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
            Long worksheetId, Long testCaseId);

    Optional<WorksheetSlotGroupValue> findByWorksheet_WorksheetIdAndSlotGroup_SlotGroupId(
            Long worksheetId, Long slotGroupId);
}
