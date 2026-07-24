package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.framework.util.HikDateUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Valid
public class TodayDetectDataDTO {
    @NotNull
    @Min(0L)
    private Integer totalNum;
    @NotNull
    @Min(0L)
    private Integer ngNum;
    @NotBlank
    private String statisticTime;
    private List<DefectCountDTO> defects;

    public LocalDateTime getStatisticTime() {
        return HikDateUtil.transformTime(this.statisticTime);
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

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof TodayDetectDataDTO other)) {
            return false;
        } else {
            if (!other.canEqual(this)) {
                return false;
            }
            Object this$totalNum = this.getTotalNum();
            Object other$totalNum = other.getTotalNum();
            if (this$totalNum == null ? other$totalNum == null : this$totalNum.equals(other$totalNum)) {
                Object this$ngNum = this.getNgNum();
                Object other$ngNum = other.getNgNum();
                if (this$ngNum == null ? other$ngNum == null : this$ngNum.equals(other$ngNum)) {
                    Object this$statisticTime = this.getStatisticTime();
                    Object other$statisticTime = other.getStatisticTime();
                    if (this$statisticTime == null ? other$statisticTime == null : this$statisticTime.equals(other$statisticTime)) {
                        Object this$defects = this.getDefects();
                        Object other$defects = other.getDefects();
                        return this$defects == null ? other$defects == null : this$defects.equals(other$defects);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof TodayDetectDataDTO;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $totalNum = this.getTotalNum();
        result = result * 59 + ($totalNum == null ? 43 : $totalNum.hashCode());
        Object $ngNum = this.getNgNum();
        result = result * 59 + ($ngNum == null ? 43 : $ngNum.hashCode());
        Object $statisticTime = this.getStatisticTime();
        result = result * 59 + ($statisticTime == null ? 43 : $statisticTime.hashCode());
        Object $defects = this.getDefects();
        return result * 59 + ($defects == null ? 43 : $defects.hashCode());
    }

    @Override
    public String toString() {
        return "TodayDetectDataDTO(totalNum="
            + this.getTotalNum()
            + ", ngNum="
            + this.getNgNum()
            + ", statisticTime="
            + this.getStatisticTime()
            + ", defects="
            + this.getDefects()
            + ")";
    }
}
