package com.hikrobotics.solution.module.line.model;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 线体表 PO（W-B05）
 *
 * <p>1:1 抄自反编译 LinePO（{@code module/line/model/LinePO.java}），对应 PG 表 public.line。</p>
 *
 * <p>本工单优先使用 {@link com.hikrobotics.solution.module.line.entity.Line}（在 entity 包）；
 * 此 PO 类保留以兼容现有 PSM 风格引用（StatusRecordDTO / DefectRecordServiceImpl / ILineDefectTypeService）。</p>
 */
@TableName("line")
public class LinePO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;

    private String lineNo;

    private String faceNo;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String color;

    private String clientNo;

    @TableField(exist = false)
    private Integer order;

    private String realtimeData;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LinePO setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LinePO setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof LinePO other)) {
            return false;
        } else {
            if (!other.canEqual(this)) {
                return false;
            }
            Object this$id = this.getId();
            Object other$id = other.getId();
            if (this$id == null ? other$id == null : this$id.equals(other$id)) {
                Object this$order = this.getOrder();
                Object other$order = other.getOrder();
                if (this$order == null ? other$order == null : this$order.equals(other$order)) {
                    Object this$name = this.getName();
                    Object other$name = other.getName();
                    if (this$name == null ? other$name == null : this$name.equals(other$name)) {
                        Object this$lineNo = this.getLineNo();
                        Object other$lineNo = other.getLineNo();
                        if (this$lineNo == null ? other$lineNo == null : this$lineNo.equals(other$lineNo)) {
                            Object this$faceNo = this.getFaceNo();
                            Object other$faceNo = other.getFaceNo();
                            if (this$faceNo == null ? other$faceNo == null : this$faceNo.equals(other$faceNo)) {
                                Object this$color = this.getColor();
                                Object other$color = other.getColor();
                                if (this$color == null ? other$color == null : this$color.equals(other$color)) {
                                    Object this$clientNo = this.getClientNo();
                                    Object other$clientNo = other.getClientNo();
                                    if (this$clientNo == null ? other$clientNo == null : this$clientNo.equals(other$clientNo)) {
                                        Object this$realtimeData = this.getRealtimeData();
                                        Object other$realtimeData = other.getRealtimeData();
                                        if (this$realtimeData == null ? other$realtimeData == null : this$realtimeData.equals(other$realtimeData)) {
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
        return other instanceof LinePO;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $order = this.getOrder();
        result = result * 59 + ($order == null ? 43 : $order.hashCode());
        Object $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        Object $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        Object $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        Object $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        Object $clientNo = this.getClientNo();
        result = result * 59 + ($clientNo == null ? 43 : $clientNo.hashCode());
        Object $realtimeData = this.getRealtimeData();
        result = result * 59 + ($realtimeData == null ? 43 : $realtimeData.hashCode());
        Object $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : $updateTime.hashCode());
        Object $createTime = this.getCreateTime();
        return result * 59 + ($createTime == null ? 43 : $createTime.hashCode());
    }

    @Override
    public String toString() {
        return "LinePO(id="
            + this.getId()
            + ", name="
            + this.getName()
            + ", lineNo="
            + this.getLineNo()
            + ", faceNo="
            + this.getFaceNo()
            + ", color="
            + this.getColor()
            + ", clientNo="
            + this.getClientNo()
            + ", order="
            + this.getOrder()
            + ", realtimeData="
            + this.getRealtimeData()
            + ", updateTime="
            + this.getUpdateTime()
            + ", createTime="
            + this.getCreateTime()
            + ")";
    }
}
