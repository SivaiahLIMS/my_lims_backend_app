package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.auth.LoginRequest;
import com.sivayahealth.lims.dto.auth.LoginResponse;
import com.sivayahealth.lims.dto.auth.MeResponse;
import com.sivayahealth.lims.dto.auth.RefreshTokenRequest;
import com.sivayahealth.lims.security.JwtTokenProvider;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and token management")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal LimsUserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile and permissions")
    public ResponseEntity<MeResponse> me(
            @AuthenticationPrincipal LimsUserDetails userDetails,
            HttpServletRequest request) {
        Long branchId = extractBranchId(request);
        List<String> permissions = userDetails.getPermissions();
        return ResponseEntity.ok(new MeResponse(
                userDetails.getUser().getId(),
                userDetails.getUsername(),
                userDetails.getUser().getEmail(),
                userDetails.getTenantId(),
                branchId,
                List.of(),
                permissions
        ));
    }

    private Long extractBranchId(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return tokenProvider.getBranchIdFromToken(bearer.substring(7));
        }
        return null;
    }
}
