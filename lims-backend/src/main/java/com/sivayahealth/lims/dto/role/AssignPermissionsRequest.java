package com.sivayahealth.lims.dto.role;

import java.util.List;

public record AssignPermissionsRequest(
    List<String> permissionCodes
) {}
