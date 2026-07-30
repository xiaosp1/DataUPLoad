/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.framework.common.query.TimePageQuery
 *  com.hikrobotics.solution.module.line.dto.DefectQueryDTO
 */
package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.framework.common.query.TimePageQuery;
import java.time.LocalDateTime;

public class DefectQueryDTO
extends TimePageQuery {
    private String lineNo;
    private Integer result;
    private String defectType;
    private String faceNo;
    private LocalDateTime lStartTime;
    private LocalDateTime lEndTime;

    public String getLineNo() {
        return this.lineNo;
    }

    public Integer getResult() {
        return this.result;
    }

    public String getDefectType() {
        return this.defectType;
    }

    public String getFaceNo() {
        return this.faceNo;
    }

    public LocalDateTime getLStartTime() {
        return this.lStartTime;
    }

    public LocalDateTime getLEndTime() {
        return this.lEndTime;
    }

    public void setLineNo(String lineNo) {
        this.lineNo = lineNo;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public void setDefectType(String defectType) {
        this.defectType = defectType;
    }

    public void setFaceNo(String faceNo) {
        this.faceNo = faceNo;
    }

    public void setLStartTime(LocalDateTime lStartTime) {
        this.lStartTime = lStartTime;
    }

    public void setLEndTime(LocalDateTime lEndTime) {
        this.lEndTime = lEndTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DefectQueryDTO)) {
            return false;
        }
        DefectQueryDTO other = (DefectQueryDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$result = this.getResult();
        Integer other$result = other.getResult();
        if (this$result == null ? other$result != null : !((Object)this$result).equals(other$result)) {
            return false;
        }
        String this$lineNo = this.getLineNo();
        String other$lineNo = other.getLineNo();
        if (this$lineNo == null ? other$lineNo != null : !this$lineNo.equals(other$lineNo)) {
            return false;
        }
        String this$defectType = this.getDefectType();
        String other$defectType = other.getDefectType();
        if (this$defectType == null ? other$defectType != null : !this$defectType.equals(other$defectType)) {
            return false;
        }
        String this$faceNo = this.getFaceNo();
        String other$faceNo = other.getFaceNo();
        if (this$faceNo == null ? other$faceNo != null : !this$faceNo.equals(other$faceNo)) {
            return false;
        }
        LocalDateTime this$lStartTime = this.getLStartTime();
        LocalDateTime other$lStartTime = other.getLStartTime();
        if (this$lStartTime == null ? other$lStartTime != null : !((Object)this$lStartTime).equals(other$lStartTime)) {
            return false;
        }
        LocalDateTime this$lEndTime = this.getLEndTime();
        LocalDateTime other$lEndTime = other.getLEndTime();
        return !(this$lEndTime == null ? other$lEndTime != null : !((Object)this$lEndTime).equals(other$lEndTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DefectQueryDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $result = this.getResult();
        result = result * 59 + ($result == null ? 43 : ((Object)$result).hashCode());
        String $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        String $defectType = this.getDefectType();
        result = result * 59 + ($defectType == null ? 43 : $defectType.hashCode());
        String $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        LocalDateTime $lStartTime = this.getLStartTime();
        result = result * 59 + ($lStartTime == null ? 43 : ((Object)$lStartTime).hashCode());
        LocalDateTime $lEndTime = this.getLEndTime();
        result = result * 59 + ($lEndTime == null ? 43 : ((Object)$lEndTime).hashCode());
        return result;
    }

    public String toString() {
        return "DefectQueryDTO(lineNo=" + this.getLineNo() + ", result=" + this.getResult() + ", defectType=" + this.getDefectType() + ", faceNo=" + this.getFaceNo() + ", lStartTime=" + this.getLStartTime() + ", lEndTime=" + this.getLEndTime() + ")";
    }
}

