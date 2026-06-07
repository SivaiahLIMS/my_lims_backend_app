package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_slot_group_value",
    uniqueConstraints = @UniqueConstraint(columnNames = {"worksheet_id", "slot_group_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetSlotGroupValue {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "value_id")
    private Long valueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worksheet_id", nullable = false)
    private WorksheetMaster worksheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_group_id", nullable = false)
    private DocumentSlotGroup slotGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private DocumentTestCase testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /** Free-text measurement entered by analyst */
    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private InstrumentMaster instrument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chemical_lot_id")
    private InventoryReagentLot chemicalLot;

    /** ELN reference code / free text */
    @Column(name = "eln_ref", columnDefinition = "TEXT")
    private String elnRef;

    /** Elapsed milliseconds from the associated timer (null when timer not used) */
    @Column(name = "elapsed_ms")
    private Long elapsedMs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by")
    private AppUser enteredBy;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private AppUser modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
