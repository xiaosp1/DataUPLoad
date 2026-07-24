package com.hikrobotics.solution.module.line.dto;

import jakarta.validation.constraints.NotNull;

public class ChgLineOrderDTO {
    @NotNull(message="\u7ebf\u4f53\u987a\u5e8f")
    private @NotNull(message="\u7ebf\u4f53\u987a\u5e8f") Integer order;
    @NotNull(message="\u7ebf\u4f53\u6807\u8bc6")
    private @NotNull(message="\u7ebf\u4f53\u6807\u8bc6") Integer lineId;

    public Integer getOrder() {
        return this.order;
    }

    public Integer getLineId() {
        return this.lineId;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ChgLineOrderDTO)) {
            return false;
        }
        ChgLineOrderDTO other = (ChgLineOrderDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$order = this.getOrder();
        Integer other$order = other.getOrder();
        if (this$order == null ? other$order != null : !((Object)this$order).equals(other$order)) {
            return false;
        }
        Integer this$lineId = this.getLineId();
        Integer other$lineId = other.getLineId();
        return !(this$lineId == null ? other$lineId != null : !((Object)this$lineId).equals(other$lineId));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ChgLineOrderDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $order = this.getOrder();
        result = result * 59 + ($order == null ? 43 : ((Object)$order).hashCode());
        Integer $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : ((Object)$lineId).hashCode());
        return result;
    }

    public String toString() {
        return "ChgLineOrderDTO(order=" + this.getOrder() + ", lineId=" + this.getLineId() + ")";
    }
}

