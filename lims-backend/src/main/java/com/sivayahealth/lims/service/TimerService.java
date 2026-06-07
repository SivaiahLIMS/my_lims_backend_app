package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.worksheet.TimerActionRequest;
import com.sivayahealth.lims.dto.worksheet.TimerStartRequest;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Server-side timer persistence for long-running test cases.
 * The timer state (start/pause/stop) is stored in worksheet_timer_log so that
 * elapsed time survives browser refreshes, overnight runs, or days-long stability tests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimerService {

    private final WorksheetTimerLogRepository timerRepo;
    private final WorksheetMasterRepository   worksheetRepo;
    private final DocumentTestCaseRepository  testCaseRepo;
    private final DocumentSlotGroupRepository slotGroupRepo;
    private final AppUserRepository           userRepo;

    @Transactional
    public WorksheetTimerLog start(Long tenantId, Long actorId, TimerStartRequest req) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, req.getWorksheetId());
        DocumentTestCase testCase = testCaseRepo.findById(req.getTestCaseId())
                .orElseThrow(() -> LimsException.notFound("TestCase not found"));
        AppUser actor = loadUser(actorId);

        // Prevent duplicate running timers for the same slot group
        if (req.getSlotGroupId() != null) {
            timerRepo.findByWorksheet_WorksheetIdAndSlotGroup_SlotGroupIdAndStatus(
                    worksheet.getWorksheetId(), req.getSlotGroupId(), "RUNNING")
                    .ifPresent(t -> { throw LimsException.badRequest("Timer already running for this slot group"); });
        }

        WorksheetTimerLog log = WorksheetTimerLog.builder()
                .worksheet(worksheet)
                .testCase(testCase)
                .timerId(req.getTimerId())
                .startedBy(actor)
                .startedAt(LocalDateTime.now())
                .elapsedMs(0L)
                .status("RUNNING")
                .build();

        if (req.getSlotGroupId() != null) {
            log.setSlotGroup(slotGroupRepo.findById(req.getSlotGroupId())
                    .orElseThrow(() -> LimsException.notFound("SlotGroup not found")));
        }

        return timerRepo.save(log);
    }

    @Transactional
    public WorksheetTimerLog act(Long tenantId, Long actorId, TimerActionRequest req) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, req.getWorksheetId());
        AppUser actor = loadUser(actorId);

        WorksheetTimerLog log = timerRepo
                .findByWorksheet_WorksheetIdAndTimerIdAndStatusNot(
                        worksheet.getWorksheetId(), req.getTimerId(), "STOPPED")
                .orElseThrow(() -> LimsException.notFound("Active timer not found: " + req.getTimerId()));

        LocalDateTime now = LocalDateTime.now();

        switch (req.getAction()) {
            case "STOP" -> {
                long extra = computeExtraMs(log, now);
                log.setElapsedMs(log.getElapsedMs() + extra);
                log.setStoppedBy(actor);
                log.setStoppedAt(now);
                log.setStatus("STOPPED");
            }
            case "PAUSE" -> {
                if (!"RUNNING".equals(log.getStatus())) {
                    throw LimsException.badRequest("Timer is not running");
                }
                long extra = computeExtraMs(log, now);
                log.setElapsedMs(log.getElapsedMs() + extra);
                log.setPausedAt(now);
                log.setStatus("PAUSED");
            }
            case "RESUME" -> {
                if (!"PAUSED".equals(log.getStatus())) {
                    throw LimsException.badRequest("Timer is not paused");
                }
                log.setResumedAt(now);
                log.setStartedAt(now);   // reset anchor for next delta calculation
                log.setStatus("RUNNING");
            }
            default -> throw LimsException.badRequest("Unknown timer action: " + req.getAction());
        }

        return timerRepo.save(log);
    }

    @Transactional(readOnly = true)
    public WorksheetTimerLog getState(Long tenantId, Long worksheetId, String timerId) {
        loadWorksheet(tenantId, worksheetId);
        return timerRepo
                .findByWorksheet_WorksheetIdAndTimerIdAndStatusNot(worksheetId, timerId, "STOPPED")
                .orElseThrow(() -> LimsException.notFound("Timer not found: " + timerId));
    }

    /** Milliseconds elapsed since the timer was last started (or resumed). */
    private long computeExtraMs(WorksheetTimerLog log, LocalDateTime now) {
        if (log.getStartedAt() == null) return 0L;
        return Duration.between(log.getStartedAt(), now).toMillis();
    }

    private WorksheetMaster loadWorksheet(Long tenantId, Long worksheetId) {
        WorksheetMaster w = worksheetRepo.findById(worksheetId)
                .orElseThrow(() -> LimsException.notFound("Worksheet not found"));
        if (!w.getTenant().getId().equals(tenantId)) {
            throw LimsException.forbidden("Access denied");
        }
        return w;
    }

    private AppUser loadUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found: " + userId));
    }
}
