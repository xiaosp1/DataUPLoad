/*
 * Decompiled with CFR 0.152.
 */
package com.hikrobotics.solution.framework.common.query;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class IdQuery {
    @NotNull(message="\u6807\u8bc6")
    @Min(value=1L, message="\u6807\u8bc6")
    private Integer id;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IdQuery)) {
            return false;
        }
        IdQuery other = (IdQuery)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$id = this.getId();
        Integer other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        return true;
    }

    protected boolean canEqual(Object other) {
        return other instanceof IdQuery;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        return result;
    }

    public String toString() {
        return "IdQuery(id=" + this.getId() + ")";
    }
}
