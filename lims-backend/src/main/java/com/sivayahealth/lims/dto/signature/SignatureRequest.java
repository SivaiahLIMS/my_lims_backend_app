package com.sivayahealth.lims.dto.signature;

import lombok.Data;

@Data
public class SignatureRequest {
    private String password;
    private String action;
    private String entityType;
    private Long entityId;
    private String reason;
}
