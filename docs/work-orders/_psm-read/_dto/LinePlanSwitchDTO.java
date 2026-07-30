/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO
 *  jakarta.validation.constraints.NotEmpty
 *  jakarta.validation.constraints.NotNull
 */
package com.hikrobotics.solution.module.line.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class LinePlanSwitchDTO {
    @NotEmpty
    private String clientNo;
    @NotNull
    private Integer lineId;
    @NotNull
    private Integer planId;

    public String getClientNo() {
        return this.clientNo;
    }

    public Integer getLineId() {
        return this.lineId;
    }

    public Integer getPlanId() {
        return this.planId;
    }

    public void setClientNo(String clientNo) {
        this.clientNo = clientNo;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LinePlanSwitchDTO)) {
            return false;
        }
        LinePlanSwitchDTO other = (LinePlanSwitchDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$lineId = this.getLineId();
        Integer other$lineId = other.getLineId();
        if (this$lineId == null ? other$lineId != null : !((Object)this$lineId).equals(other$lineId)) {
            return false;
        }
        Integer this$planId = this.getPlanId();
        Integer other$planId = other.getPlanId();
        if (this$planId == null ? other$planId != null : !((Object)this$planId).equals(other$planId)) {
            return false;
        }
        String this$clientNo = this.getClientNo();
        String other$clientNo = other.getClientNo();
        return !(this$clientNo == null ? other$clientNo != null : !this$clientNo.equals(other$clientNo));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LinePlanSwitchDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : ((Object)$lineId).hashCode());
        Integer $planId = this.getPlanId();
        result = result * 59 + ($planId == null ? 43 : ((Object)$planId).hashCode());
        String $clientNo = this.getClientNo();
        result = result * 59 + ($clientNo == null ? 43 : $clientNo.hashCode());
        return result;
    }

    public String toString() {
        return "LinePlanSwitchDTO(clientNo=" + this.getClientNo() + ", lineId=" + this.getLineId() + ", planId=" + this.getPlanId() + ")";
    }
}

