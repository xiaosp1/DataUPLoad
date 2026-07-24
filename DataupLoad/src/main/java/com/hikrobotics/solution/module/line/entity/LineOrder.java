package com.hikrobotics.solution.module.line.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("line_order")
public class LineOrder implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer lineId;
    private Integer orderValue;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

    public static LineOrder.LineOrderBuilder builder() {
        return new LineOrder.LineOrderBuilder();
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

    public LineOrder setId(Integer id) {
        this.id = id;
        return this;
    }

    public LineOrder setLineId(Integer lineId) {
        this.lineId = lineId;
        return this;
    }

    public LineOrder setOrderValue(Integer orderValue) {
        this.orderValue = orderValue;
        return this;
    }

    public LineOrder setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public LineOrder setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof LineOrder other)) {
            return false;
        } else {
            if (!other.canEqual(this)) {
                return false;
            }
            Object this$id = this.getId();
            Object other$id = other.getId();
            if (this$id == null ? other$id == null : this$id.equals(other$id)) {
                Object this$lineId = this.getLineId();
                Object other$lineId = other.getLineId();
                if (this$lineId == null ? other$lineId == null : this$lineId.equals(other$lineId)) {
                    Object this$orderValue = this.getOrderValue();
                    Object other$orderValue = other.getOrderValue();
                    if (this$orderValue == null ? other$orderValue == null : this$orderValue.equals(other$orderValue)) {
                        Object this$updateTime = this.getUpdateTime();
                        Object other$updateTime = other.getUpdateTime();
                        if (this$updateTime == null ? other$updateTime == null : this$updateTime.equals(other$updateTime)) {
                            Object this$createTime = this.getCreateTime();
                            Object other$createTime = other.getCreateTime();
                            return this$createTime == null ? other$createTime == null : this$createTime.equals(other$createTime);
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
        return other instanceof LineOrder;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : $lineId.hashCode());
        Object $orderValue = this.getOrderValue();
        result = result * 59 + ($orderValue == null ? 43 : $orderValue.hashCode());
        Object $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : $updateTime.hashCode());
        Object $createTime = this.getCreateTime();
        return result * 59 + ($createTime == null ? 43 : $createTime.hashCode());
    }

    @Override
    public String toString() {
        return "LineOrder(id="
            + this.getId()
            + ", lineId="
            + this.getLineId()
            + ", orderValue="
            + this.getOrderValue()
            + ", updateTime="
            + this.getUpdateTime()
            + ", createTime="
            + this.getCreateTime()
            + ")";
    }

    public LineOrder() {
    }

    public LineOrder(Integer id, Integer lineId, Integer orderValue, LocalDateTime updateTime, LocalDateTime createTime) {
        this.id = id;
        this.lineId = lineId;
        this.orderValue = orderValue;
        this.updateTime = updateTime;
        this.createTime = createTime;
    }

    public static class LineOrderBuilder {
        private Integer id;
        private Integer lineId;
        private Integer orderValue;
        private LocalDateTime updateTime;
        private LocalDateTime createTime;

        LineOrderBuilder() {
        }

        public LineOrder.LineOrderBuilder id(Integer id) {
            this.id = id;
            return this;
        }

        public LineOrder.LineOrderBuilder lineId(Integer lineId) {
            this.lineId = lineId;
            return this;
        }

        public LineOrder.LineOrderBuilder orderValue(Integer orderValue) {
            this.orderValue = orderValue;
            return this;
        }

        public LineOrder.LineOrderBuilder updateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public LineOrder.LineOrderBuilder createTime(LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        public LineOrder build() {
            return new LineOrder(this.id, this.lineId, this.orderValue, this.updateTime, this.createTime);
        }

        @Override
        public String toString() {
            return "LineOrder.LineOrderBuilder(id="
                + this.id
                + ", lineId="
                + this.lineId
                + ", orderValue="
                + this.orderValue
                + ", updateTime="
                + this.updateTime
                + ", createTime="
                + this.createTime
                + ")";
        }
    }
}
