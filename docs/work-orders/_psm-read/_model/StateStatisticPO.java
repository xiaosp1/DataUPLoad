/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableField
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableName
 *  com.hikrobotics.solution.common.utils.MathUtils
 *  com.hikrobotics.solution.framework.util.HikDateUtil
 *  com.hikrobotics.solution.module.line.model.StateStatisticPO
 */
package com.hikrobotics.solution.module.line.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hikrobotics.solution.common.utils.MathUtils;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

@TableName(value="state_statistic")
public class StateStatisticPO
implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO)
    private Integer id;
    @TableField(exist=false)
    private String lineNo;
    @TableField(exist=false)
    private String faceNo;
    private Integer lineId;
    private LocalDateTime statisticTime;
    @TableField(exist=false)
    private LocalDateTime time;
    private long okTime;
    private long errorTime;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

    public String getWorkShift() {
        int hours = this.statisticTime.getHour();
        return hours >= 8 && hours < 20 ? "A\u73ed" : "B\u73ed";
    }

    public String getOkRate() {
        double result = MathUtils.div((long)this.okTime, (long)(this.okTime + this.errorTime), (int)3);
        DecimalFormat df = new DecimalFormat("0.0");
        return df.format(result);
    }

    public String getErrorRate() {
        double result = MathUtils.div((long)this.errorTime, (long)(this.okTime + this.errorTime), (int)3);
        DecimalFormat df = new DecimalFormat("0.0");
        return df.format(result);
    }

    public String getTime() {
        return HikDateUtil.formatLocalDate((LocalDateTime)this.time, (String)"MM.dd\u53f7");
    }

    public Integer getId() {
        return this.id;
    }

    public String getLineNo() {
        return this.lineNo;
    }

    public String getFaceNo() {
        return this.faceNo;
    }

    public Integer getLineId() {
        return this.lineId;
    }

    public LocalDateTime getStatisticTime() {
        return this.statisticTime;
    }

    public long getOkTime() {
        return this.okTime;
    }

    public long getErrorTime() {
        return this.errorTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public StateStatisticPO setId(Integer id) {
        this.id = id;
        return this;
    }

    public StateStatisticPO setLineNo(String lineNo) {
        this.lineNo = lineNo;
        return this;
    }

    public StateStatisticPO setFaceNo(String faceNo) {
        this.faceNo = faceNo;
        return this;
    }

    public StateStatisticPO setLineId(Integer lineId) {
        this.lineId = lineId;
        return this;
    }

    public StateStatisticPO setStatisticTime(LocalDateTime statisticTime) {
        this.statisticTime = statisticTime;
        return this;
    }

    public StateStatisticPO setTime(LocalDateTime time) {
        this.time = time;
        return this;
    }

    public StateStatisticPO setOkTime(long okTime) {
        this.okTime = okTime;
        return this;
    }

    public StateStatisticPO setErrorTime(long errorTime) {
        this.errorTime = errorTime;
        return this;
    }

    public StateStatisticPO setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public StateStatisticPO setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StateStatisticPO)) {
            return false;
        }
        StateStatisticPO other = (StateStatisticPO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getOkTime() != other.getOkTime()) {
            return false;
        }
        if (this.getErrorTime() != other.getErrorTime()) {
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
        String this$lineNo = this.getLineNo();
        String other$lineNo = other.getLineNo();
        if (this$lineNo == null ? other$lineNo != null : !this$lineNo.equals(other$lineNo)) {
            return false;
        }
        String this$faceNo = this.getFaceNo();
        String other$faceNo = other.getFaceNo();
        if (this$faceNo == null ? other$faceNo != null : !this$faceNo.equals(other$faceNo)) {
            return false;
        }
        LocalDateTime this$statisticTime = this.getStatisticTime();
        LocalDateTime other$statisticTime = other.getStatisticTime();
        if (this$statisticTime == null ? other$statisticTime != null : !((Object)this$statisticTime).equals(other$statisticTime)) {
            return false;
        }
        String this$time = this.getTime();
        String other$time = other.getTime();
        if (this$time == null ? other$time != null : !this$time.equals(other$time)) {
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
        return other instanceof StateStatisticPO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $okTime = this.getOkTime();
        result = result * 59 + (int)($okTime >>> 32 ^ $okTime);
        long $errorTime = this.getErrorTime();
        result = result * 59 + (int)($errorTime >>> 32 ^ $errorTime);
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : ((Object)$lineId).hashCode());
        String $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        String $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        LocalDateTime $statisticTime = this.getStatisticTime();
        result = result * 59 + ($statisticTime == null ? 43 : ((Object)$statisticTime).hashCode());
        String $time = this.getTime();
        result = result * 59 + ($time == null ? 43 : $time.hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "StateStatisticPO(id=" + this.getId() + ", lineNo=" + this.getLineNo() + ", faceNo=" + this.getFaceNo() + ", lineId=" + this.getLineId() + ", statisticTime=" + this.getStatisticTime() + ", time=" + this.getTime() + ", okTime=" + this.getOkTime() + ", errorTime=" + this.getErrorTime() + ", updateTime=" + this.getUpdateTime() + ", createTime=" + this.getCreateTime() + ")";
    }
}

