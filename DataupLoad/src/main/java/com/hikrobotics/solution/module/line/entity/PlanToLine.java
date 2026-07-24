package com.hikrobotics.solution.module.line.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@TableName("plan_to_line")
public class PlanToLine {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer lineId;
    private Integer planId;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public Integer getId() {
        return this.id;
    }

    public Integer getLineId() {
        return this.lineId;
    }

    public Integer getPlanId() {
        return this.planId;
    }

    public Integer getStatus() {
        return this.status;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public PlanToLine setId(Integer id) {
        this.id = id;
        return this;
    }

    public PlanToLine setLineId(Integer lineId) {
        this.lineId = lineId;
        return this;
    }

    public PlanToLine setPlanId(Integer planId) {
        this.planId = planId;
        return this;
    }

    public PlanToLine setStatus(Integer status) {
        this.status = status;
        return this;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public PlanToLine setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public PlanToLine setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof PlanToLine other)) {
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
                    Object this$planId = this.getPlanId();
                    Object other$planId = other.getPlanId();
                    if (this$planId == null ? other$planId == null : this$planId.equals(other$planId)) {
                        Object this$status = this.getStatus();
                        Object other$status = other.getStatus();
                        if (this$status == null ? other$status == null : this$status.equals(other$status)) {
                            Object this$createTime = this.getCreateTime();
                            Object other$createTime = other.getCreateTime();
                            if (this$createTime == null ? other$createTime == null : this$createTime.equals(other$createTime)) {
                                Object this$updateTime = this.getUpdateTime();
                                Object other$updateTime = other.getUpdateTime();
                                return this$updateTime == null ? other$updateTime == null : this$updateTime.equals(other$updateTime);
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
            } else {
                return false;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof PlanToLine;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : $lineId.hashCode());
        Object $planId = this.getPlanId();
        result = result * 59 + ($planId == null ? 43 : $planId.hashCode());
        Object $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        Object $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : $createTime.hashCode());
        Object $updateTime = this.getUpdateTime();
        return result * 59 + ($updateTime == null ? 43 : $updateTime.hashCode());
    }

    @Override
    public String toString() {
        return "PlanToLine(id="
            + this.getId()
            + ", lineId="
            + this.getLineId()
            + ", planId="
            + this.getPlanId()
            + ", status="
            + this.getStatus()
            + ", createTime="
            + this.getCreateTime()
            + ", updateTime="
            + this.getUpdateTime()
            + ")";
    }
}
