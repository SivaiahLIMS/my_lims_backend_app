package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetFieldValueAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorksheetFieldValueAuditRepository
        extends JpaRepository<WorksheetFieldValueAudit, Long> {

    List<WorksheetFieldValueAudit> findByWorksheetIdAndSlotIdOrderByChangedAtDesc(
            Long worksheetId, Long slotId);

    List<WorksheetFieldValueAudit> findByWorksheetIdOrderByChangedAtDesc(Long worksheetId);
}
