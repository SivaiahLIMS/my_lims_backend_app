package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytics", description = "Trend analytics, utilization, and predictive intelligence")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ── OOS trend ────────────────────────────────────────────────────────────────

    @GetMapping("/products/{id}/oos-trend")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get OOS trend data for a product",
               description = "Requires: ANALYTICS_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getOosTrend(
            @PathVariable Long id,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getOosTrend(u.getTenantId(), branchId, from, to));
    }

    // ── Instrument utilization ────────────────────────────────────────────────────

    @GetMapping("/instruments/{id}/utilization")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get instrument utilization analytics")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getInstrumentUtilization(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getInstrumentUtilization(id, from, to));
    }

    // ── Predictive alerts ─────────────────────────────────────────────────────────

    @GetMapping("/predictive-alerts")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get predictive alerts for branch")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<PredictiveAlert>> getPredictiveAlerts(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false, defaultValue = "false") boolean openOnly,
            @AuthenticationPrincipal LimsUserDetails u) {
        List<PredictiveAlert> result = openOnly
                ? analyticsService.getOpenPredictiveAlerts(u.getTenantId(), branchId)
                : analyticsService.getPredictiveAlerts(u.getTenantId(), branchId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/predictive-alerts/{id}/acknowledge")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Acknowledge a predictive alert")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Acknowledged")})
    public ResponseEntity<PredictiveAlert> acknowledgeAlert(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.acknowledgePredictiveAlert(id, u.getUser().getId()));
    }

    // ── Task metrics ──────────────────────────────────────────────────────────────

    @GetMapping("/tasks/metrics")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get task metrics overview for branch")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getTaskMetrics(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getTaskMetrics(u.getTenantId(), branchId));
    }

    // ── Sample TAT ────────────────────────────────────────────────────────────────

    @GetMapping("/samples/tat")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get sample turnaround time (TAT) analytics")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getSampleTat(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getSampleTatAnalytics(u.getTenantId(), branchId, from, to));
    }

    // ── QA trends ────────────────────────────────────────────────────────────────

    @GetMapping("/qa/trends")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get QA trend analytics: OOS/CAPA/Deviation by status")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getQaTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getQaTrendAnalytics(u.getTenantId(), from, to));
    }

    // ── QC pass/fail rates ────────────────────────────────────────────────────────

    @GetMapping("/qc/pass-fail")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get QC pass/fail rates for branch")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getQcPassFail(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getQcPassFailRates(u.getTenantId(), branchId, from, to));
    }

    // ── Chemical consumption ──────────────────────────────────────────────────────

    @GetMapping("/chemicals/consumption")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get chemical consumption trend for branch")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getChemicalConsumption(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getChemicalConsumptionTrend(u.getTenantId(), branchId, from, to));
    }

    // ── User workload ─────────────────────────────────────────────────────────────

    @GetMapping("/users/workload")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get user workload analytics for branch")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<UserWorkload>> getUserWorkloads(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getUserWorkloads(u.getTenantId(), branchId));
    }

    // ── Reagent Stock Levels ──────────────────────────────────────────────────────

    @GetMapping("/inventory/stock-levels")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get current stock levels for all reagents — includes stockStatus: OK/LOW/CRITICAL/OUT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<Map<String, Object>>> getReagentStockLevels(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getReagentStockLevels(u.getTenantId(), branchId));
    }

    // ── Expiry Risk Dashboard ─────────────────────────────────────────────────────

    @GetMapping("/inventory/expiry-risk")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Expiry risk dashboard — lots bucketed into EXPIRED / CRITICAL (≤7d) / WARNING (≤30d)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getExpiryRiskDashboard(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getExpiryRiskDashboard(u.getTenantId()));
    }

    // ── Consumption Trends ────────────────────────────────────────────────────────

    @GetMapping("/inventory/consumption-trends")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Reagent consumption trends — daily time series, optionally filtered by reagent")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getConsumptionTrends(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) Long reagentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                analyticsService.getConsumptionTrends(u.getTenantId(), branchId, reagentId, from, to));
    }

    // ── Sample analytics ──────────────────────────────────────────────────────────

    @GetMapping("/samples/inflow")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Sample inflow volume by day")
    public ResponseEntity<Map<String, Object>> getSampleInflow(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getSampleInflow(u.getTenantId(), branchId, timeRange));
    }

    @GetMapping("/samples/pass-fail")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Sample pass vs fail rate")
    public ResponseEntity<Map<String, Object>> getSamplePassFail(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getSamplePassFail(u.getTenantId(), branchId != null ? branchId : 0L, timeRange));
    }

    @GetMapping("/samples/oos-frequency")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "OOS case frequency and trend")
    public ResponseEntity<Map<String, Object>> getOosFrequency(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getOosFrequency(u.getTenantId(), branchId, timeRange));
    }

    // ── User analytics ────────────────────────────────────────────────────────────

    @GetMapping("/users/analyst-workload")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Analyst workload breakdown")
    public ResponseEntity<Map<String, Object>> getAnalystWorkload(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getAnalystWorkload(u.getTenantId(), branchId != null ? branchId : 0L));
    }

    // ── Worksheet analytics ───────────────────────────────────────────────────────

    @GetMapping("/worksheets/status-trend")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Worksheet execution status trend")
    public ResponseEntity<Map<String, Object>> getWorksheetStatusTrend(
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getWorksheetStatusTrend(u.getTenantId(), timeRange));
    }

    @GetMapping("/worksheets/rejection-rate")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Worksheet rejection rate over time")
    public ResponseEntity<Map<String, Object>> getWorksheetRejectionRate(
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getWorksheetRejectionRate(u.getTenantId(), timeRange));
    }

    // ── Instrument analytics ──────────────────────────────────────────────────────

    @GetMapping("/instruments/uptime")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Instrument availability/uptime summary")
    public ResponseEntity<Map<String, Object>> getInstrumentUptime(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getInstrumentUptime(u.getTenantId(), branchId, timeRange));
    }

    @GetMapping("/instruments/calibration-status")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Instrument calibration status breakdown")
    public ResponseEntity<Map<String, Object>> getInstrumentCalibrationStatus(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getInstrumentCalibrationStatus(u.getTenantId(), branchId));
    }

    @GetMapping("/instruments/usage-by-product")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Instrument usage/reservations by instrument code")
    public ResponseEntity<Map<String, Object>> getInstrumentUsageByProduct(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getInstrumentUsageByProduct(u.getTenantId(), branchId, timeRange));
    }

    // ── Inventory analytics ───────────────────────────────────────────────────────

    @GetMapping("/inventory/consumption")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Reagent consumption over time")
    public ResponseEntity<Map<String, Object>> getInventoryConsumption(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getInventoryConsumptionByTimeRange(u.getTenantId(), branchId, timeRange));
    }

    @GetMapping("/inventory/issuance-destruction")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Issuance vs consumed inventory comparison")
    public ResponseEntity<Map<String, Object>> getIssuanceDestruction(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getInventoryIssuanceDestruction(u.getTenantId(), branchId, timeRange));
    }

    @GetMapping("/inventory/cost-per-product")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Estimated inventory cost per reagent/product")
    public ResponseEntity<Map<String, Object>> getInventoryCostPerProduct(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getInventoryCostPerProduct(u.getTenantId(), branchId));
    }

    // ── QA analytics ─────────────────────────────────────────────────────────────

    @GetMapping("/qa/deviation-trend")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Deviation count trend over time")
    public ResponseEntity<Map<String, Object>> getDeviationTrend(
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getDeviationTrend(u.getTenantId(), timeRange));
    }

    @GetMapping("/qa/deviation-severity")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Deviation breakdown by severity and status")
    public ResponseEntity<Map<String, Object>> getDeviationSeverity(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getDeviationSeverity(u.getTenantId()));
    }

    @GetMapping("/qa/capa-closure")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "CAPA closure rate")
    public ResponseEntity<Map<String, Object>> getCapaClosure(
            @RequestParam(required = false) String timeRange,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getCapaClosureRate(u.getTenantId(), timeRange));
    }

    @GetMapping("/qa/root-cause")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Root cause / deviation type analysis")
    public ResponseEntity<Map<String, Object>> getRootCause(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getRootCauseAnalysis(u.getTenantId()));
    }

    // ── AI analytics ─────────────────────────────────────────────────────────────

    @GetMapping("/ai/oos-risk-trend")
    @PreAuthorize("hasAuthority('AI_OOS_RISK_VIEW')")
    @Operation(summary = "AI-predicted OOS risk trend from inventory forecasts")
    public ResponseEntity<Map<String, Object>> getAiOosRiskTrend(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getAiOosRiskTrend(u.getTenantId(), branchId));
    }

    @GetMapping("/ai/consumption-forecast")
    @PreAuthorize("hasAuthority('AI_INVENTORY_FORECAST_VIEW')")
    @Operation(summary = "AI consumption forecast by item")
    public ResponseEntity<Map<String, Object>> getAiConsumptionForecast(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getAiConsumptionForecast(u.getTenantId(), branchId));
    }

    @GetMapping("/ai/instrument-failure")
    @PreAuthorize("hasAuthority('AI_INSTRUMENT_TREND_VIEW')")
    @Operation(summary = "AI instrument failure prediction alerts")
    public ResponseEntity<Map<String, Object>> getAiInstrumentFailure(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getAiInstrumentFailure(u.getTenantId(), branchId));
    }
}
