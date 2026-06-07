package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentSlotGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentSlotGroupRepository extends JpaRepository<DocumentSlotGroup, Long> {

    List<DocumentSlotGroup> findByTestCase_TestCaseIdOrderByGroupIndex(Long testCaseId);

    List<DocumentSlotGroup> findByDocumentVersion_IdOrderByGroupIndex(Long documentVersionId);
}
