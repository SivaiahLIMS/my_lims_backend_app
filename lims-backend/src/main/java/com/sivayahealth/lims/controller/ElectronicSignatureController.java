package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.signature.SignatureRequest;
import com.sivayahealth.lims.entity.ElectronicSignature;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ElectronicSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/electronic-signatures")
@RequiredArgsConstructor
@Tag(name = "Electronic Signatures", description = "21 CFR Part 11 compliant e-signature with password re-authentication")
@SecurityRequirement(name = "bearerAuth")
public class ElectronicSignatureController {

    private final ElectronicSignatureService signatureService;

    @PostMapping("/sign")
    @Operation(summary = "Apply an electronic signature",
               description = "Re-authenticates the caller with their password before recording the signature")
    public ResponseEntity<ElectronicSignature> sign(
            @AuthenticationPrincipal LimsUserDetails principal,
            @RequestBody SignatureRequest req,
            HttpServletRequest httpRequest) {

        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = httpRequest.getRemoteAddr();
        }

        ElectronicSignature sig = signatureService.sign(
                principal.getUser().getId(),
                req.getPassword(),
                req.getAction(),
                req.getEntityType(),
                req.getEntityId(),
                req.getReason(),
                ip);

        return ResponseEntity.ok(sig);
    }

    @GetMapping
    @Operation(summary = "List signatures for an entity")
    public ResponseEntity<List<ElectronicSignature>> list(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(signatureService.getSignaturesForEntity(entityType, entityId));
    }
}
