package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;
import java.util.List;

/**
 * Analyst execution view — a flattened, frontend-ready structure derived from
 * templateJson. Each item is one fill-in field with metadata.
 */
@Data
public class WorksheetExecutionView {
    private Long templateId;
    private String templateName;
    private String status;
    private Integer version;
    private List<ExecutionSection> sections;

    @Data
    public static class ExecutionSection {
        private String sectionName;
        private List<ExecutionGroup> slotGroups;
    }

    @Data
    public static class ExecutionGroup {
        private String groupName;
        private List<ExecutionSlot> slots;
    }

    @Data
    public static class ExecutionSlot {
        private String slotKey;
        private String label;
        private String type;   // TEXT | NUMBER | DROPDOWN | DATETIME | BOOLEAN
        private String unit;
        private Boolean required;
        private List<String> options;
        private String currentValue; // filled from WorksheetSlotGroupValue when available
    }
}
