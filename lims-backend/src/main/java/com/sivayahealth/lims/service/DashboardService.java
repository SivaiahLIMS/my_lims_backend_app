package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.SampleStatus;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CriticalAlertRepository alertRepository;
    private final InstrumentCalibrationScheduleRepository calibrationScheduleRepository;
    private final ChemicalRegistrationRepository chemicalRegistrationRepository;
    private final SampleRepository sampleRepository;
    private final OosCaseRepository oosCaseRepository;
    private final DeviationRepository deviationRepository;
    private final CapaRepository capaRepository;
    private final StabilityStudyRepository stabilityStudyRepository;
    private final ReagentPreparationRepository reagentPreparationRepository;
    private final WorksheetExecutionRepository worksheetExecutionRepository;
    private final TaskMasterRepository taskMasterRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(Long tenantId, Long branchId) {
        Map<String, Object> data = new LinkedHashMap<>();

        // Spec: totalSamples
        data.put("totalSamples", sampleRepository.findByTenantIdAndBranchId(tenantId, branchId).size());

        // Spec: pendingSamples
        data.put("pendingSamples", sampleRepository
                .findByTenantIdAndBranchIdAndStatus(tenantId, branchId, SampleStatus.PENDING).size());

        // Spec: activeWorksheets
        List<com.sivayahealth.lims.entity.WorksheetExecution> allWorksheets =
                worksheetExecutionRepository.findAllByTenant(tenantId);
        data.put("activeWorksheets", allWorksheets.stream()
                .filter(w -> "IN_PROGRESS".equals(w.getStatus()) || "SUBMITTED".equals(w.getStatus()))
                .count());

        // Spec: pendingApprovals
        data.put("pendingApprovals", worksheetExecutionRepository.findPendingApprovalByTenant(tenantId).size());

        // Spec: overdueCalibrations
        data.put("overdueCalibrations", calibrationScheduleRepository.findByStatus("OVERDUE").size());

        // Spec: openDeviations
        data.put("openDeviations", deviationRepository.findByTenantIdAndStatus(tenantId, "OPEN").size());

        // Spec: openCapas
        data.put("openCapas", capaRepository.findByTenantIdAndStatus(tenantId, "OPEN").size());

        // Spec: lowStockChemicals
        data.put("lowStockChemicals", chemicalRegistrationRepository
                .findExpiringChemicals(tenantId, LocalDate.now().plusDays(30)).size());

        // Spec: todayTasks
        data.put("todayTasks", taskMasterRepository.findByTenantIdAndBranchId(tenantId, branchId).stream()
                .filter(t -> "PENDING".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus()))
                .count());

        // Spec: recentAlerts
        data.put("recentAlerts", alertRepository
                .findByTenantIdAndBranchIdAndResolvedAtIsNull(tenantId, branchId).size());

        // Additional legacy fields kept for backward compatibility
        data.put("criticalAlerts", data.get("recentAlerts"));
        data.put("openOos", oosCaseRepository.findByTenantIdAndStatus(tenantId, "OPEN").size());
        data.put("calibrationDue", calibrationScheduleRepository.findByStatus("DUE").size());

        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWidgets(Long tenantId, Long branchId) {
        Map<String, Object> summary = getDashboardData(tenantId, branchId);
        List<Map<String, Object>> widgets = new ArrayList<>();

        widgets.add(buildWidget("total_samples", "Total Samples", summary.get("totalSamples"), null, null, "info"));
        widgets.add(buildWidget("pending_samples", "Pending Samples", summary.get("pendingSamples"), null, null, "warning"));
        widgets.add(buildWidget("active_worksheets", "Active Worksheets", summary.get("activeWorksheets"), null, null, "info"));
        widgets.add(buildWidget("pending_approvals", "Pending Approvals", summary.get("pendingApprovals"), null, null, "warning"));
        widgets.add(buildWidget("overdue_calibrations", "Overdue Calibrations", summary.get("overdueCalibrations"), null, null, "error"));
        widgets.add(buildWidget("open_deviations", "Open Deviations", summary.get("openDeviations"), null, null, "warning"));
        widgets.add(buildWidget("open_capas", "Open CAPAs", summary.get("openCapas"), null, null, "warning"));
        widgets.add(buildWidget("low_stock", "Low Stock Chemicals", summary.get("lowStockChemicals"), null, null, "error"));
        widgets.add(buildWidget("today_tasks", "Today's Tasks", summary.get("todayTasks"), null, null, "info"));
        widgets.add(buildWidget("recent_alerts", "Recent Alerts", summary.get("recentAlerts"), null, null, "error"));
        widgets.add(buildWidget("open_oos", "Open OOS Cases", summary.get("openOos"), null, null, "warning"));

        return Map.of("widgets", widgets, "summary", summary);
    }

    private Map<String, Object> buildWidget(String widgetId, String title, Object value, String unit, String trend, String status) {
        Map<String, Object> widget = new LinkedHashMap<>();
        widget.put("widgetId", widgetId);
        widget.put("title", title);
        widget.put("value", value);
        if (unit != null) widget.put("unit", unit);
        if (trend != null) widget.put("trend", trend);
        widget.put("status", status);
        return widget;
    }
}
