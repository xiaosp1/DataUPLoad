/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.framework.util.HikDateUtil
 *  com.hikrobotics.solution.module.line.dto.DefectCountDTO
 *  com.hikrobotics.solution.module.line.dto.TodayDetectDataDTO
 *  jakarta.validation.Valid
 *  jakarta.validation.constraints.Min
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 */
package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Valid
public class TodayDetectDataDTO {
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Integer totalNum;
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Integer ngNum;
    @NotBlank
    private String statisticTime;
    private List<DefectCountDTO> defects;

    public LocalDateTime getStatisticTime() {
        return HikDateUtil.transformTime((String)this.statisticTime);
    }

    public Integer getTotalNum() {
        return this.totalNum;
    }

    public Integer getNgNum() {
        return this.ngNum;
    }

    public List<DefectCountDTO> getDefects() {
        return this.defects;
    }

    public void setTotalNum(Integer totalNum) {
        this.totalNum = totalNum;
    }

    public void setNgNum(Integer ngNum) {
        this.ngNum = ngNum;
    }

    public void setStatisticTime(String statisticTime) {
        this.statisticTime = statisticTime;
    }

    public void setDefects(List<DefectCountDTO> defects) {
        this.defects = defects;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TodayDetectDataDTO)) {
            return false;
        }
        TodayDetectDataDTO other = (TodayDetectDataDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$totalNum = this.getTotalNum();
        Integer other$totalNum = other.getTotalNum();
        if (this$totalNum == null ? other$totalNum != null : !((Object)this$totalNum).equals(other$totalNum)) {
            return false;
        }
        Integer this$ngNum = this.getNgNum();
        Integer other$ngNum = other.getNgNum();
        if (this$ngNum == null ? other$ngNum != null : !((Object)this$ngNum).equals(other$ngNum)) {
            return false;
        }
        LocalDateTime this$statisticTime = this.getStatisticTime();
        LocalDateTime other$statisticTime = other.getStatisticTime();
        if (this$statisticTime == null ? other$statisticTime != null : !((Object)this$statisticTime).equals(other$statisticTime)) {
            return false;
        }
        List this$defects = this.getDefects();
        List other$defects = other.getDefects();
        return !(this$defects == null ? other$defects != null : !((Object)this$defects).equals(other$defects));
    }

    protected boolean canEqual(Object other) {
        return other instanceof TodayDetectDataDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $totalNum = this.getTotalNum();
        result = result * 59 + ($totalNum == null ? 43 : ((Object)$totalNum).hashCode());
        Integer $ngNum = this.getNgNum();
        result = result * 59 + ($ngNum == null ? 43 : ((Object)$ngNum).hashCode());
        LocalDateTime $statisticTime = this.getStatisticTime();
        result = result * 59 + ($statisticTime == null ? 43 : ((Object)$statisticTime).hashCode());
        List $defects = this.getDefects();
        result = result * 59 + ($defects == null ? 43 : ((Object)$defects).hashCode());
        return result;
    }

    public String toString() {
        return "TodayDetectDataDTO(totalNum=" + this.getTotalNum() + ", ngNum=" + this.getNgNum() + ", statisticTime=" + this.getStatisticTime() + ", defects=" + this.getDefects() + ")";
    }
}

