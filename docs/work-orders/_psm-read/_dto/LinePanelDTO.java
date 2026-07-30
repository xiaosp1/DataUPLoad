/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.alarm.dto.AlarmCountDTO
 *  com.hikrobotics.solution.module.detect.model.StatusRecordPO
 *  com.hikrobotics.solution.module.line.dto.DefectCountDisPlayDTO
 *  com.hikrobotics.solution.module.line.dto.LineCountDTO
 *  com.hikrobotics.solution.module.line.dto.LinePanelDTO
 *  com.hikrobotics.solution.module.line.dto.ToDayCountDTO
 *  org.assertj.core.util.Lists
 */
package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.module.alarm.dto.AlarmCountDTO;
import com.hikrobotics.solution.module.detect.model.StatusRecordPO;
import com.hikrobotics.solution.module.line.dto.DefectCountDisPlayDTO;
import com.hikrobotics.solution.module.line.dto.LineCountDTO;
import com.hikrobotics.solution.module.line.dto.ToDayCountDTO;
import java.util.List;
import org.assertj.core.util.Lists;

public class LinePanelDTO {
    private List<LineCountDTO> lineCountDTOS;
    private List<DefectCountDisPlayDTO> defectCountDTOS = Lists.newArrayList();
    private List<AlarmCountDTO> alarmCountDTOS;
    private List<StatusRecordPO> statusRecordPOS;
    private ToDayCountDTO toDayCountDTO;

    public List<LineCountDTO> getLineCountDTOS() {
        return this.lineCountDTOS;
    }

    public List<DefectCountDisPlayDTO> getDefectCountDTOS() {
        return this.defectCountDTOS;
    }

    public List<AlarmCountDTO> getAlarmCountDTOS() {
        return this.alarmCountDTOS;
    }

    public List<StatusRecordPO> getStatusRecordPOS() {
        return this.statusRecordPOS;
    }

    public ToDayCountDTO getToDayCountDTO() {
        return this.toDayCountDTO;
    }

    public LinePanelDTO setLineCountDTOS(List<LineCountDTO> lineCountDTOS) {
        this.lineCountDTOS = lineCountDTOS;
        return this;
    }

    public LinePanelDTO setDefectCountDTOS(List<DefectCountDisPlayDTO> defectCountDTOS) {
        this.defectCountDTOS = defectCountDTOS;
        return this;
    }

    public LinePanelDTO setAlarmCountDTOS(List<AlarmCountDTO> alarmCountDTOS) {
        this.alarmCountDTOS = alarmCountDTOS;
        return this;
    }

    public LinePanelDTO setStatusRecordPOS(List<StatusRecordPO> statusRecordPOS) {
        this.statusRecordPOS = statusRecordPOS;
        return this;
    }

    public LinePanelDTO setToDayCountDTO(ToDayCountDTO toDayCountDTO) {
        this.toDayCountDTO = toDayCountDTO;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LinePanelDTO)) {
            return false;
        }
        LinePanelDTO other = (LinePanelDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        List this$lineCountDTOS = this.getLineCountDTOS();
        List other$lineCountDTOS = other.getLineCountDTOS();
        if (this$lineCountDTOS == null ? other$lineCountDTOS != null : !((Object)this$lineCountDTOS).equals(other$lineCountDTOS)) {
            return false;
        }
        List this$defectCountDTOS = this.getDefectCountDTOS();
        List other$defectCountDTOS = other.getDefectCountDTOS();
        if (this$defectCountDTOS == null ? other$defectCountDTOS != null : !((Object)this$defectCountDTOS).equals(other$defectCountDTOS)) {
            return false;
        }
        List this$alarmCountDTOS = this.getAlarmCountDTOS();
        List other$alarmCountDTOS = other.getAlarmCountDTOS();
        if (this$alarmCountDTOS == null ? other$alarmCountDTOS != null : !((Object)this$alarmCountDTOS).equals(other$alarmCountDTOS)) {
            return false;
        }
        List this$statusRecordPOS = this.getStatusRecordPOS();
        List other$statusRecordPOS = other.getStatusRecordPOS();
        if (this$statusRecordPOS == null ? other$statusRecordPOS != null : !((Object)this$statusRecordPOS).equals(other$statusRecordPOS)) {
            return false;
        }
        ToDayCountDTO this$toDayCountDTO = this.getToDayCountDTO();
        ToDayCountDTO other$toDayCountDTO = other.getToDayCountDTO();
        return !(this$toDayCountDTO == null ? other$toDayCountDTO != null : !this$toDayCountDTO.equals(other$toDayCountDTO));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LinePanelDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List $lineCountDTOS = this.getLineCountDTOS();
        result = result * 59 + ($lineCountDTOS == null ? 43 : ((Object)$lineCountDTOS).hashCode());
        List $defectCountDTOS = this.getDefectCountDTOS();
        result = result * 59 + ($defectCountDTOS == null ? 43 : ((Object)$defectCountDTOS).hashCode());
        List $alarmCountDTOS = this.getAlarmCountDTOS();
        result = result * 59 + ($alarmCountDTOS == null ? 43 : ((Object)$alarmCountDTOS).hashCode());
        List $statusRecordPOS = this.getStatusRecordPOS();
        result = result * 59 + ($statusRecordPOS == null ? 43 : ((Object)$statusRecordPOS).hashCode());
        ToDayCountDTO $toDayCountDTO = this.getToDayCountDTO();
        result = result * 59 + ($toDayCountDTO == null ? 43 : $toDayCountDTO.hashCode());
        return result;
    }

    public String toString() {
        return "LinePanelDTO(lineCountDTOS=" + this.getLineCountDTOS() + ", defectCountDTOS=" + this.getDefectCountDTOS() + ", alarmCountDTOS=" + this.getAlarmCountDTOS() + ", statusRecordPOS=" + this.getStatusRecordPOS() + ", toDayCountDTO=" + this.getToDayCountDTO() + ")";
    }

    public LinePanelDTO() {
    }

    public LinePanelDTO(List<LineCountDTO> lineCountDTOS, List<DefectCountDisPlayDTO> defectCountDTOS, List<AlarmCountDTO> alarmCountDTOS, List<StatusRecordPO> statusRecordPOS, ToDayCountDTO toDayCountDTO) {
        this.lineCountDTOS = lineCountDTOS;
        this.defectCountDTOS = defectCountDTOS;
        this.alarmCountDTOS = alarmCountDTOS;
        this.statusRecordPOS = statusRecordPOS;
        this.toDayCountDTO = toDayCountDTO;
    }
}

