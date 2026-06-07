package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_slot_group",
    uniqueConstraints = @UniqueConstraint(columnNames = {"test_case_id", "group_index"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentSlotGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_group_id")
    private Long slotGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private DocumentTestCase testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private DocumentTemplateBlock block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "group_index", nullable = false)
    private Integer groupIndex;

    @Column(name = "label", columnDefinition = "TEXT")
    private String label;

    /** TRUE when this slot group comes from a "Remark: ---" — ELN only, no textbox/dropdowns */
    @Column(name = "is_remark", nullable = false)
    private boolean isRemark = false;

    @Column(name = "is_timer_enabled", nullable = false)
    private boolean isTimerEnabled = false;
}
