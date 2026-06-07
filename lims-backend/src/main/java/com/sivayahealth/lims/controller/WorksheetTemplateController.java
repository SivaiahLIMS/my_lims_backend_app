package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.worksheet.*;
import com.sivayahealth.lims.entity.WorksheetMaster;
import com.sivayahealth.lims.entity.WorksheetSlotGroupValue;
import com.sivayahealth.lims.entity.WorksheetTimerLog;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.TimerService;
import com.sivayahealth.lims.service.WorksheetAiService;
import com.sivayahealth.lims.service.WorksheetLifecycleService;
import com.sivayahealth.lims.service.WorksheetTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/worksheet-templates")
@RequiredArgsConstructor
@Tag(name = "Worksheet Templates",
     description = "Template creation (DOCX upload / manual / LLM auto-generate), " +
                   "lifecycle review, analyst assignment, slot-group execution values, and timers.")
@SecurityRequirement(name = "bearerAuth")
public class WorksheetTemplateController {

    private final WorksheetTemplateService  templateService;
    private final WorksheetLifecycleService lifecycleService;
    private final TimerService              timerService;
    private final WorksheetAiService        worksheetAiService;

    // ── Template CRUD ─────────────────────────────────────────────────────────

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('WORKSHEET_CREATE')")
    @Operation(summary = "Auto-generate a DRAFT worksheet template via Gemini LLM")
    public ResponseEntity<WorksheetTemplateResponse> generate(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody GenerateTemplateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(worksheetAiService.generateTemplate(
                        actor.getTenantId(), actor.getUser().getId(), req));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WORKSHEET_CREATE')")
    @Operation(summary = "Create a new worksheet template (any mode)")
    public ResponseEntity<WorksheetTemplateResponse> create(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody CreateTemplateRequest req) {
        var template = templateService.create(actor.getTenantId(), actor.getUser().getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.toResponse(template));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List templates for a branch")
    public ResponseEntity<List<WorksheetTemplateResponse>> list(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(templateService.list(actor.getTenantId(), branchId, status));
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Get a single template")
    public ResponseEntity<WorksheetTemplateResponse> get(
            @AuthenticationPrincipal LimsUserDetails actor,
            @PathVariable Long templateId) {
        return ResponseEntity.ok(
                templateService.toResponse(templateService.get(actor.getTenantId(), templateId)));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('WORKSHEET_EDIT')")
    @Operation(summary = "Update a DRAFT or REJECTED template")
    public ResponseEntity<WorksheetTemplateResponse> update(
            @AuthenticationPrincipal LimsUserDetails actor,
            @PathVariable Long templateId,
            @RequestBody UpdateTemplateRequest req) {
        var t = templateService.update(actor.getTenantId(), actor.getUser().getId(), templateId, req);
        return ResponseEntity.ok(templateService.toResponse(t));
    }

    // ── Template lifecycle ────────────────────────────────────────────────────

    @PostMapping("/submit-for-review")
    @PreAuthorize("hasAuthority('WORKSHEET_SUBMIT')")
    @Operation(summary = "Submit a DRAFT template for review")
    public ResponseEntity<WorksheetTemplateResponse> submitForReview(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody SubmitTemplateForReviewRequest req) {
        var t = templateService.submitForReview(actor.getTenantId(), actor.getUser().getId(), req);
        return ResponseEntity.ok(templateService.toResponse(t));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('WORKSHEET_APPROVE')")
    @Operation(summary = "Approve an IN_REVIEW template")
    public ResponseEntity<WorksheetTemplateResponse> approve(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody ApproveTemplateRequest req) {
        var t = templateService.approve(actor.getTenantId(), actor.getUser().getId(), req);
        return ResponseEntity.ok(templateService.toResponse(t));
    }

    @PostMapping("/reject")
    @PreAuthorize("hasAuthority('WORKSHEET_APPROVE')")
    @Operation(summary = "Reject an IN_REVIEW template")
    public ResponseEntity<WorksheetTemplateResponse> reject(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody RejectTemplateRequest req) {
        var t = templateService.reject(actor.getTenantId(), actor.getUser().getId(), req);
        return ResponseEntity.ok(templateService.toResponse(t));
    }

    // ── Analyst assignment ────────────────────────────────────────────────────

    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('WORKSHEET_ASSIGN')")
    @Operation(summary = "Assign analysts to a worksheet (whole or per-test-case)")
    public ResponseEntity<Void> assign(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody AssignTestCasesRequest req) {
        lifecycleService.assignTestCases(actor.getTenantId(), actor.getUser().getId(), req);
        return ResponseEntity.ok().build();
    }

    // ── Slot-group value capture ───────────────────────────────────────────────

    @PostMapping("/slot-values")
    @PreAuthorize("hasAuthority('WORKSHEET_EXECUTE')")
    @Operation(summary = "Save (upsert) a slot-group value during worksheet execution")
    public ResponseEntity<WorksheetSlotGroupValue> saveSlotValue(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody SaveSlotGroupValueRequest req) {
        return ResponseEntity.ok(
                lifecycleService.saveSlotGroupValue(actor.getTenantId(), actor.getUser().getId(), req));
    }

    // ── Worksheet review ──────────────────────────────────────────────────────

    @PostMapping("/worksheets/submit-for-review")
    @PreAuthorize("hasAuthority('WORKSHEET_SUBMIT')")
    @Operation(summary = "Submit executed worksheet for manager review")
    public ResponseEntity<WorksheetMaster> submitWorksheetForReview(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody SubmitWorksheetForReviewRequest req) {
        return ResponseEntity.ok(
                lifecycleService.submitForReview(actor.getTenantId(), actor.getUser().getId(), req));
    }

    @PostMapping("/worksheets/review")
    @PreAuthorize("hasAuthority('WORKSHEET_APPROVE')")
    @Operation(summary = "Manager approves or rejects an executed worksheet")
    public ResponseEntity<WorksheetMaster> reviewWorksheet(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody ReviewWorksheetRequest req) {
        return ResponseEntity.ok(
                lifecycleService.reviewWorksheet(actor.getTenantId(), actor.getUser().getId(), req));
    }

    @PostMapping("/worksheets/{worksheetId}/retest")
    @PreAuthorize("hasAuthority('WORKSHEET_CREATE')")
    @Operation(summary = "Initiate a retest from an existing worksheet")
    public ResponseEntity<WorksheetMaster> retest(
            @AuthenticationPrincipal LimsUserDetails actor,
            @PathVariable Long worksheetId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                lifecycleService.initiateRetest(actor.getTenantId(), actor.getUser().getId(), worksheetId));
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    @PostMapping("/timers/start")
    @PreAuthorize("hasAuthority('WORKSHEET_EXECUTE')")
    @Operation(summary = "Start a timer for a slot group")
    public ResponseEntity<WorksheetTimerLog> startTimer(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody TimerStartRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                timerService.start(actor.getTenantId(), actor.getUser().getId(), req));
    }

    @PostMapping("/timers/action")
    @PreAuthorize("hasAuthority('WORKSHEET_EXECUTE')")
    @Operation(summary = "Stop, pause, or resume a running timer")
    public ResponseEntity<WorksheetTimerLog> timerAction(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestBody TimerActionRequest req) {
        return ResponseEntity.ok(timerService.act(actor.getTenantId(), actor.getUser().getId(), req));
    }

    @GetMapping("/timers")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Get current timer state by worksheetId + timerId")
    public ResponseEntity<WorksheetTimerLog> getTimerState(
            @AuthenticationPrincipal LimsUserDetails actor,
            @RequestParam Long worksheetId,
            @RequestParam String timerId) {
        return ResponseEntity.ok(
                timerService.getState(actor.getTenantId(), worksheetId, timerId));
    }
}
