package com.hikrobotics.solution.module.line.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Valid
public class RealTimeDetectData {
    @NotNull
    @Min(0L)
    private Integer total;
    @NotNull
    @Min(0L)
    private Integer ngCount;
    @NotNull
    @Min(0L)
    private Integer removeTotal;
    @NotNull
    @Min(0L)
    private Integer removeFail;
    @NotNull
    @Min(0L)
    private Double efficiency;
    @NotNull
    @Min(0L)
    private Double totalNgRate;
    @NotNull
    @Min(0L)
    private Integer occupancy;
    @NotNull
    @Min(0L)
    private Double occupancyRate;
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

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof RealTimeDetectData other)) {
            return false;
        } else {
            if (!other.canEqual(this)) {
                return false;
            }
            Object this$total = this.getTotal();
            Object other$total = other.getTotal();
            if (this$total == null ? other$total == null : this$total.equals(other$total)) {
                Object this$ngCount = this.getNgCount();
                Object other$ngCount = other.getNgCount();
                if (this$ngCount == null ? other$ngCount == null : this$ngCount.equals(other$ngCount)) {
                    Object this$removeTotal = this.getRemoveTotal();
                    Object other$removeTotal = other.getRemoveTotal();
                    if (this$removeTotal == null ? other$removeTotal == null : this$removeTotal.equals(other$removeTotal)) {
                        Object this$removeFail = this.getRemoveFail();
                        Object other$removeFail = other.getRemoveFail();
                        if (this$removeFail == null ? other$removeFail == null : this$removeFail.equals(other$removeFail)) {
                            Object this$efficiency = this.getEfficiency();
                            Object other$efficiency = other.getEfficiency();
                            if (this$efficiency == null ? other$efficiency == null : this$efficiency.equals(other$efficiency)) {
                                Object this$totalNgRate = this.getTotalNgRate();
                                Object other$totalNgRate = other.getTotalNgRate();
                                if (this$totalNgRate == null ? other$totalNgRate == null : this$totalNgRate.equals(other$totalNgRate)) {
                                    Object this$occupancy = this.getOccupancy();
                                    Object other$occupancy = other.getOccupancy();
                                    if (this$occupancy == null ? other$occupancy == null : this$occupancy.equals(other$occupancy)) {
                                        Object this$occupancyRate = this.getOccupancyRate();
                                        Object other$occupancyRate = other.getOccupancyRate();
                                        if (this$occupancyRate == null ? other$occupancyRate == null : this$occupancyRate.equals(other$occupancyRate)) {
                                            Object this$startTime = this.getStartTime();
                                            Object other$startTime = other.getStartTime();
                                            if (this$startTime == null ? other$startTime == null : this$startTime.equals(other$startTime)) {
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
                                } else {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
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
        return other instanceof RealTimeDetectData;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $total = this.getTotal();
        result = result * 59 + ($total == null ? 43 : $total.hashCode());
        Object $ngCount = this.getNgCount();
        result = result * 59 + ($ngCount == null ? 43 : $ngCount.hashCode());
        Object $removeTotal = this.getRemoveTotal();
        result = result * 59 + ($removeTotal == null ? 43 : $removeTotal.hashCode());
        Object $removeFail = this.getRemoveFail();
        result = result * 59 + ($removeFail == null ? 43 : $removeFail.hashCode());
        Object $efficiency = this.getEfficiency();
        result = result * 59 + ($efficiency == null ? 43 : $efficiency.hashCode());
        Object $totalNgRate = this.getTotalNgRate();
        result = result * 59 + ($totalNgRate == null ? 43 : $totalNgRate.hashCode());
        Object $occupancy = this.getOccupancy();
        result = result * 59 + ($occupancy == null ? 43 : $occupancy.hashCode());
        Object $occupancyRate = this.getOccupancyRate();
        result = result * 59 + ($occupancyRate == null ? 43 : $occupancyRate.hashCode());
        Object $startTime = this.getStartTime();
        result = result * 59 + ($startTime == null ? 43 : $startTime.hashCode());
        Object $defects = this.getDefects();
        return result * 59 + ($defects == null ? 43 : $defects.hashCode());
    }

    @Override
    public String toString() {
        return "RealTimeDetectData(total="
            + this.getTotal()
            + ", ngCount="
            + this.getNgCount()
            + ", removeTotal="
            + this.getRemoveTotal()
            + ", removeFail="
            + this.getRemoveFail()
            + ", efficiency="
            + this.getEfficiency()
            + ", totalNgRate="
            + this.getTotalNgRate()
            + ", occupancy="
            + this.getOccupancy()
            + ", occupancyRate="
            + this.getOccupancyRate()
            + ", startTime="
            + this.getStartTime()
            + ", defects="
            + this.getDefects()
            + ")";
    }
}
