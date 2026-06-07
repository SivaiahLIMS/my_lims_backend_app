package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_timer_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetTimerLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timer_log_id")
    private Long timerLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worksheet_id", nullable = false)
    private WorksheetMaster worksheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private DocumentTestCase testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_group_id")
    private DocumentSlotGroup slotGroup;

    /** Client-supplied stable identifier for the timer UI widget */
    @Column(name = "timer_id", length = 100)
    private String timerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by")
    private AppUser startedBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stopped_by")
    private AppUser stoppedBy;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    /** Total accumulated milliseconds at the time of last stop/pause */
    @Column(name = "elapsed_ms")
    private Long elapsedMs;

    /** RUNNING | PAUSED | STOPPED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "RUNNING";
}
