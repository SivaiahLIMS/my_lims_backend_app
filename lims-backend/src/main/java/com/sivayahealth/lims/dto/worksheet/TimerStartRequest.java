package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Starts a timer for a slot group inside an executing worksheet. */
@Data
public class TimerStartRequest {
    private Long worksheetId;
    private Long testCaseId;
    private Long slotGroupId;
    /** Client-generated stable ID used to locate this timer on subsequent stop/pause calls */
    private String timerId;
}
