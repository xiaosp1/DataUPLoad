/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableName
 *  com.hikrobotics.solution.module.line.model.StateChangePO
 */
package com.hikrobotics.solution.module.line.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value="state_change")
public class StateChangePO
implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO)
    private Integer id;
    private Integer lineId;
    private Integer type;
    private LocalDateTime changeTime;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

    public Integer getId() {
        return this.id;
    }

    public Integer getLineId() {
        return this.lineId;
    }

    public Integer getType() {
        return this.type;
    }

    public LocalDateTime getChangeTime() {
        return this.changeTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public StateChangePO setId(Integer id) {
        this.id = id;
        return this;
    }

    public StateChangePO setLineId(Integer lineId) {
        this.lineId = lineId;
        return this;
    }

    public StateChangePO setType(Integer type) {
        this.type = type;
        return this;
    }

    public StateChangePO setChangeTime(LocalDateTime changeTime) {
        this.changeTime = changeTime;
        return this;
    }

    public StateChangePO setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public StateChangePO setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StateChangePO)) {
            return false;
        }
        StateChangePO other = (StateChangePO)o;
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
        Integer this$type = this.getType();
        Integer other$type = other.getType();
        if (this$type == null ? other$type != null : !((Object)this$type).equals(other$type)) {
            return false;
        }
        LocalDateTime this$changeTime = this.getChangeTime();
        LocalDateTime other$changeTime = other.getChangeTime();
        if (this$changeTime == null ? other$changeTime != null : !((Object)this$changeTime).equals(other$changeTime)) {
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
        return other instanceof StateChangePO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : ((Object)$lineId).hashCode());
        Integer $type = this.getType();
        result = result * 59 + ($type == null ? 43 : ((Object)$type).hashCode());
        LocalDateTime $changeTime = this.getChangeTime();
        result = result * 59 + ($changeTime == null ? 43 : ((Object)$changeTime).hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "StateChangePO(id=" + this.getId() + ", lineId=" + this.getLineId() + ", type=" + this.getType() + ", changeTime=" + this.getChangeTime() + ", updateTime=" + this.getUpdateTime() + ", createTime=" + this.getCreateTime() + ")";
    }
}

