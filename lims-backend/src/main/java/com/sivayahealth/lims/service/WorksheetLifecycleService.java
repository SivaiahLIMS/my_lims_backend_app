package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.worksheet.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Drives the worksheet execution lifecycle:
 *   DRAFT → SUBMITTED_FOR_REVIEW → APPROVED / REJECTED → IN_PROGRESS → COMPLETED
 *
 * Also handles:
 *   - test-case-level analyst assignment / reassignment
 *   - saving slot-group values during execution
 *   - retest initiation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetLifecycleService {

    private final WorksheetMasterRepository              worksheetRepo;
    private final WorksheetTestCaseAssignmentRepository  assignmentRepo;
    private final WorksheetSlotGroupValueRepository      slotGroupValueRepo;
    private final DocumentTestCaseRepository             testCaseRepo;
    private final DocumentSlotGroupRepository            slotGroupRepo;
    private final InstrumentMasterRepository             instrumentRepo;
    private final InventoryReagentLotRepository          lotRepo;
    private final AppUserRepository                      userRepo;
    private final TenantRepository                       tenantRepo;
    private final BranchRepository                       branchRepo;

    // ── Analyst assignment ────────────────────────────────────────────────────

    /**
     * Assigns one or more analysts to the whole worksheet or to specific test cases.
     * If testCaseIds is null/empty the assignment row uses a NULL test_case_id (covers all).
     */
    @Transactional
    public void assignTestCases(Long tenantId, Long actorId, AssignTestCasesRequest req) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, req.getWorksheetId());
        AppUser assigner = loadUser(actorId);

        boolean wholeWorksheet = req.getTestCaseIds() == null || req.getTestCaseIds().isEmpty();

        for (Long analystId : req.getAnalystIds()) {
            AppUser analyst = loadUser(analystId);
            if (wholeWorksheet) {
                upsertAssignment(worksheet, null, analyst, assigner);
            } else {
                for (Long testCaseId : req.getTestCaseIds()) {
                    DocumentTestCase tc = testCaseRepo.findById(testCaseId)
                            .orElseThrow(() -> LimsException.notFound("TestCase not found: " + testCaseId));
                    upsertAssignment(worksheet, tc, analyst, assigner);
                }
            }
        }

        if ("APPROVED".equals(worksheet.getStatus())) {
            worksheet.setStatus("IN_PROGRESS");
            worksheet.setModifiedBy(assigner);
            worksheet.setModifiedAt(LocalDateTime.now());
            worksheetRepo.save(worksheet);
        }
    }

    private void upsertAssignment(WorksheetMaster worksheet, DocumentTestCase testCase,
                                  AppUser analyst, AppUser assigner) {
        Long wsId     = worksheet.getWorksheetId();
        Long tcId     = testCase != null ? testCase.getTestCaseId() : null;
        Long analystId = analyst.getId();

        WorksheetTestCaseAssignment existing = tcId != null
                ? assignmentRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseIdAndAnalyst_Id(
                        wsId, tcId, analystId).orElse(null)
                : null;

        if (existing != null) {
            existing.setStatus("ASSIGNED");
            existing.setAssignedBy(assigner);
            existing.setAssignedAt(LocalDateTime.now());
            assignmentRepo.save(existing);
        } else {
            assignmentRepo.save(WorksheetTestCaseAssignment.builder()
                    .worksheet(worksheet)
                    .testCase(testCase)
                    .analyst(analyst)
                    .assignedBy(assigner)
                    .assignedAt(LocalDateTime.now())
                    .status("ASSIGNED")
                    .build());
        }
    }

    // ── Slot-group value capture ───────────────────────────────────────────────

    @Transactional
    public WorksheetSlotGroupValue saveSlotGroupValue(Long tenantId, Long actorId,
                                                      SaveSlotGroupValueRequest req) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, req.getWorksheetId());
        DocumentSlotGroup slotGroup = slotGroupRepo.findById(req.getSlotGroupId())
                .orElseThrow(() -> LimsException.notFound("SlotGroup not found"));
        DocumentTestCase testCase = testCaseRepo.findById(req.getTestCaseId())
                .orElseThrow(() -> LimsException.notFound("TestCase not found"));
        AppUser actor = loadUser(actorId);

        WorksheetSlotGroupValue value = slotGroupValueRepo
                .findByWorksheet_WorksheetIdAndSlotGroup_SlotGroupId(
                        worksheet.getWorksheetId(), slotGroup.getSlotGroupId())
                .orElseGet(() -> WorksheetSlotGroupValue.builder()
                        .worksheet(worksheet)
                        .slotGroup(slotGroup)
                        .testCase(testCase)
                        .tenant(worksheet.getTenant())
                        .branch(worksheet.getBranch())
                        .enteredBy(actor)
                        .enteredAt(LocalDateTime.now())
                        .build());

        value.setTextValue(req.getTextValue());
        value.setElnRef(req.getElnRef());
        value.setElapsedMs(req.getElapsedMs());
        value.setModifiedBy(actor);
        value.setModifiedAt(LocalDateTime.now());

        if (req.getInstrumentId() != null) {
            value.setInstrument(instrumentRepo.findById(req.getInstrumentId())
                    .orElseThrow(() -> LimsException.notFound("Instrument not found")));
        }
        if (req.getChemicalLotId() != null) {
            value.setChemicalLot(lotRepo.findById(req.getChemicalLotId())
                    .orElseThrow(() -> LimsException.notFound("Chemical lot not found")));
        }

        return slotGroupValueRepo.save(value);
    }

    // ── Worksheet review ──────────────────────────────────────────────────────

    @Transactional
    public WorksheetMaster submitForReview(Long tenantId, Long actorId,
                                           SubmitWorksheetForReviewRequest req) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, req.getWorksheetId());
        if (!"IN_PROGRESS".equals(worksheet.getStatus())) {
            throw LimsException.badRequest("Only IN_PROGRESS worksheets can be submitted for review");
        }
        AppUser actor = loadUser(actorId);
        worksheet.setStatus("SUBMITTED_FOR_REVIEW");
        worksheet.setReviewNote(req.getNote());
        worksheet.setModifiedBy(actor);
        worksheet.setModifiedAt(LocalDateTime.now());
        return worksheetRepo.save(worksheet);
    }

    @Transactional
    public WorksheetMaster reviewWorksheet(Long tenantId, Long actorId,
                                           ReviewWorksheetRequest req) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, req.getWorksheetId());
        if (!"SUBMITTED_FOR_REVIEW".equals(worksheet.getStatus())) {
            throw LimsException.badRequest("Worksheet is not pending review");
        }
        AppUser actor = loadUser(actorId);
        worksheet.setReviewedBy(actor);
        worksheet.setReviewedAt(LocalDateTime.now());
        worksheet.setReviewNote(req.getReviewNote());
        worksheet.setModifiedBy(actor);
        worksheet.setModifiedAt(LocalDateTime.now());

        switch (req.getDecision()) {
            case "APPROVE" -> {
                worksheet.setStatus("COMPLETED");
                worksheet.setApprovedBy(actor);
                worksheet.setApprovedAt(LocalDateTime.now());
            }
            case "REJECT"  -> worksheet.setStatus("IN_PROGRESS");
            default        -> throw LimsException.badRequest("Invalid decision: " + req.getDecision());
        }
        return worksheetRepo.save(worksheet);
    }

    // ── Retest ────────────────────────────────────────────────────────────────

    @Transactional
    public WorksheetMaster initiateRetest(Long tenantId, Long actorId, Long worksheetId) {
        WorksheetMaster original = loadWorksheet(tenantId, worksheetId);
        AppUser actor = loadUser(actorId);

        WorksheetMaster retest = WorksheetMaster.builder()
                .tenant(original.getTenant())
                .branch(original.getBranch())
                .product(original.getProduct())
                .batchNo(original.getBatchNo())
                .template(original.getTemplate())
                .documentVersion(original.getDocumentVersion())
                .worksheetTemplate(original.getWorksheetTemplate())
                .retestOfWorksheet(original)
                .status("DRAFT")
                .createdBy(actor)
                .createdAt(LocalDateTime.now())
                .build();

        return worksheetRepo.save(retest);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WorksheetMaster loadWorksheet(Long tenantId, Long worksheetId) {
        WorksheetMaster w = worksheetRepo.findById(worksheetId)
                .orElseThrow(() -> LimsException.notFound("Worksheet not found"));
        if (!w.getTenant().getId().equals(tenantId)) {
            throw LimsException.forbidden("Access denied");
        }
        return w;
    }

    private AppUser loadUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found: " + userId));
    }
}
