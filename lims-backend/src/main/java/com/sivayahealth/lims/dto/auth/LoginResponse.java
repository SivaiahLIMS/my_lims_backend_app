package com.sivayahealth.lims.dto.auth;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long userId,
    String username,
    Long tenantId,
    Long branchId
) {
    public LoginResponse(String accessToken, String refreshToken, Long userId,
                         String username, Long tenantId, Long branchId) {
        this(accessToken, refreshToken, "Bearer", userId, username, tenantId, branchId);
    }
}
