package com.sivayahealth.lims.service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.sivayahealth.lims.dto.worksheet.GenerateTemplateRequest;
import com.sivayahealth.lims.dto.worksheet.WorksheetTemplateResponse;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Generates worksheet templates from natural-language descriptions using
 * Vertex AI Gemini. Uses Application Default Credentials (automatic on Cloud Run).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetAiService {

    private final WorksheetTemplateRepository templateRepo;
    private final AppUserRepository           userRepo;
    private final TenantRepository            tenantRepo;
    private final BranchRepository            branchRepo;
    private final WorksheetTemplateService    templateService;

    @Value("${gcp.project-id}")
    private String gcpProjectId;

    @Value("${gcp.vertexai.location:us-central1}")
    private String location;

    @Value("${gcp.vertexai.model:gemini-1.5-flash}")
    private String modelName;

    @Transactional
    public WorksheetTemplateResponse generateTemplate(Long tenantId, Long actorId,
                                                       GenerateTemplateRequest req) {
        if (gcpProjectId == null || gcpProjectId.isBlank()) {
            throw LimsException.badRequest("GCP_PROJECT_ID is not configured — cannot call Vertex AI");
        }

        String prompt = buildPrompt(req);
        String templateJson = callGemini(prompt);

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepo.findById(req.getBranchId())
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser actor = userRepo.findById(actorId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        WorksheetTemplate t = WorksheetTemplate.builder()
                .tenant(tenant)
                .branch(branch)
                .templateName("AI Generated - " + req.getProductDescription())
                .mode("LLM")
                .templateJson(templateJson)
                .status("DRAFT")
                .version(1)
                .createdBy(actor)
                .createdAt(LocalDateTime.now())
                .build();

        return templateService.toResponse(templateRepo.save(t));
    }

    private String callGemini(String prompt) {
        try (VertexAI vertexAI = new VertexAI(gcpProjectId, location)) {
            GenerativeModel model = new GenerativeModel(modelName, vertexAI);
            GenerateContentResponse response = model.generateContent(prompt);
            String raw = ResponseHandler.getText(response);
            return extractJson(raw);
        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage(), e);
            throw LimsException.badRequest("LLM generation failed: " + e.getMessage());
        }
    }

    private String buildPrompt(GenerateTemplateRequest req) {
        return """
                You are a LIMS (Laboratory Information Management System) expert.
                Generate a worksheet template JSON for the following pharmaceutical product.

                Product / description: %s
                Requested tests: %s
                Additional context: %s

                Output ONLY a valid JSON object (no markdown, no prose) following this exact schema:
                {
                  "templateName": "<string>",
                  "sections": [
                    {
                      "sectionName": "<string>",
                      "slotGroups": [
                        {
                          "groupName": "<string>",
                          "slots": [
                            {
                              "slotKey": "<string>",
                              "label": "<string>",
                              "type": "TEXT|NUMBER|DROPDOWN|DATETIME|BOOLEAN",
                              "unit": "<string or null>",
                              "required": true|false,
                              "options": ["<value>"] | null
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }

                Include all standard LIMS fields: sample identification, analyst, instrument,
                date/time, test parameters, results, acceptance criteria, pass/fail flag,
                and reviewer sign-off. Tailor the slots to the requested tests.
                """.formatted(
                req.getProductDescription(),
                req.getRequestedTests() != null ? req.getRequestedTests() : "standard QC tests",
                req.getAdditionalContext() != null ? req.getAdditionalContext() : "none"
        );
    }

    /** Strips markdown code-fence wrappers Gemini sometimes adds. */
    private String extractJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence    = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return trimmed;
    }
}
