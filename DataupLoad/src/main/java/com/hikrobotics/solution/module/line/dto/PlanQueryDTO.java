/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.framework.common.query.TimePageQuery
 *  com.hikrobotics.solution.module.line.dto.PlanQueryDTO
 */
package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.framework.common.query.TimePageQuery;

public class PlanQueryDTO
extends TimePageQuery {
    private String name;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PlanQueryDTO)) {
            return false;
        }
        PlanQueryDTO other = (PlanQueryDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        return !(this$name == null ? other$name != null : !this$name.equals(other$name));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PlanQueryDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    public String toString() {
        return "PlanQueryDTO(name=" + this.getName() + ")";
    }
}

