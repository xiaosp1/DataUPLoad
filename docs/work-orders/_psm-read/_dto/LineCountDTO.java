/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.LineCountDTO
 */
package com.hikrobotics.solution.module.line.dto;

import java.text.DecimalFormat;

public class LineCountDTO {
    private Integer count = 0;
    private String time;
    private String percentage;
    private Integer errorCount = 0;

    public LineCountDTO calPercentage() {
        if (this.errorCount == 0) {
            this.percentage = "0.00";
        } else {
            float countValue = this.count.floatValue();
            float error = this.errorCount.floatValue();
            DecimalFormat df = new DecimalFormat("0.00");
            this.percentage = df.format(error / countValue * 100.0f);
        }
        return this;
    }

    public Integer getCount() {
        return this.count;
    }

    public String getTime() {
        return this.time;
    }

    public String getPercentage() {
        return this.percentage;
    }

    public Integer getErrorCount() {
        return this.errorCount;
    }

    public LineCountDTO setCount(Integer count) {
        this.count = count;
        return this;
    }

    public LineCountDTO setTime(String time) {
        this.time = time;
        return this;
    }

    public LineCountDTO setPercentage(String percentage) {
        this.percentage = percentage;
        return this;
    }

    public LineCountDTO setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LineCountDTO)) {
            return false;
        }
        LineCountDTO other = (LineCountDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$count = this.getCount();
        Integer other$count = other.getCount();
        if (this$count == null ? other$count != null : !((Object)this$count).equals(other$count)) {
            return false;
        }
        Integer this$errorCount = this.getErrorCount();
        Integer other$errorCount = other.getErrorCount();
        if (this$errorCount == null ? other$errorCount != null : !((Object)this$errorCount).equals(other$errorCount)) {
            return false;
        }
        String this$time = this.getTime();
        String other$time = other.getTime();
        if (this$time == null ? other$time != null : !this$time.equals(other$time)) {
            return false;
        }
        String this$percentage = this.getPercentage();
        String other$percentage = other.getPercentage();
        return !(this$percentage == null ? other$percentage != null : !this$percentage.equals(other$percentage));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LineCountDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $count = this.getCount();
        result = result * 59 + ($count == null ? 43 : ((Object)$count).hashCode());
        Integer $errorCount = this.getErrorCount();
        result = result * 59 + ($errorCount == null ? 43 : ((Object)$errorCount).hashCode());
        String $time = this.getTime();
        result = result * 59 + ($time == null ? 43 : $time.hashCode());
        String $percentage = this.getPercentage();
        result = result * 59 + ($percentage == null ? 43 : $percentage.hashCode());
        return result;
    }

    public String toString() {
        return "LineCountDTO(count=" + this.getCount() + ", time=" + this.getTime() + ", percentage=" + this.getPercentage() + ", errorCount=" + this.getErrorCount() + ")";
    }
}

