package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.worksheet.ExecutionBoardView;
import com.sivayahealth.lims.dto.worksheet.GenerateWorksheetsRequest;
import com.sivayahealth.lims.entity.WorksheetMaster;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ExecutionBoardService;
import com.sivayahealth.lims.service.SampleWorksheetGenerationService;
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
@RequestMapping("/samples/{sampleId}/worksheets")
@RequiredArgsConstructor
@Tag(name = "Sample Worksheet Generation",
     description = "Generate worksheets from sample test assignments and view execution board")
@SecurityRequirement(name = "bearerAuth")
public class SampleWorksheetController {

    private final SampleWorksheetGenerationService generationService;
    private final ExecutionBoardService             executionBoardService;

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('WORKSHEET_CREATE')")
    @Operation(summary = "Bulk-generate worksheets from test assignments",
               description = "Creates one DRAFT worksheet per assignment in the request. " +
                             "Links each TestAssignment.worksheet_id to the created worksheet.")
    public ResponseEntity<List<WorksheetMaster>> generate(
            @PathVariable Long sampleId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody GenerateWorksheetsRequest req,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                generationService.generateWorksheets(
                        u.getTenantId(), branchId, sampleId, req, u.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List all worksheets linked to a sample")
    public ResponseEntity<List<WorksheetMaster>> listForSample(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(generationService.getWorksheetsForSample(sampleId));
    }

    @GetMapping("/execution-board")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Execution board: aggregated sample → tests → worksheets → results → events")
    public ResponseEntity<ExecutionBoardView> executionBoard(
            @PathVariable Long sampleId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                executionBoardService.getBoard(u.getTenantId(), branchId, sampleId));
    }
}
