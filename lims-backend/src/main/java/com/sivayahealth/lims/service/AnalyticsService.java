package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final DocumentTestResultRepository documentTestResultRepository;
    private final InstrumentReadingRepository instrumentReadingRepository;
    private final InstrumentReservationRepository instrumentReservationRepository;
    private final PredictiveAlertRepository predictiveAlertRepository;
    private final InstrumentMetricSnapshotRepository instrumentMetricSnapshotRepository;
    private final AppUserRepository appUserRepository;
    private final TaskMasterRepository taskMasterRepository;
    private final SampleRepository sampleRepository;
    private final SampleTestRepository sampleTestRepository;
    private final OosCaseRepository oosCaseRepository;
    private final CapaRepository capaRepository;
    private final DeviationRepository deviationRepository;
    private final ChemicalIssuanceRepository chemicalIssuanceRepository;
    private final UserWorkloadRepository userWorkloadRepository;
    private final InventoryReagentRepository inventoryReagentRepository;
    private final InventoryReagentLotRepository inventoryReagentLotRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InstrumentCalibrationRepository instrumentCalibrationRepository;
    private final InstrumentMasterRepository instrumentMasterRepository;
    private final WorksheetExecutionRepository worksheetExecutionRepository;
    private final AiInventoryForecastRepository aiInventoryForecastRepository;

    // ── Existing methods ─────────────────────────────────────────────────────────

    public Map<String, Object> getOosTrend(Long tenantId, Long branchId, LocalDate from, LocalDate to) {
        List<DocumentTestResult> oosResults = documentTestResultRepository.findByTenantIdAndBranchIdAndOosTrue(tenantId, branchId);

        Map<LocalDate, Long> byDate = oosResults.stream()
                .filter(r -> {
                    LocalDate d = r.getCreatedAt().toLocalDate();
                    return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
                })
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> points = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("date", e.getKey().toString());
                    point.put("oosCount", e.getValue());
                    return point;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        result.put("totalOos", oosResults.size());
        return result;
    }

    public Map<String, Object> getInstrumentUtilization(Long instrumentId, LocalDate from, LocalDate to) {
        List<InstrumentReservation> reservations = instrumentReservationRepository.findByInstrument_Id(instrumentId);
        List<InstrumentReading> readings = instrumentReadingRepository.findByInstrument_Id(instrumentId);

        Map<String, Object> result = new HashMap<>();
        result.put("instrumentId", instrumentId);
        result.put("totalReservations", reservations.size());
        result.put("approvedReservations", reservations.stream().filter(r -> "APPROVED".equals(r.getStatus())).count());
        result.put("totalReadings", readings.size());

        List<InstrumentMetricSnapshot> snapshots = instrumentMetricSnapshotRepository
                .findByInstrument_IdOrderByMetricDateAsc(instrumentId);
        result.put("metricSnapshots", snapshots);

        return result;
    }

    public List<PredictiveAlert> getPredictiveAlerts(Long tenantId, Long branchId) {
        return predictiveAlertRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    public List<PredictiveAlert> getOpenPredictiveAlerts(Long tenantId, Long branchId) {
        return predictiveAlertRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, "OPEN");
    }

    @Transactional
    public PredictiveAlert acknowledgePredictiveAlert(Long id, Long userId) {
        PredictiveAlert alert = predictiveAlertRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Predictive alert not found: " + id));
        AppUser user = appUserRepository.findById(userId).orElse(null);
        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(user);
        alert.setAcknowledgedAt(LocalDateTime.now());
        return predictiveAlertRepository.save(alert);
    }

    public Map<String, Object> getTaskMetrics(Long tenantId, Long branchId) {
        List<TaskMaster> tasks = taskMasterRepository.findByTenantIdAndBranchId(tenantId, branchId);
        Map<String, Long> byStatus = tasks.stream()
                .collect(Collectors.groupingBy(TaskMaster::getStatus, Collectors.counting()));
        Map<String, Long> byType = tasks.stream()
                .collect(Collectors.groupingBy(TaskMaster::getType, Collectors.counting()));

        Map<String, Object> result = new HashMap<>();
        result.put("byStatus", byStatus);
        result.put("byType", byType);
        result.put("total", tasks.size());
        return result;
    }

    @Transactional
    public InstrumentMetricSnapshot saveMetricSnapshot(InstrumentMetricSnapshot snapshot) {
        return instrumentMetricSnapshotRepository.save(snapshot);
    }

    @Transactional
    public PredictiveAlert createPredictiveAlert(PredictiveAlert alert) {
        return predictiveAlertRepository.save(alert);
    }

    // ── Sample Turnaround Time (TAT) analytics ────────────────────────────────────

    public Map<String, Object> getSampleTatAnalytics(Long tenantId, Long branchId,
                                                       LocalDate from, LocalDate to) {
        List<Sample> samples = sampleRepository.findByTenantIdAndBranchId(tenantId, branchId);

        List<Sample> completed = samples.stream()
                .filter(s -> s.getStatus() != null &&
                        (s.getStatus() == com.sivayahealth.lims.entity.SampleStatus.COMPLETED
                         || s.getStatus() == com.sivayahealth.lims.entity.SampleStatus.APPROVED))
                .filter(s -> s.getCreatedAt() != null)
                .filter(s -> {
                    LocalDate d = s.getCreatedAt().toLocalDate();
                    return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
                })
                .collect(Collectors.toList());

        OptionalDouble avgTat = completed.stream()
                .filter(s -> s.getDueDate() != null)
                .mapToLong(s -> java.time.Duration.between(s.getCreatedAt(), s.getDueDate()).toHours())
                .average();

        Map<String, Long> byStatus = samples.stream()
                .collect(Collectors.groupingBy(s -> s.getStatus().name(), Collectors.counting()));

        Map<String, Object> result = new HashMap<>();
        result.put("totalSamples", samples.size());
        result.put("completedSamples", completed.size());
        result.put("averageTatHours", avgTat.isPresent() ? Math.round(avgTat.getAsDouble()) : 0);
        result.put("byStatus", byStatus);
        return result;
    }

    // ── CAPA / Deviation trend by month ──────────────────────────────────────────

    public Map<String, Object> getQaTrendAnalytics(Long tenantId, LocalDate from, LocalDate to) {
        List<OosCase> oosCases = oosCaseRepository.findByTenantId(tenantId);
        List<Capa> capas = capaRepository.findByTenantId(tenantId);
        List<Deviation> deviations = deviationRepository.findByTenantId(tenantId);

        Map<String, Long> oosByStatus = oosCases.stream()
                .collect(Collectors.groupingBy(OosCase::getStatus, Collectors.counting()));
        Map<String, Long> capaByStatus = capas.stream()
                .collect(Collectors.groupingBy(Capa::getStatus, Collectors.counting()));
        Map<String, Long> deviationByStatus = deviations.stream()
                .collect(Collectors.groupingBy(Deviation::getStatus, Collectors.counting()));

        Map<String, Object> result = new HashMap<>();
        result.put("oos", Map.of("total", oosCases.size(), "byStatus", oosByStatus));
        result.put("capa", Map.of("total", capas.size(), "byStatus", capaByStatus));
        result.put("deviation", Map.of("total", deviations.size(), "byStatus", deviationByStatus));
        return result;
    }

    // ── QC pass/fail rates ────────────────────────────────────────────────────────

    public Map<String, Object> getQcPassFailRates(Long tenantId, Long branchId,
                                                    LocalDate from, LocalDate to) {
        List<DocumentTestResult> allResults = documentTestResultRepository
                .findByTenantIdAndBranchId(tenantId, branchId);

        List<DocumentTestResult> filtered = allResults.stream()
                .filter(r -> {
                    if (r.getCreatedAt() == null) return true;
                    LocalDate d = r.getCreatedAt().toLocalDate();
                    return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
                })
                .collect(Collectors.toList());

        long oosCount = filtered.stream().filter(DocumentTestResult::isOos).count();
        long passCount = filtered.size() - oosCount;
        long total = filtered.size();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("pass", passCount);
        result.put("oos", oosCount);
        result.put("passRate", total > 0 ? Math.round((double) passCount / total * 100.0) : 0);
        result.put("oosRate", total > 0 ? Math.round((double) oosCount / total * 100.0) : 0);
        return result;
    }

    // ── Chemical consumption trend ────────────────────────────────────────────────

    public Map<String, Object> getChemicalConsumptionTrend(Long tenantId, Long branchId,
                                                             LocalDate from, LocalDate to) {
        List<ChemicalIssuance> issuances = chemicalIssuanceRepository
                .findByTenantIdAndBranchId(tenantId, branchId);

        List<ChemicalIssuance> filtered = issuances.stream()
                .filter(i -> {
                    if (i.getIssuedDate() == null) return true;
                    LocalDate d = i.getIssuedDate().toLocalDate();
                    return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
                })
                .collect(Collectors.toList());

        Map<String, Long> byPurpose = filtered.stream()
                .filter(i -> i.getPurpose() != null)
                .collect(Collectors.groupingBy(ChemicalIssuance::getPurpose, Collectors.counting()));

        Map<String, Object> result = new HashMap<>();
        result.put("totalIssuances", filtered.size());
        result.put("byPurpose", byPurpose);
        return result;
    }

    // ── User workload analytics ───────────────────────────────────────────────────

    public List<UserWorkload> getUserWorkloads(Long tenantId, Long branchId) {
        return userWorkloadRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    // ── Reagent Stock Levels ──────────────────────────────────────────────────────

    /**
     * Returns current stock summary for every ACTIVE reagent in a branch.
     * Each entry contains: reagentId, name, reagentCode, totalAvailable, uom,
     * minStockLevel, reorderLevel, stockStatus (OK / LOW / CRITICAL / OUT).
     */
    public List<Map<String, Object>> getReagentStockLevels(Long tenantId, Long branchId) {
        List<InventoryReagent> reagents = branchId != null
                ? inventoryReagentRepository.findByTenantIdAndBranchId(tenantId, branchId)
                : inventoryReagentRepository.findByTenantId(tenantId);

        return reagents.stream().map(r -> {
            BigDecimal total = inventoryReagentLotRepository.findAvailableByReagentFEFO(r.getId())
                    .stream()
                    .map(InventoryReagentLot::getCurrentQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String stockStatus;
            if (total.compareTo(BigDecimal.ZERO) == 0) {
                stockStatus = "OUT";
            } else if (r.getMinStockLevel() != null && total.compareTo(r.getMinStockLevel()) <= 0) {
                stockStatus = "CRITICAL";
            } else if (r.getReorderLevel() != null && total.compareTo(r.getReorderLevel()) <= 0) {
                stockStatus = "LOW";
            } else {
                stockStatus = "OK";
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("reagentId", r.getId());
            entry.put("reagentCode", r.getReagentCode());
            entry.put("name", r.getName());
            entry.put("category", r.getCategory());
            entry.put("totalAvailable", total);
            entry.put("uom", r.getDefaultUom());
            entry.put("minStockLevel", r.getMinStockLevel());
            entry.put("reorderLevel", r.getReorderLevel());
            entry.put("stockStatus", stockStatus);
            return entry;
        }).collect(Collectors.toList());
    }

    // ── Expiry Risk Dashboard ─────────────────────────────────────────────────────

    /**
     * Classifies all lots into: EXPIRED, expiring in ≤7 days (CRITICAL),
     * 8–30 days (WARNING), and >30 days (OK).
     */
    public Map<String, Object> getExpiryRiskDashboard(Long tenantId) {
        LocalDate today = LocalDate.now();
        List<InventoryReagentLot> allLots = inventoryReagentLotRepository
                .findExpiringLots(tenantId, today.plusDays(90));

        List<Map<String, Object>> expired = new ArrayList<>();
        List<Map<String, Object>> critical = new ArrayList<>();
        List<Map<String, Object>> warning = new ArrayList<>();

        for (InventoryReagentLot lot : allLots) {
            if (lot.getExpiryDate() == null) continue;
            long daysLeft = today.until(lot.getExpiryDate()).getDays();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("lotId", lot.getId());
            item.put("lotNumber", lot.getLotNumber());
            item.put("reagentName", lot.getReagent().getName());
            item.put("reagentCode", lot.getReagent().getReagentCode());
            item.put("expiryDate", lot.getExpiryDate());
            item.put("daysUntilExpiry", daysLeft);
            item.put("currentQty", lot.getCurrentQty());
            item.put("uom", lot.getUom());
            item.put("status", lot.getStatus());

            if (daysLeft < 0 || "EXPIRED".equals(lot.getStatus())) expired.add(item);
            else if (daysLeft <= 7) critical.add(item);
            else if (daysLeft <= 30) warning.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expired", expired);
        result.put("critical", critical);
        result.put("warning", warning);
        result.put("summary", Map.of(
                "expiredCount", expired.size(),
                "criticalCount", critical.size(),
                "warningCount", warning.size()
        ));
        return result;
    }

    // ── Consumption Trends ────────────────────────────────────────────────────────

    /**
     * Groups CONSUME movements by date for a given reagent (or all in a branch).
     * Returns a time series with daily totals usable for charting.
     */
    public Map<String, Object> getConsumptionTrends(Long tenantId, Long branchId,
                                                     Long reagentId, LocalDate from, LocalDate to) {
        List<InventoryReagent> reagents;
        if (reagentId != null) {
            InventoryReagent r = inventoryReagentRepository.findById(reagentId)
                    .orElseThrow(() -> LimsException.notFound("Reagent not found"));
            reagents = List.of(r);
        } else {
            reagents = branchId != null
                    ? inventoryReagentRepository.findByTenantIdAndBranchId(tenantId, branchId)
                    : inventoryReagentRepository.findByTenantId(tenantId);
        }

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        Map<String, BigDecimal> dailyConsumption = new TreeMap<>();
        Map<String, BigDecimal> byReagent = new LinkedHashMap<>();
        BigDecimal totalConsumed = BigDecimal.ZERO;

        for (InventoryReagent reagent : reagents) {
            List<InventoryReagentLot> lots = inventoryReagentLotRepository.findByReagentId(reagent.getId());
            BigDecimal reagentTotal = BigDecimal.ZERO;

            for (InventoryReagentLot lot : lots) {
                List<InventoryMovement> movements = inventoryMovementRepository
                        .findByLotIdOrderByPerformedAtDesc(lot.getId());

                for (InventoryMovement m : movements) {
                    if (!"CONSUME".equals(m.getMovementType())) continue;
                    LocalDate day = m.getPerformedAt().toLocalDate();
                    if (day.isBefore(effectiveFrom) || day.isAfter(effectiveTo)) continue;

                    String key = day.toString();
                    dailyConsumption.merge(key, m.getQuantity(), BigDecimal::add);
                    reagentTotal = reagentTotal.add(m.getQuantity());
                    totalConsumed = totalConsumed.add(m.getQuantity());
                }
            }
            if (reagentTotal.compareTo(BigDecimal.ZERO) > 0) {
                byReagent.put(reagent.getName() + " (" + reagent.getReagentCode() + ")", reagentTotal);
            }
        }

        List<Map<String, Object>> series = dailyConsumption.entrySet().stream()
                .map(e -> {
                    Map<String, Object> pt = new LinkedHashMap<>();
                    pt.put("date", e.getKey());
                    pt.put("consumed", e.getValue());
                    return pt;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", effectiveFrom);
        result.put("to", effectiveTo);
        result.put("totalConsumed", totalConsumed);
        result.put("byReagent", byReagent);
        result.put("series", series);
        return result;
    }

    // ── Sample inflow (volume by day) ─────────────────────────────────────────────

    public Map<String, Object> getSampleInflow(Long tenantId, Long branchId, String timeRange) {
        List<Sample> samples = branchId != null
                ? sampleRepository.findByTenantIdAndBranchId(tenantId, branchId)
                : sampleRepository.findAll().stream()
                    .filter(s -> s.getTenant() != null && s.getTenant().getId().equals(tenantId))
                    .toList();

        LocalDate from = resolveFrom(timeRange);
        Map<String, Long> byDate = samples.stream()
                .filter(s -> s.getCreatedAt() != null && !s.getCreatedAt().toLocalDate().isBefore(from))
                .collect(Collectors.groupingBy(
                        s -> s.getCreatedAt().toLocalDate().toString(), Collectors.counting()));

        List<Map<String, Object>> series = new TreeMap<>(byDate).entrySet().stream()
                .map(e -> Map.<String, Object>of("date", e.getKey(), "count", e.getValue()))
                .toList();

        return Map.of("series", series, "total", samples.size(), "from", from.toString());
    }

    // ── Sample pass/fail ──────────────────────────────────────────────────────────

    public Map<String, Object> getSamplePassFail(Long tenantId, Long branchId, String timeRange) {
        List<DocumentTestResult> results = documentTestResultRepository
                .findByTenantIdAndBranchId(tenantId, branchId);

        LocalDate from = resolveFrom(timeRange);
        List<DocumentTestResult> filtered = results.stream()
                .filter(r -> r.getCreatedAt() == null || !r.getCreatedAt().toLocalDate().isBefore(from))
                .toList();

        long pass = filtered.stream().filter(r -> !r.isOos()).count();
        long fail = filtered.stream().filter(DocumentTestResult::isOos).count();
        long total = filtered.size();

        return Map.of(
                "pass", pass, "fail", fail, "total", total,
                "passRate", total > 0 ? Math.round(100.0 * pass / total) : 0,
                "failRate", total > 0 ? Math.round(100.0 * fail / total) : 0
        );
    }

    // ── OOS frequency by product ──────────────────────────────────────────────────

    public Map<String, Object> getOosFrequency(Long tenantId, Long branchId, String timeRange) {
        List<OosCase> cases = oosCaseRepository.findByTenantId(tenantId);
        LocalDate from = resolveFrom(timeRange);
        List<OosCase> filtered = cases.stream()
                .filter(c -> c.getRaisedAt() == null || !c.getRaisedAt().toLocalDate().isBefore(from))
                .toList();

        Map<String, Long> byStatus = filtered.stream()
                .collect(Collectors.groupingBy(OosCase::getStatus, Collectors.counting()));
        Map<String, Long> byMonth = filtered.stream()
                .filter(c -> c.getRaisedAt() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getRaisedAt().toLocalDate().withDayOfMonth(1).toString(),
                        Collectors.counting()));

        return Map.of("total", filtered.size(), "byStatus", byStatus,
                "byMonth", new TreeMap<>(byMonth));
    }

    // ── Analyst workload ──────────────────────────────────────────────────────────

    public Map<String, Object> getAnalystWorkload(Long tenantId, Long branchId) {
        List<UserWorkload> workloads = userWorkloadRepository.findByTenantIdAndBranchId(tenantId, branchId);
        List<Map<String, Object>> data = workloads.stream().map(w -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("userId", w.getUser() != null ? w.getUser().getId() : null);
            entry.put("username", w.getUser() != null ? w.getUser().getUsername() : null);
            entry.put("openTasks", w.getOpenTasks());
            entry.put("updatedAt", w.getUpdatedAt() != null ? w.getUpdatedAt().toString() : null);
            return entry;
        }).toList();
        return Map.of("analysts", data, "total", workloads.size());
    }

    // ── Worksheet status trend ────────────────────────────────────────────────────

    public Map<String, Object> getWorksheetStatusTrend(Long tenantId, String timeRange) {
        List<WorksheetExecution> executions = worksheetExecutionRepository.findAllByTenant(tenantId);
        LocalDate from = resolveFrom(timeRange);
        List<WorksheetExecution> filtered = executions.stream()
                .filter(w -> w.getExecutedAt() == null || !w.getExecutedAt().toLocalDate().isBefore(from))
                .toList();

        Map<String, Long> byStatus = filtered.stream()
                .filter(w -> w.getStatus() != null)
                .collect(Collectors.groupingBy(WorksheetExecution::getStatus, Collectors.counting()));

        return Map.of("total", filtered.size(), "byStatus", byStatus);
    }

    // ── Worksheet rejection rate ──────────────────────────────────────────────────

    public Map<String, Object> getWorksheetRejectionRate(Long tenantId, String timeRange) {
        List<WorksheetExecution> executions = worksheetExecutionRepository.findAllByTenant(tenantId);
        LocalDate from = resolveFrom(timeRange);
        List<WorksheetExecution> filtered = executions.stream()
                .filter(w -> w.getExecutedAt() == null || !w.getExecutedAt().toLocalDate().isBefore(from))
                .toList();

        long total = filtered.size();
        long rejected = filtered.stream().filter(w -> "REJECTED".equals(w.getStatus())).count();

        return Map.of("total", total, "rejected", rejected,
                "rejectionRate", total > 0 ? Math.round(100.0 * rejected / total) : 0);
    }

    // ── Instrument uptime ─────────────────────────────────────────────────────────

    public Map<String, Object> getInstrumentUptime(Long tenantId, Long branchId, String timeRange) {
        List<InstrumentMaster> instruments = branchId != null
                ? instrumentMasterRepository.findByTenantIdAndBranchId(tenantId, branchId)
                : instrumentMasterRepository.findByTenantId(tenantId);

        long available = instruments.stream().filter(i -> "AVAILABLE".equals(i.getStatus())).count();
        long total = instruments.size();

        return Map.of(
                "totalInstruments", total,
                "available", available,
                "unavailable", total - available,
                "uptimePct", total > 0 ? Math.round(100.0 * available / total) : 0
        );
    }

    // ── Instrument calibration status ─────────────────────────────────────────────

    public Map<String, Object> getInstrumentCalibrationStatus(Long tenantId, Long branchId) {
        List<InstrumentCalibration> all = instrumentCalibrationRepository.findAll().stream()
                .filter(c -> c.getTenant() != null && c.getTenant().getId().equals(tenantId))
                .filter(c -> branchId == null || (c.getBranch() != null && c.getBranch().getId().equals(branchId)))
                .toList();

        Map<String, Long> byStatus = all.stream()
                .filter(c -> c.getStatus() != null)
                .collect(Collectors.groupingBy(InstrumentCalibration::getStatus, Collectors.counting()));

        long overdue = branchId != null
                ? instrumentCalibrationRepository.findOverdueForCalibration(tenantId, branchId, LocalDate.now()).size()
                : 0L;

        return Map.of("byStatus", byStatus, "overdueCount", overdue, "total", all.size());
    }

    // ── Instrument usage by product ───────────────────────────────────────────────

    public Map<String, Object> getInstrumentUsageByProduct(Long tenantId, Long branchId, String timeRange) {
        List<InstrumentMaster> instruments = branchId != null
                ? instrumentMasterRepository.findByTenantIdAndBranchId(tenantId, branchId)
                : instrumentMasterRepository.findByTenantId(tenantId);

        LocalDate from = resolveFrom(timeRange);
        Map<String, Long> byInstrument = new LinkedHashMap<>();

        for (InstrumentMaster instrument : instruments) {
            List<InstrumentReservation> reservations = instrumentReservationRepository
                    .findByInstrument_Id(instrument.getId());
            long count = reservations.stream()
                    .filter(r -> r.getStartTime() == null || !r.getStartTime().toLocalDate().isBefore(from))
                    .count();
            if (count > 0) {
                byInstrument.put(instrument.getInstrumentCode(), count);
            }
        }

        return Map.of("reservationsByInstrument", byInstrument, "instrumentCount", instruments.size());
    }

    // ── Inventory issuance vs destruction ────────────────────────────────────────

    public Map<String, Object> getInventoryIssuanceDestruction(Long tenantId, Long branchId, String timeRange) {
        List<InventoryReagent> reagents = branchId != null
                ? inventoryReagentRepository.findByTenantIdAndBranchId(tenantId, branchId)
                : inventoryReagentRepository.findByTenantId(tenantId);

        LocalDate from = resolveFrom(timeRange);
        BigDecimal issued = BigDecimal.ZERO;
        BigDecimal consumed = BigDecimal.ZERO;
        int totalMovements = 0;

        for (InventoryReagent reagent : reagents) {
            List<InventoryReagentLot> lots = inventoryReagentLotRepository.findByReagentId(reagent.getId());
            for (InventoryReagentLot lot : lots) {
                List<InventoryMovement> movements = inventoryMovementRepository
                        .findByLotIdOrderByPerformedAtDesc(lot.getId());
                for (InventoryMovement m : movements) {
                    if (m.getPerformedAt() != null && m.getPerformedAt().toLocalDate().isBefore(from)) continue;
                    totalMovements++;
                    if ("ISSUE".equals(m.getMovementType()) && m.getQuantity() != null)
                        issued = issued.add(m.getQuantity());
                    if ("CONSUME".equals(m.getMovementType()) && m.getQuantity() != null)
                        consumed = consumed.add(m.getQuantity());
                }
            }
        }

        return Map.of("issuedQty", issued, "consumedQty", consumed, "totalMovements", totalMovements);
    }

    // ── Inventory cost per product ────────────────────────────────────────────────

    public Map<String, Object> getInventoryCostPerProduct(Long tenantId, Long branchId) {
        List<InventoryReagent> reagents = branchId != null
                ? inventoryReagentRepository.findByTenantIdAndBranchId(tenantId, branchId)
                : inventoryReagentRepository.findByTenantId(tenantId);

        List<Map<String, Object>> data = reagents.stream().map(r -> {
            List<InventoryReagentLot> lots = inventoryReagentLotRepository.findByReagentId(r.getId());
            BigDecimal totalQty = lots.stream()
                    .filter(l -> l.getCurrentQty() != null)
                    .map(InventoryReagentLot::getCurrentQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int lotCount = lots.size();
            return Map.<String, Object>of(
                    "reagentId", r.getId(), "name", r.getName(),
                    "code", r.getReagentCode(), "totalQty", totalQty, "lotCount", lotCount);
        }).toList();

        return Map.of("products", data, "count", data.size());
    }

    // ── QA deviation trend ────────────────────────────────────────────────────────

    public Map<String, Object> getDeviationTrend(Long tenantId, String timeRange) {
        List<Deviation> deviations = deviationRepository.findByTenantId(tenantId);
        LocalDate from = resolveFrom(timeRange);
        List<Deviation> filtered = deviations.stream()
                .filter(d -> d.getRaisedAt() == null || !d.getRaisedAt().toLocalDate().isBefore(from))
                .toList();

        Map<String, Long> byMonth = filtered.stream()
                .filter(d -> d.getRaisedAt() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getRaisedAt().toLocalDate().withDayOfMonth(1).toString(),
                        Collectors.counting()));

        return Map.of("total", filtered.size(), "byMonth", new TreeMap<>(byMonth));
    }

    // ── QA deviation severity breakdown ──────────────────────────────────────────

    public Map<String, Object> getDeviationSeverity(Long tenantId) {
        List<Deviation> deviations = deviationRepository.findByTenantId(tenantId);
        Map<String, Long> bySeverity = deviations.stream()
                .filter(d -> d.getSeverity() != null)
                .collect(Collectors.groupingBy(Deviation::getSeverity, Collectors.counting()));
        Map<String, Long> byStatus = deviations.stream()
                .filter(d -> d.getStatus() != null)
                .collect(Collectors.groupingBy(Deviation::getStatus, Collectors.counting()));
        return Map.of("total", deviations.size(), "bySeverity", bySeverity, "byStatus", byStatus);
    }

    // ── QA CAPA closure rate ──────────────────────────────────────────────────────

    public Map<String, Object> getCapaClosureRate(Long tenantId, String timeRange) {
        List<Capa> capas = capaRepository.findByTenantId(tenantId);
        long closed = capas.stream().filter(c -> "CLOSED".equals(c.getStatus())).count();
        long total = capas.size();

        return Map.of("total", total, "closed", closed, "open", total - closed,
                "closureRate", total > 0 ? Math.round(100.0 * closed / total) : 0);
    }

    // ── QA root cause analysis ────────────────────────────────────────────────────

    public Map<String, Object> getRootCauseAnalysis(Long tenantId) {
        List<Deviation> deviations = deviationRepository.findByTenantId(tenantId);
        Map<String, Long> byType = deviations.stream()
                .filter(d -> d.getDeviationType() != null)
                .collect(Collectors.groupingBy(Deviation::getDeviationType, Collectors.counting()));
        Map<String, Long> bySourceType = deviations.stream()
                .filter(d -> d.getSourceType() != null)
                .collect(Collectors.groupingBy(Deviation::getSourceType, Collectors.counting()));
        return Map.of("total", deviations.size(), "byDeviationType", byType, "bySourceType", bySourceType);
    }

    // ── AI OOS risk trend ─────────────────────────────────────────────────────────

    public Map<String, Object> getAiOosRiskTrend(Long tenantId, Long branchId) {
        List<AiInventoryForecast> forecasts = aiInventoryForecastRepository.findByTenantIdAndBranchId(tenantId, branchId != null ? branchId : 0L);

        List<Map<String, Object>> data = forecasts.stream().map(f -> Map.<String, Object>of(
                "itemId", f.getItemId(),
                "itemType", f.getItemType(),
                "forecastDate", f.getForecastDate() != null ? f.getForecastDate().toString() : null,
                "predictedUsage", f.getPredictedUsage() != null ? f.getPredictedUsage() : BigDecimal.ZERO,
                "modelVersion", f.getModelVersion() != null ? f.getModelVersion() : ""
        )).toList();

        return Map.of("forecasts", data, "count", data.size());
    }

    // ── AI consumption forecast ───────────────────────────────────────────────────

    public Map<String, Object> getAiConsumptionForecast(Long tenantId, Long branchId) {
        List<AiInventoryForecast> forecasts = aiInventoryForecastRepository.findByTenantIdAndBranchId(tenantId, branchId != null ? branchId : 0L);

        Map<String, List<Map<String, Object>>> byItem = forecasts.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getItemType() + ":" + f.getItemId(),
                        Collectors.mapping(f -> Map.<String, Object>of(
                                "date", f.getForecastDate() != null ? f.getForecastDate().toString() : "",
                                "predictedUsage", f.getPredictedUsage() != null ? f.getPredictedUsage() : BigDecimal.ZERO),
                                Collectors.toList())));

        return Map.of("byItem", byItem, "count", forecasts.size());
    }

    // ── AI instrument failure prediction ─────────────────────────────────────────

    public Map<String, Object> getAiInstrumentFailure(Long tenantId, Long branchId) {
        List<PredictiveAlert> alerts = branchId != null
                ? predictiveAlertRepository.findByTenantIdAndBranchId(tenantId, branchId)
                : predictiveAlertRepository.findAll().stream()
                    .filter(a -> a.getTenantId() != null && a.getTenantId().equals(tenantId))
                    .toList();

        List<Map<String, Object>> data = alerts.stream()
                .filter(a -> "INSTRUMENT_FAILURE".equals(a.getAlertType()) || a.getAlertType() != null)
                .map(a -> Map.<String, Object>of(
                        "alertId", a.getId(),
                        "alertType", a.getAlertType(),
                        "severity", a.getSeverity() != null ? a.getSeverity() : "MEDIUM",
                        "message", a.getMessage() != null ? a.getMessage() : "",
                        "status", a.getStatus(),
                        "createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null
                )).toList();

        return Map.of("alerts", data, "count", data.size());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    public Map<String, Object> getInventoryConsumptionByTimeRange(Long tenantId, Long branchId, String timeRange) {
        LocalDate from = resolveFrom(timeRange);
        return getConsumptionTrends(tenantId, branchId, null, from, null);
    }

    private LocalDate resolveFrom(String timeRange) {
        if (timeRange == null) return LocalDate.now().minusDays(30);
        return switch (timeRange) {
            case "7d" -> LocalDate.now().minusDays(7);
            case "90d" -> LocalDate.now().minusDays(90);
            case "365d" -> LocalDate.now().minusDays(365);
            default -> LocalDate.now().minusDays(30);
        };
    }
}
