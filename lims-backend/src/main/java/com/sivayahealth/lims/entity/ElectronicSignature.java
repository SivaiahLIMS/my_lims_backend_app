package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "electronic_signature")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElectronicSignature {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "signed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime signedAt = LocalDateTime.now();
}
