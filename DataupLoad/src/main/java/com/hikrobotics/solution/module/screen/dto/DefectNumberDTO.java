/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.detect.entity.DefectDayRecord
 *  com.hikrobotics.solution.module.screen.dto.DefectNumberDTO
 */
package com.hikrobotics.solution.module.screen.dto;

import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;

public class DefectNumberDTO {
    private String defectName;
    private Integer defectCount;
    private Integer countThreshold;

    public DefectNumberDTO(DefectDayRecord po) {
        this.defectCount = po.getCount();
        this.defectName = po.getType();
    }

    public String getDefectName() {
        return this.defectName;
    }

    public Integer getDefectCount() {
        return this.defectCount;
    }

    public Integer getCountThreshold() {
        return this.countThreshold;
    }

    public DefectNumberDTO setDefectName(String defectName) {
        this.defectName = defectName;
        return this;
    }

    public DefectNumberDTO setDefectCount(Integer defectCount) {
        this.defectCount = defectCount;
        return this;
    }

    public DefectNumberDTO setCountThreshold(Integer countThreshold) {
        this.countThreshold = countThreshold;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DefectNumberDTO)) {
            return false;
        }
        DefectNumberDTO other = (DefectNumberDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$defectCount = this.getDefectCount();
        Integer other$defectCount = other.getDefectCount();
        if (this$defectCount == null ? other$defectCount != null : !((Object)this$defectCount).equals(other$defectCount)) {
            return false;
        }
        Integer this$countThreshold = this.getCountThreshold();
        Integer other$countThreshold = other.getCountThreshold();
        if (this$countThreshold == null ? other$countThreshold != null : !((Object)this$countThreshold).equals(other$countThreshold)) {
            return false;
        }
        String this$defectName = this.getDefectName();
        String other$defectName = other.getDefectName();
        return !(this$defectName == null ? other$defectName != null : !this$defectName.equals(other$defectName));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DefectNumberDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $defectCount = this.getDefectCount();
        result = result * 59 + ($defectCount == null ? 43 : ((Object)$defectCount).hashCode());
        Integer $countThreshold = this.getCountThreshold();
        result = result * 59 + ($countThreshold == null ? 43 : ((Object)$countThreshold).hashCode());
        String $defectName = this.getDefectName();
        result = result * 59 + ($defectName == null ? 43 : $defectName.hashCode());
        return result;
    }

    public String toString() {
        return "DefectNumberDTO(defectName=" + this.getDefectName() + ", defectCount=" + this.getDefectCount() + ", countThreshold=" + this.getCountThreshold() + ")";
    }

    public DefectNumberDTO() {
    }
}
