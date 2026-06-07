package com.sivayahealth.lims.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivayahealth.lims.entity.WorksheetTemplate;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.WorksheetTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

/**
 * Generates a DOCX representation of a WorksheetTemplate.
 * The template's templateJson must follow the schema:
 * { sections: [ { sectionName, slotGroups: [ { groupName, slots: [ { slotKey, label, type, unit, required, options } ] } ] } ] }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetExportService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WorksheetTemplateRepository templateRepo;
    private final ObjectMapper objectMapper;

    public byte[] exportDocx(Long tenantId, Long templateId) {
        WorksheetTemplate t = templateRepo.findByTemplateIdAndTenantId(templateId, tenantId)
                .orElseThrow(() -> LimsException.notFound("WorksheetTemplate not found"));

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildDocument(doc, t);
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("DOCX export failed for template {}: {}", templateId, e.getMessage(), e);
            throw LimsException.badRequest("DOCX export failed: " + e.getMessage());
        }
    }

    private void buildDocument(XWPFDocument doc, WorksheetTemplate t) throws Exception {
        // ── Header block ──────────────────────────────────────────────────────
        setPageMargins(doc);
        addTitle(doc, t.getTemplateName());
        addMetaTable(doc, t);

        if (t.getTemplateJson() == null || t.getTemplateJson().isBlank()) {
            addParagraph(doc, "(No template content defined)", false);
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(t.getTemplateJson());
        } catch (Exception e) {
            addParagraph(doc, "Invalid templateJson — cannot render.", false);
            return;
        }

        JsonNode sections = root.path("sections");
        if (!sections.isArray() || sections.isEmpty()) {
            addParagraph(doc, "(No sections in template)", false);
            return;
        }

        // ── Sections ──────────────────────────────────────────────────────────
        for (JsonNode section : sections) {
            String sectionName = section.path("sectionName").asText("Section");
            addHeading(doc, sectionName, 1);

            JsonNode slotGroups = section.path("slotGroups");
            if (!slotGroups.isArray()) continue;

            for (JsonNode group : slotGroups) {
                String groupName = group.path("groupName").asText("Group");
                addHeading(doc, groupName, 2);

                JsonNode slots = group.path("slots");
                if (!slots.isArray() || slots.isEmpty()) continue;

                XWPFTable table = doc.createTable();
                styleTable(table);

                // Header row
                XWPFTableRow headerRow = table.getRow(0);
                setCellText(headerRow.getCell(0), "Field", true);
                addCell(headerRow, "Type", true);
                addCell(headerRow, "Unit", true);
                addCell(headerRow, "Required", true);
                addCell(headerRow, "Value / Options", true);

                for (JsonNode slot : slots) {
                    XWPFTableRow row = table.createRow();
                    setCellText(row.getCell(0), slot.path("label").asText(slot.path("slotKey").asText()), false);
                    setCellText(row.getCell(1), slot.path("type").asText("TEXT"), false);
                    setCellText(row.getCell(2), slot.path("unit").asText(""), false);
                    setCellText(row.getCell(3), slot.path("required").asBoolean(false) ? "Yes" : "No", false);

                    String valueCell;
                    JsonNode opts = slot.path("options");
                    if (opts.isArray() && !opts.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (Iterator<JsonNode> it = opts.elements(); it.hasNext(); ) {
                            if (sb.length() > 0) sb.append(" / ");
                            sb.append(it.next().asText());
                        }
                        valueCell = sb.toString();
                    } else {
                        valueCell = "";  // blank fill-in field
                    }
                    setCellText(row.getCell(4), valueCell, false);
                }

                doc.createParagraph(); // spacing
            }
        }

        // ── Signature block ───────────────────────────────────────────────────
        addSignatureBlock(doc);
    }

    // ── Document helpers ──────────────────────────────────────────────────────

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(16);
        r.addBreak();
    }

    private void addMetaTable(XWPFDocument doc, WorksheetTemplate t) {
        XWPFTable meta = doc.createTable(3, 4);
        styleTable(meta);

        XWPFTableRow r0 = meta.getRow(0);
        setCellText(r0.getCell(0), "Template ID", true);
        setCellText(r0.getCell(1), String.valueOf(t.getTemplateId()), false);
        setCellText(r0.getCell(2), "Status", true);
        setCellText(r0.getCell(3), t.getStatus(), false);

        XWPFTableRow r1 = meta.getRow(1);
        setCellText(r1.getCell(0), "Version", true);
        setCellText(r1.getCell(1), "v" + t.getVersion(), false);
        setCellText(r1.getCell(2), "Mode", true);
        setCellText(r1.getCell(3), t.getMode(), false);

        XWPFTableRow r2 = meta.getRow(2);
        setCellText(r2.getCell(0), "Created At", true);
        setCellText(r2.getCell(1), t.getCreatedAt() != null ? t.getCreatedAt().format(DT) : "", false);
        setCellText(r2.getCell(2), "Approved At", true);
        setCellText(r2.getCell(3), t.getApprovedAt() != null ? t.getApprovedAt().format(DT) : "—", false);

        doc.createParagraph();
    }

    private void addHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Heading" + level);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(level == 1 ? 13 : 11);
    }

    private void addParagraph(XWPFDocument doc, String text, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
    }

    private void addSignatureBlock(XWPFDocument doc) {
        doc.createParagraph();
        addHeading(doc, "Signatures", 1);

        XWPFTable sig = doc.createTable(2, 4);
        styleTable(sig);

        XWPFTableRow h = sig.getRow(0);
        setCellText(h.getCell(0), "Analyst Name", true);
        setCellText(h.getCell(1), "Signature", true);
        setCellText(h.getCell(2), "Reviewer Name", true);
        setCellText(h.getCell(3), "Signature", true);

        XWPFTableRow v = sig.getRow(1);
        setCellText(v.getCell(0), "", false);
        setCellText(v.getCell(1), "", false);
        setCellText(v.getCell(2), "", false);
        setCellText(v.getCell(3), "", false);
    }

    private void addCell(XWPFTableRow row, String text, boolean bold) {
        XWPFTableCell cell = row.createCell();
        XWPFParagraph p = cell.getParagraphArray(0);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold) {
        XWPFParagraph p = cell.getParagraphArray(0);
        if (p == null) p = cell.addParagraph();
        p.getRuns().forEach(run -> run.setText("", 0));
        if (p.getRuns().isEmpty()) {
            XWPFRun r = p.createRun();
            r.setText(text);
            r.setBold(bold);
        } else {
            XWPFRun r = p.getRuns().get(0);
            r.setText(text, 0);
            r.setBold(bold);
        }
    }

    private void styleTable(XWPFTable table) {
        table.setTableAlignment(TableRowAlign.LEFT);
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) tblPr = table.getCTTbl().addNewTblPr();
        CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblW.setType(STTblWidth.PCT);
        tblW.setW(BigInteger.valueOf(5000));
    }

    private void setPageMargins(XWPFDocument doc) {
        CTBody body = doc.getDocument().getBody();
        CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        CTPageMar mar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        mar.setTop(BigInteger.valueOf(720));
        mar.setBottom(BigInteger.valueOf(720));
        mar.setLeft(BigInteger.valueOf(900));
        mar.setRight(BigInteger.valueOf(900));
    }
}
