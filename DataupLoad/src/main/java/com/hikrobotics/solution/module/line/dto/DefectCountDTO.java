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

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DefectCountDTO other)) {
            return false;
        } else {
            if (!other.canEqual(this)) {
                return false;
            }
            Object this$count = this.getCount();
            Object other$count = other.getCount();
            if (this$count == null ? other$count == null : this$count.equals(other$count)) {
                Object this$showFlag = this.getShowFlag();
                Object other$showFlag = other.getShowFlag();
                if (this$showFlag == null ? other$showFlag == null : this$showFlag.equals(other$showFlag)) {
                    Object this$time = this.getTime();
                    Object other$time = other.getTime();
                    if (this$time == null ? other$time == null : this$time.equals(other$time)) {
                        Object this$type = this.getType();
                        Object other$type = other.getType();
                        return this$type == null ? other$type == null : this$type.equals(other$type);
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
        return other instanceof DefectCountDTO;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $count = this.getCount();
        result = result * 59 + ($count == null ? 43 : $count.hashCode());
        Object $showFlag = this.getShowFlag();
        result = result * 59 + ($showFlag == null ? 43 : $showFlag.hashCode());
        Object $time = this.getTime();
        result = result * 59 + ($time == null ? 43 : $time.hashCode());
        Object $type = this.getType();
        return result * 59 + ($type == null ? 43 : $type.hashCode());
    }

    @Override
    public String toString() {
        return "DefectCountDTO(count=" + this.getCount() + ", time=" + this.getTime() + ", type=" + this.getType() + ", showFlag=" + this.getShowFlag() + ")";
    }
}
