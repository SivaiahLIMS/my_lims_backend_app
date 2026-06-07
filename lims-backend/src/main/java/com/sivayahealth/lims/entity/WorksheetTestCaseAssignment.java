package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_test_case_assignment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"worksheet_id", "test_case_id", "analyst_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetTestCaseAssignment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worksheet_id", nullable = false)
    private WorksheetMaster worksheet;

    /** NULL means the assignment covers all test cases in the worksheet */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id")
    private DocumentTestCase testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyst_id", nullable = false)
    private AppUser analyst;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private AppUser assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    /** ASSIGNED | IN_PROGRESS | COMPLETED | REASSIGNED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "ASSIGNED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retest_of_assignment_id")
    private WorksheetTestCaseAssignment retestOfAssignment;
}
