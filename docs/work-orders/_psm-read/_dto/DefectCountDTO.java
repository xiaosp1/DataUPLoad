/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.DefectCountDTO
 */
package com.hikrobotics.solution.module.line.dto;

public class DefectCountDTO {
    private Integer count = 0;
    private String time;
    private String type;
    private Integer showFlag;

    public Integer getCount() {
        return this.count;
    }

    public String getTime() {
        return this.time;
    }

    public String getType() {
        return this.type;
    }

    public Integer getShowFlag() {
        return this.showFlag;
    }

    public DefectCountDTO setCount(Integer count) {
        this.count = count;
        return this;
    }

    public DefectCountDTO setTime(String time) {
        this.time = time;
        return this;
    }

    public DefectCountDTO setType(String type) {
        this.type = type;
        return this;
    }

    public DefectCountDTO setShowFlag(Integer showFlag) {
        this.showFlag = showFlag;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DefectCountDTO)) {
            return false;
        }
        DefectCountDTO other = (DefectCountDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$count = this.getCount();
        Integer other$count = other.getCount();
        if (this$count == null ? other$count != null : !((Object)this$count).equals(other$count)) {
            return false;
        }
        Integer this$showFlag = this.getShowFlag();
        Integer other$showFlag = other.getShowFlag();
        if (this$showFlag == null ? other$showFlag != null : !((Object)this$showFlag).equals(other$showFlag)) {
            return false;
        }
        String this$time = this.getTime();
        String other$time = other.getTime();
        if (this$time == null ? other$time != null : !this$time.equals(other$time)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        return !(this$type == null ? other$type != null : !this$type.equals(other$type));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DefectCountDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $count = this.getCount();
        result = result * 59 + ($count == null ? 43 : ((Object)$count).hashCode());
        Integer $showFlag = this.getShowFlag();
        result = result * 59 + ($showFlag == null ? 43 : ((Object)$showFlag).hashCode());
        String $time = this.getTime();
        result = result * 59 + ($time == null ? 43 : $time.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        return result;
    }

    public String toString() {
        return "DefectCountDTO(count=" + this.getCount() + ", time=" + this.getTime() + ", type=" + this.getType() + ", showFlag=" + this.getShowFlag() + ")";
    }
}

