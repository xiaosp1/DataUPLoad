/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.DefectCountDTO
 *  com.hikrobotics.solution.module.line.dto.RealTimeDetectData
 *  jakarta.validation.Valid
 *  jakarta.validation.constraints.Min
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 */
package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Valid
public class RealTimeDetectData {
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Integer total;
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Integer ngCount;
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Integer removeTotal;
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Integer removeFail;
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Double efficiency;
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Double totalNgRate;
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Integer occupancy;
    @NotNull
    @Min(value=0L)
    private @NotNull @Min(value=0L) Double occupancyRate;
    @NotBlank
    private String startTime;
    private List<DefectCountDTO> defects;

    public Integer getTotal() {
        return this.total;
    }

    public Integer getNgCount() {
        return this.ngCount;
    }

    public Integer getRemoveTotal() {
        return this.removeTotal;
    }

    public Integer getRemoveFail() {
        return this.removeFail;
    }

    public Double getEfficiency() {
        return this.efficiency;
    }

    public Double getTotalNgRate() {
        return this.totalNgRate;
    }

    public Integer getOccupancy() {
        return this.occupancy;
    }

    public Double getOccupancyRate() {
        return this.occupancyRate;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public List<DefectCountDTO> getDefects() {
        return this.defects;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public void setNgCount(Integer ngCount) {
        this.ngCount = ngCount;
    }

    public void setRemoveTotal(Integer removeTotal) {
        this.removeTotal = removeTotal;
    }

    public void setRemoveFail(Integer removeFail) {
        this.removeFail = removeFail;
    }

    public void setEfficiency(Double efficiency) {
        this.efficiency = efficiency;
    }

    public void setTotalNgRate(Double totalNgRate) {
        this.totalNgRate = totalNgRate;
    }

    public void setOccupancy(Integer occupancy) {
        this.occupancy = occupancy;
    }

    public void setOccupancyRate(Double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setDefects(List<DefectCountDTO> defects) {
        this.defects = defects;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof RealTimeDetectData)) {
            return false;
        }
        RealTimeDetectData other = (RealTimeDetectData)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$total = this.getTotal();
        Integer other$total = other.getTotal();
        if (this$total == null ? other$total != null : !((Object)this$total).equals(other$total)) {
            return false;
        }
        Integer this$ngCount = this.getNgCount();
        Integer other$ngCount = other.getNgCount();
        if (this$ngCount == null ? other$ngCount != null : !((Object)this$ngCount).equals(other$ngCount)) {
            return false;
        }
        Integer this$removeTotal = this.getRemoveTotal();
        Integer other$removeTotal = other.getRemoveTotal();
        if (this$removeTotal == null ? other$removeTotal != null : !((Object)this$removeTotal).equals(other$removeTotal)) {
            return false;
        }
        Integer this$removeFail = this.getRemoveFail();
        Integer other$removeFail = other.getRemoveFail();
        if (this$removeFail == null ? other$removeFail != null : !((Object)this$removeFail).equals(other$removeFail)) {
            return false;
        }
        Double this$efficiency = this.getEfficiency();
        Double other$efficiency = other.getEfficiency();
        if (this$efficiency == null ? other$efficiency != null : !((Object)this$efficiency).equals(other$efficiency)) {
            return false;
        }
        Double this$totalNgRate = this.getTotalNgRate();
        Double other$totalNgRate = other.getTotalNgRate();
        if (this$totalNgRate == null ? other$totalNgRate != null : !((Object)this$totalNgRate).equals(other$totalNgRate)) {
            return false;
        }
        Integer this$occupancy = this.getOccupancy();
        Integer other$occupancy = other.getOccupancy();
        if (this$occupancy == null ? other$occupancy != null : !((Object)this$occupancy).equals(other$occupancy)) {
            return false;
        }
        Double this$occupancyRate = this.getOccupancyRate();
        Double other$occupancyRate = other.getOccupancyRate();
        if (this$occupancyRate == null ? other$occupancyRate != null : !((Object)this$occupancyRate).equals(other$occupancyRate)) {
            return false;
        }
        String this$startTime = this.getStartTime();
        String other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime)) {
            return false;
        }
        List this$defects = this.getDefects();
        List other$defects = other.getDefects();
        return !(this$defects == null ? other$defects != null : !((Object)this$defects).equals(other$defects));
    }

    protected boolean canEqual(Object other) {
        return other instanceof RealTimeDetectData;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $total = this.getTotal();
        result = result * 59 + ($total == null ? 43 : ((Object)$total).hashCode());
        Integer $ngCount = this.getNgCount();
        result = result * 59 + ($ngCount == null ? 43 : ((Object)$ngCount).hashCode());
        Integer $removeTotal = this.getRemoveTotal();
        result = result * 59 + ($removeTotal == null ? 43 : ((Object)$removeTotal).hashCode());
        Integer $removeFail = this.getRemoveFail();
        result = result * 59 + ($removeFail == null ? 43 : ((Object)$removeFail).hashCode());
        Double $efficiency = this.getEfficiency();
        result = result * 59 + ($efficiency == null ? 43 : ((Object)$efficiency).hashCode());
        Double $totalNgRate = this.getTotalNgRate();
        result = result * 59 + ($totalNgRate == null ? 43 : ((Object)$totalNgRate).hashCode());
        Integer $occupancy = this.getOccupancy();
        result = result * 59 + ($occupancy == null ? 43 : ((Object)$occupancy).hashCode());
        Double $occupancyRate = this.getOccupancyRate();
        result = result * 59 + ($occupancyRate == null ? 43 : ((Object)$occupancyRate).hashCode());
        String $startTime = this.getStartTime();
        result = result * 59 + ($startTime == null ? 43 : $startTime.hashCode());
        List $defects = this.getDefects();
        result = result * 59 + ($defects == null ? 43 : ((Object)$defects).hashCode());
        return result;
    }

    public String toString() {
        return "RealTimeDetectData(total=" + this.getTotal() + ", ngCount=" + this.getNgCount() + ", removeTotal=" + this.getRemoveTotal() + ", removeFail=" + this.getRemoveFail() + ", efficiency=" + this.getEfficiency() + ", totalNgRate=" + this.getTotalNgRate() + ", occupancy=" + this.getOccupancy() + ", occupancyRate=" + this.getOccupancyRate() + ", startTime=" + this.getStartTime() + ", defects=" + this.getDefects() + ")";
    }
}

