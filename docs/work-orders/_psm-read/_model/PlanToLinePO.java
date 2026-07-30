/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableName
 *  com.fasterxml.jackson.annotation.JsonFormat
 *  com.hikrobotics.solution.module.line.model.PlanToLinePO
 */
package com.hikrobotics.solution.module.line.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@TableName(value="plan_to_line")
public class PlanToLinePO {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO)
    private Integer id;
    private Integer lineId;
    private Integer planId;
    private Integer status;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
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

    public PlanToLinePO setId(Integer id) {
        this.id = id;
        return this;
    }

    public PlanToLinePO setLineId(Integer lineId) {
        this.lineId = lineId;
        return this;
    }

    public PlanToLinePO setPlanId(Integer planId) {
        this.planId = planId;
        return this;
    }

    public PlanToLinePO setStatus(Integer status) {
        this.status = status;
        return this;
    }

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    public PlanToLinePO setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    public PlanToLinePO setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PlanToLinePO)) {
            return false;
        }
        PlanToLinePO other = (PlanToLinePO)o;
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
        Integer this$planId = this.getPlanId();
        Integer other$planId = other.getPlanId();
        if (this$planId == null ? other$planId != null : !((Object)this$planId).equals(other$planId)) {
            return false;
        }
        Integer this$status = this.getStatus();
        Integer other$status = other.getStatus();
        if (this$status == null ? other$status != null : !((Object)this$status).equals(other$status)) {
            return false;
        }
        LocalDateTime this$createTime = this.getCreateTime();
        LocalDateTime other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime)) {
            return false;
        }
        LocalDateTime this$updateTime = this.getUpdateTime();
        LocalDateTime other$updateTime = other.getUpdateTime();
        return !(this$updateTime == null ? other$updateTime != null : !((Object)this$updateTime).equals(other$updateTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PlanToLinePO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : ((Object)$lineId).hashCode());
        Integer $planId = this.getPlanId();
        result = result * 59 + ($planId == null ? 43 : ((Object)$planId).hashCode());
        Integer $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : ((Object)$status).hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        return result;
    }

    public String toString() {
        return "PlanToLinePO(id=" + this.getId() + ", lineId=" + this.getLineId() + ", planId=" + this.getPlanId() + ", status=" + this.getStatus() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}

