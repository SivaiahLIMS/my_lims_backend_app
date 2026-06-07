package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_template")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetTemplate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "template_name", length = 500, nullable = false)
    private String templateName;

    /** UPLOAD | MANUAL | AUTO */
    @Column(name = "mode", length = 20, nullable = false)
    private String mode = "MANUAL";

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /** DRAFT | IN_REVIEW | APPROVED | REJECTED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";

    @Column(name = "template_json", columnDefinition = "TEXT")
    private String templateJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id")
    private DocumentMaster sourceDocument;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private AppUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AppUser approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private AppUser modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
