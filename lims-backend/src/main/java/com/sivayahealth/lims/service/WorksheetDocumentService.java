package com.sivayahealth.lims.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetDocumentService {

    private final WorksheetMasterRepository            worksheetRepo;
    private final DocumentTestCaseRepository           testCaseRepo;
    private final DocumentTemplateBlockRepository      blockRepo;
    private final DocumentFieldSlotRepository          slotRepo;
    private final WorksheetFieldValueRepository        fieldValueRepo;
    private final WorksheetTestCaseResultRepository    resultRepo;
    private final AppUserRepository                    userRepo;
    private final WorksheetValidationEventRepository   validationEventRepo;
    private final InstrumentMasterRepository           instrumentRepo;
    private final InventoryReagentLotRepository        reagentLotRepo;
    private final AuditService                         auditService;
    private final WorksheetFieldValueAuditRepository   fieldValueAuditRepo;
    private final QaService                             qaService;

    private final ObjectMapper objectMapper;

    // ── Template rendering ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WorksheetTemplateView getTemplateView(Long tenantId, Long branchId, Long worksheetId) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, branchId, worksheetId);
        if (worksheet.getDocumentVersion() == null) {
            throw LimsException.badRequest("Worksheet has no document version linked");
        }
        Long versionId = worksheet.getDocumentVersion().getId();

        List<DocumentTestCase> testCases =
                testCaseRepo.findByDocumentVersion_IdOrderByTestCaseIndexAsc(versionId);

        List<TestCaseView> tcViews = new ArrayList<>();
        for (DocumentTestCase tc : testCases) {
            List<DocumentTemplateBlock> blocks =
                    blockRepo.findByTestCase_TestCaseIdOrderByBlockIndexAsc(tc.getTestCaseId());
            List<DocumentFieldSlot> slots =
                    slotRepo.findByTestCase_TestCaseIdOrderByFieldIndexAsc(tc.getTestCaseId());
            List<WorksheetFieldValue> values =
                    fieldValueRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                            worksheetId, tc.getTestCaseId());

            Optional<WorksheetTestCaseResult> result =
                    resultRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                            worksheetId, tc.getTestCaseId());

            tcViews.add(new TestCaseView(tc, blocks, slots, values, result.orElse(null)));
        }

        return new WorksheetTemplateView(worksheet, tcViews);
    }

    // ── Field value save / upsert ─────────────────────────────────────────────

    @Transactional
    public WorksheetFieldValue saveFieldValue(Long tenantId, Long branchId,
                                               Long worksheetId, Long userId,
                                               Long slotId,
                                               BigDecimal numericValue,
                                               String unit,
                                               String qualifier,
                                               String comment) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, branchId, worksheetId);
        if (!"IN_PROGRESS".equals(worksheet.getStatus()) && !"DRAFT".equals(worksheet.getStatus())) {
            throw LimsException.badRequest(
                    "Fields can only be filled when worksheet is DRAFT or IN_PROGRESS");
        }
        AppUser analyst = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        DocumentFieldSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> LimsException.notFound("Field slot not found"));

        Optional<WorksheetFieldValue> existing =
                fieldValueRepo.findByWorksheet_WorksheetIdAndSlot_SlotId(worksheetId, slotId);

        String oldValue = existing.map(v ->
                v.getNumericValue() != null ? v.getNumericValue().toPlainString() : v.getTextValue()
        ).orElse(null);

        WorksheetFieldValue fv = existing.orElseGet(() -> WorksheetFieldValue.builder()
                .worksheet(worksheet)
                .slot(slot)
                .testCase(slot.getTestCase())
                .tenant(worksheet.getTenant())
                .branch(worksheet.getBranch())
                .build());

        fv.setNumericValue(numericValue);
        fv.setUnit(unit);
        fv.setQualifier(qualifier != null ? qualifier : "EXACT");
        fv.setComment(comment);

        // Validate instrument active status when slot is INSTRUMENT_SELECT type
        if ("INSTRUMENT_SELECT".equals(slot.getFieldType()) && fv.getRefId() != null) {
            InstrumentMaster instrument = instrumentRepo.findById(fv.getRefId())
                    .orElseThrow(() -> LimsException.notFound("Instrument not found"));
            if (!"ACTIVE".equalsIgnoreCase(instrument.getStatus())) {
                throw LimsException.badRequest(
                        "Instrument '" + instrument.getName() + "' is not ACTIVE (status: "
                                + instrument.getStatus() + ")");
            }
        }

        // Validate chemical lot not expired and available when slot is CHEMICAL_SELECT type
        if ("CHEMICAL_SELECT".equals(slot.getFieldType()) && fv.getRefId() != null) {
            InventoryReagentLot lot = reagentLotRepo.findById(fv.getRefId())
                    .orElseThrow(() -> LimsException.notFound("Reagent lot not found"));
            if (lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(LocalDate.now())) {
                throw LimsException.badRequest(
                        "Reagent lot '" + lot.getLotNumber() + "' is expired (expiry: "
                                + lot.getExpiryDate() + ")");
            }
            if (!"AVAILABLE".equalsIgnoreCase(lot.getStatus())) {
                throw LimsException.badRequest(
                        "Reagent lot '" + lot.getLotNumber() + "' is not available (status: "
                                + lot.getStatus() + ")");
            }
        }

        if (existing.isEmpty()) {
            fv.setEnteredBy(analyst);
            fv.setEnteredAt(LocalDateTime.now());
        } else {
            fv.setModifiedBy(analyst);
            fv.setModifiedAt(LocalDateTime.now());
        }

        WorksheetFieldValue saved = fieldValueRepo.save(fv);

        String newValue = numericValue != null ? numericValue.toPlainString() : fv.getTextValue();

        fieldValueAuditRepo.save(WorksheetFieldValueAudit.builder()
                .worksheetId(worksheetId)
                .slotId(slotId)
                .testCaseId(slot.getTestCase() != null ? slot.getTestCase().getTestCaseId() : null)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(userId)
                .changeType(existing.isPresent() ? "UPDATE" : "INSERT")
                .build());

        auditService.log(tenantId, userId, "WORKSHEET_FIELD_VALUE", saved.getValueId(),
                existing.isPresent() ? "UPDATE" : "INSERT", oldValue, newValue);

        return saved;
    }

    // ── Formula computation ───────────────────────────────────────────────────

    @Transactional
    public WorksheetTestCaseResult computeResult(Long tenantId, Long branchId,
                                                  Long worksheetId,
                                                  Long testCaseId,
                                                  Long userId,
                                                  String resultUnit) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, branchId, worksheetId);
        AppUser analyst = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        DocumentTestCase testCase = testCaseRepo.findById(testCaseId)
                .orElseThrow(() -> LimsException.notFound("Test case not found"));

        List<DocumentFieldSlot> slots =
                slotRepo.findByTestCase_TestCaseIdOrderByFieldIndexAsc(testCaseId);
        List<WorksheetFieldValue> values =
                fieldValueRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                        worksheetId, testCaseId);

        Map<Long, BigDecimal> slotValueMap = new HashMap<>();
        for (WorksheetFieldValue v : values) {
            if (v.getNumericValue() != null) {
                slotValueMap.put(v.getSlot().getSlotId(), v.getNumericValue());
            }
        }

        List<String> missing = new ArrayList<>();
        for (DocumentFieldSlot slot : slots) {
            if ("NUMBER".equals(slot.getFieldType()) && !slotValueMap.containsKey(slot.getSlotId())) {
                missing.add(slot.getFieldVariable() + " (" + slot.getLabel() + ")");
            }
        }
        if (!missing.isEmpty()) {
            throw LimsException.badRequest("Missing values for: " + String.join(", ", missing));
        }

        Map<String, BigDecimal> varValues = new HashMap<>();
        for (DocumentFieldSlot slot : slots) {
            if (slotValueMap.containsKey(slot.getSlotId())) {
                varValues.put(slot.getFieldVariable(), slotValueMap.get(slot.getSlotId()));
            }
        }

        String expression = testCase.getFormulaExpression();
        if (expression == null || expression.isBlank()) {
            expression = testCase.getFormulaText();
        }

        String substituted = substituteExpression(expression, varValues);
        BigDecimal computed = evaluate(substituted);

        // Auto-determine passFail from OOS validation events for this test case
        List<WorksheetValidationEvent> events =
                validationEventRepo.findByWorksheetIdAndTestCaseIdOrderByValidatedAtDesc(
                        worksheetId, testCaseId);
        boolean hasOos = events.stream().anyMatch(e -> "OOS".equals(e.getStatus()));
        String autoPassFail = hasOos ? "FAIL" : "PENDING";

        Optional<WorksheetTestCaseResult> existing =
                resultRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                        worksheetId, testCaseId);

        String oldComputed = existing
                .map(r -> r.getComputedResult() != null ? r.getComputedResult().toPlainString() : null)
                .orElse(null);

        WorksheetTestCaseResult result = existing.orElseGet(() ->
                WorksheetTestCaseResult.builder()
                        .worksheet(worksheet)
                        .testCase(testCase)
                        .tenant(worksheet.getTenant())
                        .branch(worksheet.getBranch())
                        .build());

        result.setFormulaSubstituted(substituted);
        result.setComputedResult(computed);
        result.setResultUnit(resultUnit);
        result.setPassFail(autoPassFail);
        if (hasOos) {
            result.setReviewComments(
                    "Auto-set to FAIL: OOS validation event detected. Reviewer must provide justification to override.");
        }
        result.setComputedBy(analyst);
        result.setComputedAt(LocalDateTime.now());

        WorksheetTestCaseResult saved = resultRepo.save(result);

        auditService.log(tenantId, userId, "WORKSHEET_TEST_CASE_RESULT", saved.getResultId(),
                existing.isPresent() ? "RECOMPUTE" : "COMPUTE", oldComputed,
                computed.toPlainString());

        return saved;
    }

    // ── Result review ─────────────────────────────────────────────────────────

    @Transactional
    public WorksheetTestCaseResult reviewResult(Long tenantId, Long branchId,
                                                 Long worksheetId,
                                                 Long testCaseId,
                                                 Long userId,
                                                 String passFail,
                                                 String comments) {
        loadWorksheet(tenantId, branchId, worksheetId);
        AppUser reviewer = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        WorksheetTestCaseResult result =
                resultRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                                worksheetId, testCaseId)
                        .orElseThrow(() -> LimsException.notFound(
                                "No computed result found for this test case"));

        if (!Set.of("PASS", "FAIL").contains(passFail)) {
            throw LimsException.badRequest("passFail must be PASS or FAIL");
        }

        // Justification required when overriding an auto-FAIL (OOS) to PASS
        if ("PASS".equals(passFail) && "FAIL".equals(result.getPassFail())) {
            if (comments == null || comments.isBlank()) {
                throw LimsException.badRequest(
                        "A justification comment is required when overriding a FAIL result to PASS");
            }
        }

        String oldPassFail = result.getPassFail();

        result.setPassFail(passFail);
        result.setReviewedBy(reviewer);
        result.setReviewedAt(LocalDateTime.now());
        result.setReviewComments(comments);

        WorksheetTestCaseResult saved = resultRepo.save(result);

        auditService.log(tenantId, userId, "WORKSHEET_TEST_CASE_RESULT", saved.getResultId(),
                "REVIEW", oldPassFail, passFail);

        // Auto-create OOS case when reviewer confirms FAIL (not a re-confirmation of existing FAIL)
        if ("FAIL".equals(passFail) && !"FAIL".equals(oldPassFail)) {
            WorksheetMaster worksheet = saved.getWorksheet();
            if (worksheet.getSample() != null) {
                com.sivayahealth.lims.dto.qa.CreateOosRequest oosReq =
                        new com.sivayahealth.lims.dto.qa.CreateOosRequest();
                oosReq.setBranchId(branchId);
                oosReq.setSampleId(worksheet.getSample().getId());
                oosReq.setTestId(testCaseId);
                oosReq.setOosType("OOS");
                oosReq.setDescription("Auto-raised: test case " + testCaseId
                        + " on worksheet " + worksheetId + " reviewed as FAIL");
                qaService.createOos(tenantId, branchId, oosReq, userId);
            }
        }

        return saved;
    }

    // ── Expression evaluation ─────────────────────────────────────────────────

    private String substituteExpression(String expression, Map<String, BigDecimal> varValues) {
        List<String> vars = varValues.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        String result = expression;
        for (String var : vars) {
            result = result.replaceAll("\\b" + Pattern.quote(var) + "\\b",
                    varValues.get(var).toPlainString());
        }
        return result;
    }

    private BigDecimal evaluate(String expression) {
        try {
            // Preserve both E and e for scientific notation
            String cleaned = expression.replaceAll("[^0-9+\\-*/().Ee]", " ").trim();
            ExprParser parser = new ExprParser(cleaned);
            return parser.parse().setScale(6, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Formula evaluation failed for expression '{}': {}", expression, e.getMessage());
            throw LimsException.badRequest("Could not evaluate formula: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public List<WorksheetFieldValueAudit> getFieldAuditTrail(Long worksheetId) {
        return fieldValueAuditRepo.findByWorksheetIdOrderByChangedAtDesc(worksheetId);
    }

    private WorksheetMaster loadWorksheet(Long tenantId, Long branchId, Long worksheetId) {
        WorksheetMaster w = worksheetRepo.findById(worksheetId)
                .orElseThrow(() -> LimsException.notFound("Worksheet not found"));
        if (!w.getTenant().getId().equals(tenantId) || !w.getBranch().getId().equals(branchId)) {
            throw LimsException.notFound("Worksheet not found");
        }
        return w;
    }

    // ── View records ──────────────────────────────────────────────────────────

    public record WorksheetTemplateView(
            WorksheetMaster worksheet,
            List<TestCaseView> testCases) {}

    public record TestCaseView(
            DocumentTestCase testCase,
            List<DocumentTemplateBlock> blocks,
            List<DocumentFieldSlot> slots,
            List<WorksheetFieldValue> values,
            WorksheetTestCaseResult result) {}

    // ── Arithmetic expression parser ──────────────────────────────────────────

    private static class ExprParser {
        private final String input;
        private int pos = 0;

        ExprParser(String input) { this.input = input.trim(); }

        BigDecimal parse() { BigDecimal v = expr(); skipWs(); return v; }

        private BigDecimal expr() {
            BigDecimal v = term();
            while (pos < input.length()) {
                skipWs();
                char c = peek();
                if (c == '+') { pos++; v = v.add(term()); }
                else if (c == '-') { pos++; v = v.subtract(term()); }
                else break;
            }
            return v;
        }

        private BigDecimal term() {
            BigDecimal v = factor();
            while (pos < input.length()) {
                skipWs();
                char c = peek();
                if (c == '*') { pos++; v = v.multiply(factor()); }
                else if (c == '/') {
                    pos++;
                    BigDecimal divisor = factor();
                    if (divisor.compareTo(BigDecimal.ZERO) == 0)
                        throw new ArithmeticException("Division by zero");
                    v = v.divide(divisor, 10, RoundingMode.HALF_UP);
                } else break;
            }
            return v;
        }

        private BigDecimal factor() {
            skipWs();
            if (pos >= input.length())
                throw new RuntimeException("Unexpected end of expression");
            char c = peek();
            if (c == '(') {
                pos++;
                BigDecimal v = expr();
                skipWs();
                if (pos < input.length() && peek() == ')') pos++;
                return v;
            }
            if (c == '-') { pos++; return factor().negate(); }
            return number();
        }

        private BigDecimal number() {
            skipWs();
            int start = pos;
            if (pos < input.length() && (peek() == '-' || peek() == '+')) pos++;
            while (pos < input.length() && (Character.isDigit(peek()) || peek() == '.'
                    || peek() == 'E' || peek() == 'e')) pos++;
            String num = input.substring(start, pos).trim();
            if (num.isEmpty()) throw new RuntimeException("Expected number at position " + start);
            return new BigDecimal(num);
        }

        private void skipWs() { while (pos < input.length() && input.charAt(pos) == ' ') pos++; }
        private char peek() { return input.charAt(pos); }
    }
}
