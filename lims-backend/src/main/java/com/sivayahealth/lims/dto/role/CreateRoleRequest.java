package com.sivayahealth.lims.dto.role;

public record CreateRoleRequest(
    String code,
    String name,
    String description
) {}
