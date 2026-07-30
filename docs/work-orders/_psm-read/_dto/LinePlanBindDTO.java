/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.LinePlanBindDTO
 *  jakarta.validation.constraints.NotEmpty
 *  jakarta.validation.constraints.NotNull
 */
package com.hikrobotics.solution.module.line.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class LinePlanBindDTO {
    @NotEmpty
    private String clientNo;
    @NotNull
    private Integer lineId;
    @NotEmpty
    private List<Integer> planIds;

    public String getClientNo() {
        return this.clientNo;
    }

    public Integer getLineId() {
        return this.lineId;
    }

    public List<Integer> getPlanIds() {
        return this.planIds;
    }

    public void setClientNo(String clientNo) {
        this.clientNo = clientNo;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public void setPlanIds(List<Integer> planIds) {
        this.planIds = planIds;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LinePlanBindDTO)) {
            return false;
        }
        LinePlanBindDTO other = (LinePlanBindDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$lineId = this.getLineId();
        Integer other$lineId = other.getLineId();
        if (this$lineId == null ? other$lineId != null : !((Object)this$lineId).equals(other$lineId)) {
            return false;
        }
        String this$clientNo = this.getClientNo();
        String other$clientNo = other.getClientNo();
        if (this$clientNo == null ? other$clientNo != null : !this$clientNo.equals(other$clientNo)) {
            return false;
        }
        List this$planIds = this.getPlanIds();
        List other$planIds = other.getPlanIds();
        return !(this$planIds == null ? other$planIds != null : !((Object)this$planIds).equals(other$planIds));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LinePlanBindDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : ((Object)$lineId).hashCode());
        String $clientNo = this.getClientNo();
        result = result * 59 + ($clientNo == null ? 43 : $clientNo.hashCode());
        List $planIds = this.getPlanIds();
        result = result * 59 + ($planIds == null ? 43 : ((Object)$planIds).hashCode());
        return result;
    }

    public String toString() {
        return "LinePlanBindDTO(clientNo=" + this.getClientNo() + ", lineId=" + this.getLineId() + ", planIds=" + this.getPlanIds() + ")";
    }
}

