package com.hikrobotics.solution.module.line.dto;

import jakarta.validation.constraints.NotBlank;

public class ClientPlanQueryDTO {
    @NotBlank
    private String lineNo;
    @NotBlank
    private String faceNo;

    public String getLineNo() {
        return this.lineNo;
    }

    public String getFaceNo() {
        return this.faceNo;
    }

    public void setLineNo(String lineNo) {
        this.lineNo = lineNo;
    }

    public void setFaceNo(String faceNo) {
        this.faceNo = faceNo;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ClientPlanQueryDTO)) {
            return false;
        }
        ClientPlanQueryDTO other = (ClientPlanQueryDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$lineNo = this.getLineNo();
        String other$lineNo = other.getLineNo();
        if (this$lineNo == null ? other$lineNo != null : !this$lineNo.equals(other$lineNo)) {
            return false;
        }
        String this$faceNo = this.getFaceNo();
        String other$faceNo = other.getFaceNo();
        return !(this$faceNo == null ? other$faceNo != null : !this$faceNo.equals(other$faceNo));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ClientPlanQueryDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        String $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        return result;
    }

    public String toString() {
        return "ClientPlanQueryDTO(lineNo=" + this.getLineNo() + ", faceNo=" + this.getFaceNo() + ")";
    }
}

