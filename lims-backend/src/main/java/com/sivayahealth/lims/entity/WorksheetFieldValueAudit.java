package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_field_value_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetFieldValueAudit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worksheet_id", nullable = false)
    private Long worksheetId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "test_case_id")
    private Long testCaseId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(name = "change_type", nullable = false, length = 10)
    @Builder.Default
    private String changeType = "UPDATE";
}
