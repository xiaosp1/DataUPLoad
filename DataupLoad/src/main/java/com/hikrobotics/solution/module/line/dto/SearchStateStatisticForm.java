/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.toolkit.StringUtils
 *  com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm
 *  jakarta.validation.constraints.NotBlank
 *  org.assertj.core.util.Sets
 */
package com.hikrobotics.solution.module.line.dto;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.util.Sets;

public class SearchStateStatisticForm {
    public static LocalTime EIGHT = LocalTime.parse("08:00:00");
    private String lineIds;
    private String faceIds;
    @NotBlank
    private String startTime;
    @NotBlank
    private String endTime;

    public Set<Integer> getFaceIds() {
        HashSet faceId = Sets.newHashSet();
        if (StringUtils.isNotBlank((CharSequence)this.faceIds)) {
            for (String id : this.faceIds.split(",")) {
                faceId.add(Integer.parseInt(id));
            }
        }
        return faceId;
    }

    public LocalDate getStartTime() {
        return LocalDate.parse(this.startTime);
    }

    public LocalDate getEndTime() {
        return LocalDate.parse(this.endTime);
    }

    public String getLineIds() {
        return this.lineIds;
    }

    public void setLineIds(String lineIds) {
        this.lineIds = lineIds;
    }

    public void setFaceIds(String faceIds) {
        this.faceIds = faceIds;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SearchStateStatisticForm)) {
            return false;
        }
        SearchStateStatisticForm other = (SearchStateStatisticForm)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$lineIds = this.getLineIds();
        String other$lineIds = other.getLineIds();
        if (this$lineIds == null ? other$lineIds != null : !this$lineIds.equals(other$lineIds)) {
            return false;
        }
        Set this$faceIds = this.getFaceIds();
        Set other$faceIds = other.getFaceIds();
        if (this$faceIds == null ? other$faceIds != null : !((Object)this$faceIds).equals(other$faceIds)) {
            return false;
        }
        LocalDate this$startTime = this.getStartTime();
        LocalDate other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !((Object)this$startTime).equals(other$startTime)) {
            return false;
        }
        LocalDate this$endTime = this.getEndTime();
        LocalDate other$endTime = other.getEndTime();
        return !(this$endTime == null ? other$endTime != null : !((Object)this$endTime).equals(other$endTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof SearchStateStatisticForm;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $lineIds = this.getLineIds();
        result = result * 59 + ($lineIds == null ? 43 : $lineIds.hashCode());
        Set $faceIds = this.getFaceIds();
        result = result * 59 + ($faceIds == null ? 43 : ((Object)$faceIds).hashCode());
        LocalDate $startTime = this.getStartTime();
        result = result * 59 + ($startTime == null ? 43 : ((Object)$startTime).hashCode());
        LocalDate $endTime = this.getEndTime();
        result = result * 59 + ($endTime == null ? 43 : ((Object)$endTime).hashCode());
        return result;
    }

    public String toString() {
        return "SearchStateStatisticForm(lineIds=" + this.getLineIds() + ", faceIds=" + this.getFaceIds() + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ")";
    }
}

