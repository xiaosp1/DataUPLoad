/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.LinePlanBindQueryDTO
 *  jakarta.validation.constraints.NotNull
 */
package com.hikrobotics.solution.module.line.dto;

import jakarta.validation.constraints.NotNull;

public class LinePlanBindQueryDTO {
    @NotNull
    public Integer lineId;

    public Integer getLineId() {
        return this.lineId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LinePlanBindQueryDTO)) {
            return false;
        }
        LinePlanBindQueryDTO other = (LinePlanBindQueryDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$lineId = this.getLineId();
        Integer other$lineId = other.getLineId();
        return !(this$lineId == null ? other$lineId != null : !((Object)this$lineId).equals(other$lineId));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LinePlanBindQueryDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : ((Object)$lineId).hashCode());
        return result;
    }

    public String toString() {
        return "LinePlanBindQueryDTO(lineId=" + this.getLineId() + ")";
    }
}

