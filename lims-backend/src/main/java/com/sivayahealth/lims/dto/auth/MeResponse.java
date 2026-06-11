package com.sivayahealth.lims.dto.auth;

import java.util.List;

public record MeResponse(
    Long userId,
    String username,
    String email,
    Long tenantId,
    Long branchId,
    List<String> roles,
    List<String> permissions
) {}
