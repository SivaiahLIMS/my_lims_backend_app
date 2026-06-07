package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

/** Upsert values for one slot group during worksheet execution. */
@Data
public class SaveSlotGroupValueRequest {
    private Long worksheetId;
    private Long slotGroupId;
    private Long testCaseId;
    private String textValue;
    private Long instrumentId;
    private Long chemicalLotId;
    private String elnRef;
    private Long elapsedMs;
}
