/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableName
 *  com.hikrobotics.solution.module.line.model.LineOrderPO
 *  com.hikrobotics.solution.module.line.model.LineOrderPO$LineOrderPOBuilder
 */
package com.hikrobotics.solution.module.line.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hikrobotics.solution.module.line.model.LineOrderPO;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value="line_order")
public class LineOrderPO
implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO)
    private Integer id;
    private Integer lineId;
    private Integer orderValue;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

    public static LineOrderPOBuilder builder() {
        return new LineOrderPOBuilder();
    }

    public Integer getId() {
        return this.id;
    }

    public Integer getLineId() {
        return this.lineId;
    }

    public Integer getOrderValue() {
        return this.orderValue;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LineOrderPO setId(Integer id) {
        this.id = id;
        return this;
    }

    public LineOrderPO setLineId(Integer lineId) {
        this.lineId = lineId;
        return this;
    }

    public LineOrderPO setOrderValue(Integer orderValue) {
        this.orderValue = orderValue;
        return this;
    }

    public LineOrderPO setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public LineOrderPO setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LineOrderPO)) {
            return false;
        }
        LineOrderPO other = (LineOrderPO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$id = this.getId();
        Integer other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$lineId = this.getLineId();
        Integer other$lineId = other.getLineId();
        if (this$lineId == null ? other$lineId != null : !((Object)this$lineId).equals(other$lineId)) {
            return false;
        }
        Integer this$orderValue = this.getOrderValue();
        Integer other$orderValue = other.getOrderValue();
        if (this$orderValue == null ? other$orderValue != null : !((Object)this$orderValue).equals(other$orderValue)) {
            return false;
        }
        LocalDateTime this$updateTime = this.getUpdateTime();
        LocalDateTime other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !((Object)this$updateTime).equals(other$updateTime)) {
            return false;
        }
        LocalDateTime this$createTime = this.getCreateTime();
        LocalDateTime other$createTime = other.getCreateTime();
        return !(this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LineOrderPO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : ((Object)$lineId).hashCode());
        Integer $orderValue = this.getOrderValue();
        result = result * 59 + ($orderValue == null ? 43 : ((Object)$orderValue).hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "LineOrderPO(id=" + this.getId() + ", lineId=" + this.getLineId() + ", orderValue=" + this.getOrderValue() + ", updateTime=" + this.getUpdateTime() + ", createTime=" + this.getCreateTime() + ")";
    }

    public LineOrderPO() {
    }

    public LineOrderPO(Integer id, Integer lineId, Integer orderValue, LocalDateTime updateTime, LocalDateTime createTime) {
        this.id = id;
        this.lineId = lineId;
        this.orderValue = orderValue;
        this.updateTime = updateTime;
        this.createTime = createTime;
    }
}

