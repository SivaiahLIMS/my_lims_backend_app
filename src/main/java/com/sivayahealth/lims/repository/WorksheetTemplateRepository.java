package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorksheetTemplateRepository extends JpaRepository<WorksheetTemplate, Long> {

    List<WorksheetTemplate> findByTenantIdAndBranchIdOrderByCreatedAtDesc(
            Long tenantId, Long branchId);

    List<WorksheetTemplate> findByTenantIdAndBranchIdAndStatusOrderByCreatedAtDesc(
            Long tenantId, Long branchId, String status);

    Optional<WorksheetTemplate> findByTemplateIdAndTenantId(Long templateId, Long tenantId);
}
