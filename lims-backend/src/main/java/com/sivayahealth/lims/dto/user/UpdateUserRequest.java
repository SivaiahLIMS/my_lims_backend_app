package com.sivayahealth.lims.dto.user;

public record UpdateUserRequest(
    String email,
    String firstName,
    String lastName,
    String phone,
    String status
) {}
