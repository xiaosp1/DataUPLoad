/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.DefectCountDisPlayDTO
 *  org.assertj.core.util.Lists
 */
package com.hikrobotics.solution.module.line.dto;

import java.util.List;
import org.assertj.core.util.Lists;

public class DefectCountDisPlayDTO {
    private List<String> time = Lists.newArrayList();
    private String type;
    private List<Integer> count = Lists.newArrayList();

    public List<String> getTime() {
        return this.time;
    }

    public String getType() {
        return this.type;
    }

    public List<Integer> getCount() {
        return this.count;
    }

    public DefectCountDisPlayDTO setTime(List<String> time) {
        this.time = time;
        return this;
    }

    public DefectCountDisPlayDTO setType(String type) {
        this.type = type;
        return this;
    }

    public DefectCountDisPlayDTO setCount(List<Integer> count) {
        this.count = count;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DefectCountDisPlayDTO)) {
            return false;
        }
        DefectCountDisPlayDTO other = (DefectCountDisPlayDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        List this$time = this.getTime();
        List other$time = other.getTime();
        if (this$time == null ? other$time != null : !((Object)this$time).equals(other$time)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        List this$count = this.getCount();
        List other$count = other.getCount();
        return !(this$count == null ? other$count != null : !((Object)this$count).equals(other$count));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DefectCountDisPlayDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List $time = this.getTime();
        result = result * 59 + ($time == null ? 43 : ((Object)$time).hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        List $count = this.getCount();
        result = result * 59 + ($count == null ? 43 : ((Object)$count).hashCode());
        return result;
    }

    public String toString() {
        return "DefectCountDisPlayDTO(time=" + this.getTime() + ", type=" + this.getType() + ", count=" + this.getCount() + ")";
    }
}

