/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.FieldStrategy
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableField
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableName
 *  com.fasterxml.jackson.annotation.JsonFormat
 *  com.hikrobotics.solution.module.line.model.LinePO
 */
package com.hikrobotics.solution.module.line.model;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value="line")
public class LinePO
implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO)
    private Integer id;
    private String name;
    private String lineNo;
    private String faceNo;
    @TableField(updateStrategy=FieldStrategy.IGNORED)
    private String color;
    private String clientNo;
    @TableField(exist=false)
    private Integer order;
    private String realtimeData;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public String getKey() {
        return this.getLineNo() + ":" + this.getFaceNo();
    }

    public String getPos() {
        return this.lineNo + ":" + this.faceNo;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getLineNo() {
        return this.lineNo;
    }

    public String getFaceNo() {
        return this.faceNo;
    }

    public String getColor() {
        return this.color;
    }

    public String getClientNo() {
        return this.clientNo;
    }

    public Integer getOrder() {
        return this.order;
    }

    public String getRealtimeData() {
        return this.realtimeData;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LinePO setId(Integer id) {
        this.id = id;
        return this;
    }

    public LinePO setName(String name) {
        this.name = name;
        return this;
    }

    public LinePO setLineNo(String lineNo) {
        this.lineNo = lineNo;
        return this;
    }

    public LinePO setFaceNo(String faceNo) {
        this.faceNo = faceNo;
        return this;
    }

    public LinePO setColor(String color) {
        this.color = color;
        return this;
    }

    public LinePO setClientNo(String clientNo) {
        this.clientNo = clientNo;
        return this;
    }

    public LinePO setOrder(Integer order) {
        this.order = order;
        return this;
    }

    public LinePO setRealtimeData(String realtimeData) {
        this.realtimeData = realtimeData;
        return this;
    }

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    public LinePO setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    public LinePO setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LinePO)) {
            return false;
        }
        LinePO other = (LinePO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$id = this.getId();
        Integer other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$order = this.getOrder();
        Integer other$order = other.getOrder();
        if (this$order == null ? other$order != null : !((Object)this$order).equals(other$order)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
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
        String this$color = this.getColor();
        String other$color = other.getColor();
        if (this$color == null ? other$color != null : !this$color.equals(other$color)) {
            return false;
        }
        String this$clientNo = this.getClientNo();
        String other$clientNo = other.getClientNo();
        if (this$clientNo == null ? other$clientNo != null : !this$clientNo.equals(other$clientNo)) {
            return false;
        }
        String this$realtimeData = this.getRealtimeData();
        String other$realtimeData = other.getRealtimeData();
        if (this$realtimeData == null ? other$realtimeData != null : !this$realtimeData.equals(other$realtimeData)) {
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
        return other instanceof LinePO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $order = this.getOrder();
        result = result * 59 + ($order == null ? 43 : ((Object)$order).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        String $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        String $clientNo = this.getClientNo();
        result = result * 59 + ($clientNo == null ? 43 : $clientNo.hashCode());
        String $realtimeData = this.getRealtimeData();
        result = result * 59 + ($realtimeData == null ? 43 : $realtimeData.hashCode());
        LocalDateTime $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : ((Object)$updateTime).hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "LinePO(id=" + this.getId() + ", name=" + this.getName() + ", lineNo=" + this.getLineNo() + ", faceNo=" + this.getFaceNo() + ", color=" + this.getColor() + ", clientNo=" + this.getClientNo() + ", order=" + this.getOrder() + ", realtimeData=" + this.getRealtimeData() + ", updateTime=" + this.getUpdateTime() + ", createTime=" + this.getCreateTime() + ")";
    }
}

