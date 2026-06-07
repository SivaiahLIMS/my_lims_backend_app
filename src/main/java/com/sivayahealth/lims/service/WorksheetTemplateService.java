package com.sivayahealth.lims.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivayahealth.lims.dto.worksheet.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CRUD and lifecycle management for WorksheetTemplate.
 * Lifecycle: DRAFT → IN_REVIEW → APPROVED / REJECTED → (back to DRAFT if rejected)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetTemplateService {

    private final WorksheetTemplateRepository templateRepo;
    private final AppUserRepository           userRepo;
    private final TenantRepository            tenantRepo;
    private final BranchRepository            branchRepo;
    private final DocumentMasterRepository    documentMasterRepo;
    private final ObjectMapper                objectMapper;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public WorksheetTemplate create(Long tenantId, Long actorId, CreateTemplateRequest req) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepo.findById(req.getBranchId())
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser actor = loadUser(actorId);

        WorksheetTemplate t = WorksheetTemplate.builder()
                .tenant(tenant)
                .branch(branch)
                .templateName(req.getTemplateName())
                .mode(req.getMode() != null ? req.getMode() : "MANUAL")
                .templateJson(req.getTemplateJson())
                .status("DRAFT")
                .version(1)
                .createdBy(actor)
                .createdAt(LocalDateTime.now())
                .build();

        if (req.getSourceDocumentId() != null) {
            t.setSourceDocument(documentMasterRepo.findById(req.getSourceDocumentId())
                    .orElseThrow(() -> LimsException.notFound("Source document not found")));
        }

        return templateRepo.save(t);
    }

    @Transactional
    public WorksheetTemplate update(Long tenantId, Long actorId, Long templateId,
                                    UpdateTemplateRequest req) {
        WorksheetTemplate t = loadTemplate(tenantId, templateId);
        if (!"DRAFT".equals(t.getStatus()) && !"REJECTED".equals(t.getStatus())) {
            throw LimsException.badRequest("Only DRAFT or REJECTED templates can be edited");
        }
        if (req.getTemplateName() != null) t.setTemplateName(req.getTemplateName());
        if (req.getTemplateJson() != null)  t.setTemplateJson(req.getTemplateJson());
        if (req.getMode() != null)          t.setMode(req.getMode());
        t.setModifiedBy(loadUser(actorId));
        t.setModifiedAt(LocalDateTime.now());
        return templateRepo.save(t);
    }

    @Transactional(readOnly = true)
    public WorksheetTemplate get(Long tenantId, Long templateId) {
        return loadTemplate(tenantId, templateId);
    }

    @Transactional(readOnly = true)
    public List<WorksheetTemplateResponse> list(Long tenantId, Long branchId, String status) {
        List<WorksheetTemplate> templates = status != null
                ? templateRepo.findByTenantIdAndBranchIdAndStatusOrderByCreatedAtDesc(
                        tenantId, branchId, status)
                : templateRepo.findByTenantIdAndBranchIdOrderByCreatedAtDesc(
                        tenantId, branchId);
        return templates.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Transactional
    public WorksheetTemplate submitForReview(Long tenantId, Long actorId,
                                             SubmitTemplateForReviewRequest req) {
        WorksheetTemplate t = loadTemplate(tenantId, req.getTemplateId());
        if (!"DRAFT".equals(t.getStatus()) && !"REJECTED".equals(t.getStatus())) {
            throw LimsException.badRequest("Only DRAFT or REJECTED templates can be submitted for review");
        }
        if (t.getTemplateJson() == null || t.getTemplateJson().isBlank()) {
            throw LimsException.badRequest("Template JSON must not be empty before review");
        }
        t.setStatus("IN_REVIEW");
        t.setReviewNote(req.getNote());
        t.setModifiedBy(loadUser(actorId));
        t.setModifiedAt(LocalDateTime.now());
        return templateRepo.save(t);
    }

    @Transactional
    public WorksheetTemplate approve(Long tenantId, Long actorId, ApproveTemplateRequest req) {
        WorksheetTemplate t = loadTemplate(tenantId, req.getTemplateId());
        if (!"IN_REVIEW".equals(t.getStatus())) {
            throw LimsException.badRequest("Only IN_REVIEW templates can be approved");
        }
        AppUser actor = loadUser(actorId);
        t.setStatus("APPROVED");
        t.setApprovedBy(actor);
        t.setApprovedAt(LocalDateTime.now());
        t.setReviewNote(req.getNote());
        t.setModifiedBy(actor);
        t.setModifiedAt(LocalDateTime.now());
        return templateRepo.save(t);
    }

    @Transactional
    public WorksheetTemplate reject(Long tenantId, Long actorId, RejectTemplateRequest req) {
        WorksheetTemplate t = loadTemplate(tenantId, req.getTemplateId());
        if (!"IN_REVIEW".equals(t.getStatus())) {
            throw LimsException.badRequest("Only IN_REVIEW templates can be rejected");
        }
        AppUser actor = loadUser(actorId);
        t.setStatus("REJECTED");
        t.setReviewedBy(actor);
        t.setReviewedAt(LocalDateTime.now());
        t.setReviewNote(req.getReviewNote());
        t.setModifiedBy(actor);
        t.setModifiedAt(LocalDateTime.now());
        return templateRepo.save(t);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WorksheetTemplate loadTemplate(Long tenantId, Long templateId) {
        return templateRepo.findByTemplateIdAndTenantId(templateId, tenantId)
                .orElseThrow(() -> LimsException.notFound("WorksheetTemplate not found"));
    }

    private AppUser loadUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
    }

    public WorksheetTemplateResponse toResponse(WorksheetTemplate t) {
        WorksheetTemplateResponse r = new WorksheetTemplateResponse();
        r.setTemplateId(t.getTemplateId());
        r.setTemplateName(t.getTemplateName());
        r.setMode(t.getMode());
        r.setVersion(t.getVersion());
        r.setStatus(t.getStatus());
        r.setTemplateJson(t.getTemplateJson());
        r.setReviewNote(t.getReviewNote());
        r.setCreatedAt(t.getCreatedAt());
        r.setReviewedAt(t.getReviewedAt());
        r.setApprovedAt(t.getApprovedAt());
        if (t.getSourceDocument() != null)
            r.setSourceDocumentId(t.getSourceDocument().getId());
        if (t.getReviewedBy() != null)
            r.setReviewedById(t.getReviewedBy().getId());
        if (t.getApprovedBy() != null)
            r.setApprovedById(t.getApprovedBy().getId());
        if (t.getCreatedBy() != null)
            r.setCreatedById(t.getCreatedBy().getId());
        return r;
    }

    public WorksheetExecutionView toExecutionView(Long tenantId, Long templateId) {
        WorksheetTemplate t = loadTemplate(tenantId, templateId);
        WorksheetExecutionView view = new WorksheetExecutionView();
        view.setTemplateId(t.getTemplateId());
        view.setTemplateName(t.getTemplateName());
        view.setStatus(t.getStatus());
        view.setVersion(t.getVersion());

        List<WorksheetExecutionView.ExecutionSection> sections = new ArrayList<>();
        if (t.getTemplateJson() != null && !t.getTemplateJson().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(t.getTemplateJson());
                for (JsonNode sec : root.path("sections")) {
                    WorksheetExecutionView.ExecutionSection es = new WorksheetExecutionView.ExecutionSection();
                    es.setSectionName(sec.path("sectionName").asText("Section"));
                    List<WorksheetExecutionView.ExecutionGroup> groups = new ArrayList<>();
                    for (JsonNode grp : sec.path("slotGroups")) {
                        WorksheetExecutionView.ExecutionGroup eg = new WorksheetExecutionView.ExecutionGroup();
                        eg.setGroupName(grp.path("groupName").asText("Group"));
                        List<WorksheetExecutionView.ExecutionSlot> slots = new ArrayList<>();
                        for (JsonNode slot : grp.path("slots")) {
                            WorksheetExecutionView.ExecutionSlot es2 = new WorksheetExecutionView.ExecutionSlot();
                            es2.setSlotKey(slot.path("slotKey").asText());
                            es2.setLabel(slot.path("label").asText());
                            es2.setType(slot.path("type").asText("TEXT"));
                            es2.setUnit(slot.path("unit").isNull() ? null : slot.path("unit").asText(null));
                            es2.setRequired(slot.path("required").asBoolean(false));
                            List<String> options = new ArrayList<>();
                            for (JsonNode opt : slot.path("options")) options.add(opt.asText());
                            es2.setOptions(options.isEmpty() ? null : options);
                            es2.setCurrentValue(null);
                            slots.add(es2);
                        }
                        eg.setSlots(slots);
                        groups.add(eg);
                    }
                    es.setSlotGroups(groups);
                    sections.add(es);
                }
            } catch (Exception e) {
                log.warn("Could not parse templateJson for execution view: {}", e.getMessage());
            }
        }
        view.setSections(sections);
        return view;
    }
}

