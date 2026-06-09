package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.worksheet.GenerateWorksheetsRequest;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SampleWorksheetGenerationService {

    private final SampleRepository              sampleRepository;
    private final TestAssignmentRepository      testAssignmentRepository;
    private final WorksheetMasterRepository     worksheetRepository;
    private final WorksheetReviewHistoryRepository reviewHistoryRepository;
    private final DocumentVersionRepository     documentVersionRepository;
    private final DocumentMasterRepository      documentMasterRepository;
    private final AppUserRepository             userRepository;

    @Transactional
    public List<WorksheetMaster> generateWorksheets(Long tenantId, Long branchId,
                                                     Long sampleId,
                                                     GenerateWorksheetsRequest req,
                                                     Long userId) {
        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> LimsException.notFound("Sample not found: " + sampleId));

        if (!sample.getTenant().getId().equals(tenantId)
                || !sample.getBranch().getId().equals(branchId)) {
            throw LimsException.notFound("Sample not found");
        }

        AppUser creator = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        List<WorksheetMaster> created = new ArrayList<>();

        for (GenerateWorksheetsRequest.WorksheetAssignment wa : req.getAssignments()) {
            TestAssignment assignment = testAssignmentRepository.findById(wa.getTestAssignmentId())
                    .orElseThrow(() -> LimsException.notFound(
                            "Test assignment not found: " + wa.getTestAssignmentId()));

            if (!assignment.getSampleTest().getSample().getId().equals(sampleId)) {
                throw LimsException.badRequest(
                        "Test assignment " + wa.getTestAssignmentId()
                                + " does not belong to sample " + sampleId);
            }

            DocumentVersion docVersion = null;
            if (wa.getDocumentVersionId() != null) {
                docVersion = documentVersionRepository.findById(wa.getDocumentVersionId())
                        .orElseThrow(() -> LimsException.notFound(
                                "Document version not found: " + wa.getDocumentVersionId()));
            }

            DocumentMaster template = null;
            if (wa.getTemplateId() != null) {
                template = documentMasterRepository.findById(wa.getTemplateId())
                        .orElseThrow(() -> LimsException.notFound(
                                "Document template not found: " + wa.getTemplateId()));
            }

            WorksheetMaster worksheet = WorksheetMaster.builder()
                    .tenant(sample.getTenant())
                    .branch(sample.getBranch())
                    .sample(sample)
                    .batchNo(sample.getBatchNo())
                    .documentVersion(docVersion)
                    .template(template)
                    .assignedTo(assignment.getAnalyst())
                    .assignedBy(creator)
                    .status("DRAFT")
                    .isArchived(false)
                    .createdBy(creator)
                    .createdAt(LocalDateTime.now())
                    .build();

            WorksheetMaster saved = worksheetRepository.save(worksheet);

            assignment.setWorksheet(saved);
            testAssignmentRepository.save(assignment);

            reviewHistoryRepository.save(WorksheetReviewHistory.builder()
                    .worksheet(saved)
                    .tenant(saved.getTenant())
                    .branch(saved.getBranch())
                    .oldStatus(null)
                    .newStatus("DRAFT")
                    .actionBy(creator)
                    .comments("Auto-generated from sample " + sampleId)
                    .build());

            created.add(saved);
        }

        return created;
    }

    @Transactional(readOnly = true)
    public List<WorksheetMaster> getWorksheetsForSample(Long sampleId) {
        return worksheetRepository.findBySample_Id(sampleId);
    }
}
