package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.module.alarm.dto.AlarmCountDTO;
import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import java.util.ArrayList;
import java.util.List;

/**
 * PSM 1:1 LinePanelDTO — 大屏面板聚合 DTO。
 * StatusRecordPO → StatusRecord（DataupLoad entity 映射）。
 */
public class LinePanelDTO {
    private List<LineCountDTO> lineCountDTOS;
    private List<DefectCountDisPlayDTO> defectCountDTOS = new ArrayList<>();
    private List<AlarmCountDTO> alarmCountDTOS;
    private List<StatusRecord> statusRecordPOS;
    private ToDayCountDTO toDayCountDTO;

    public List<LineCountDTO> getLineCountDTOS() { return lineCountDTOS; }
    public LinePanelDTO setLineCountDTOS(List<LineCountDTO> lineCountDTOS) { this.lineCountDTOS = lineCountDTOS; return this; }

    public List<DefectCountDisPlayDTO> getDefectCountDTOS() { return defectCountDTOS; }
    public LinePanelDTO setDefectCountDTOS(List<DefectCountDisPlayDTO> defectCountDTOS) { this.defectCountDTOS = defectCountDTOS; return this; }

    public List<AlarmCountDTO> getAlarmCountDTOS() { return alarmCountDTOS; }
    public LinePanelDTO setAlarmCountDTOS(List<AlarmCountDTO> alarmCountDTOS) { this.alarmCountDTOS = alarmCountDTOS; return this; }

    public List<StatusRecord> getStatusRecordPOS() { return statusRecordPOS; }
    public LinePanelDTO setStatusRecordPOS(List<StatusRecord> statusRecordPOS) { this.statusRecordPOS = statusRecordPOS; return this; }

    public ToDayCountDTO getToDayCountDTO() { return toDayCountDTO; }
    public LinePanelDTO setToDayCountDTO(ToDayCountDTO toDayCountDTO) { this.toDayCountDTO = toDayCountDTO; return this; }

    public LinePanelDTO() {}

    public LinePanelDTO(List<LineCountDTO> lineCountDTOS, List<DefectCountDisPlayDTO> defectCountDTOS,
                        List<AlarmCountDTO> alarmCountDTOS, List<StatusRecord> statusRecordPOS,
                        ToDayCountDTO toDayCountDTO) {
        this.lineCountDTOS = lineCountDTOS;
        this.defectCountDTOS = defectCountDTOS;
        this.alarmCountDTOS = alarmCountDTOS;
        this.statusRecordPOS = statusRecordPOS;
        this.toDayCountDTO = toDayCountDTO;
    }
}
