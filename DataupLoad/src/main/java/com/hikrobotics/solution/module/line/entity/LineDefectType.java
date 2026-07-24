package com.hikrobotics.solution.module.line.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("line_defect_type")
public class LineDefectType implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer showFlag;
    private String lineNo;
    private String faceNo;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

    public String getPos() {
        return this.lineNo + ":" + this.faceNo;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Integer getShowFlag() {
        return this.showFlag;
    }

    public String getLineNo() {
        return this.lineNo;
    }

    public String getFaceNo() {
        return this.faceNo;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LineDefectType setId(Integer id) {
        this.id = id;
        return this;
    }

    public LineDefectType setName(String name) {
        this.name = name;
        return this;
    }

    public LineDefectType setShowFlag(Integer showFlag) {
        this.showFlag = showFlag;
        return this;
    }

    public LineDefectType setLineNo(String lineNo) {
        this.lineNo = lineNo;
        return this;
    }

    public LineDefectType setFaceNo(String faceNo) {
        this.faceNo = faceNo;
        return this;
    }

    public LineDefectType setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public LineDefectType setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof LineDefectType other)) {
            return false;
        } else {
            if (!other.canEqual(this)) {
                return false;
            }
            Object this$id = this.getId();
            Object other$id = other.getId();
            if (this$id == null ? other$id == null : this$id.equals(other$id)) {
                Object this$showFlag = this.getShowFlag();
                Object other$showFlag = other.getShowFlag();
                if (this$showFlag == null ? other$showFlag == null : this$showFlag.equals(other$showFlag)) {
                    Object this$name = this.getName();
                    Object other$name = other.getName();
                    if (this$name == null ? other$name == null : this$name.equals(other$name)) {
                        Object this$lineNo = this.getLineNo();
                        Object other$lineNo = other.getLineNo();
                        if (this$lineNo == null ? other$lineNo == null : this$lineNo.equals(other$lineNo)) {
                            Object this$faceNo = this.getFaceNo();
                            Object other$faceNo = other.getFaceNo();
                            if (this$faceNo == null ? other$faceNo == null : this$faceNo.equals(other$faceNo)) {
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
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof LineDefectType;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $showFlag = this.getShowFlag();
        result = result * 59 + ($showFlag == null ? 43 : $showFlag.hashCode());
        Object $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        Object $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        Object $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        Object $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : $updateTime.hashCode());
        Object $createTime = this.getCreateTime();
        return result * 59 + ($createTime == null ? 43 : $createTime.hashCode());
    }

    @Override
    public String toString() {
        return "LineDefectType(id="
            + this.getId()
            + ", name="
            + this.getName()
            + ", showFlag="
            + this.getShowFlag()
            + ", lineNo="
            + this.getLineNo()
            + ", faceNo="
            + this.getFaceNo()
            + ", updateTime="
            + this.getUpdateTime()
            + ", createTime="
            + this.getCreateTime()
            + ")";
    }
}
