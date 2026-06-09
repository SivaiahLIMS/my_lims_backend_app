package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ar_sequence")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArSequence {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "last_seq", nullable = false)
    private Long lastSeq;
}
