package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Stops or pauses a running timer. */
@Data
public class TimerActionRequest {
    private Long worksheetId;
    private String timerId;
    /** STOP | PAUSE | RESUME */
    private String action;
}
