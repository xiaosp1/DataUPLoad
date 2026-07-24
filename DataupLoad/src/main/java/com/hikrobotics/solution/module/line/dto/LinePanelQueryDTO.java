package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.framework.common.query.TimePageQuery;
import jakarta.validation.constraints.NotNull;

public class LinePanelQueryDTO
extends TimePageQuery {
    @NotNull
    private Integer faceId;

    public Integer getFaceId() {
        return this.faceId;
    }

    public void setFaceId(Integer faceId) {
        this.faceId = faceId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LinePanelQueryDTO)) {
            return false;
        }
        LinePanelQueryDTO other = (LinePanelQueryDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$faceId = this.getFaceId();
        Integer other$faceId = other.getFaceId();
        return !(this$faceId == null ? other$faceId != null : !((Object)this$faceId).equals(other$faceId));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LinePanelQueryDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $faceId = this.getFaceId();
        result = result * 59 + ($faceId == null ? 43 : ((Object)$faceId).hashCode());
        return result;
    }

    public String toString() {
        return "LinePanelQueryDTO(faceId=" + this.getFaceId() + ")";
    }
}

