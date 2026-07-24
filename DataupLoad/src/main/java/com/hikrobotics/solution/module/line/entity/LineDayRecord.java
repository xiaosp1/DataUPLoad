package com.hikrobotics.solution.module.line.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@TableName("line_day_record")
public class LineDayRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private int rightCount;
    private int errorCount;
    private String lineNo;
    private String faceNo;
    private Integer removeTotal;
    private Integer uploadRemoveTotal;
    private String time;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public String getKey() {
        return this.lineNo + ":" + this.faceNo;
    }

    public LocalTime getLocalTime() {
        return HikDateUtil.transformTime(this.time).toLocalTime();
    }

    public Integer getId() {
        return this.id;
    }

    public int getRightCount() {
        return this.rightCount;
    }

    public int getErrorCount() {
        return this.errorCount;
    }

    public String getLineNo() {
        return this.lineNo;
    }

    public String getFaceNo() {
        return this.faceNo;
    }

    public Integer getRemoveTotal() {
        return this.removeTotal;
    }

    public Integer getUploadRemoveTotal() {
        return this.uploadRemoveTotal;
    }

    public String getTime() {
        return this.time;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LineDayRecord setId(Integer id) {
        this.id = id;
        return this;
    }

    public LineDayRecord setRightCount(int rightCount) {
        this.rightCount = rightCount;
        return this;
    }

    public LineDayRecord setErrorCount(int errorCount) {
        this.errorCount = errorCount;
        return this;
    }

    public LineDayRecord setLineNo(String lineNo) {
        this.lineNo = lineNo;
        return this;
    }

    public LineDayRecord setFaceNo(String faceNo) {
        this.faceNo = faceNo;
        return this;
    }

    public LineDayRecord setRemoveTotal(Integer removeTotal) {
        this.removeTotal = removeTotal;
        return this;
    }

    public LineDayRecord setUploadRemoveTotal(Integer uploadRemoveTotal) {
        this.uploadRemoveTotal = uploadRemoveTotal;
        return this;
    }

    public LineDayRecord setTime(String time) {
        this.time = time;
        return this;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LineDayRecord setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LineDayRecord setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof LineDayRecord other)) {
            return false;
        } else {
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getRightCount() != other.getRightCount()) {
                return false;
            }
            if (this.getErrorCount() != other.getErrorCount()) {
                return false;
            }
            Object this$id = this.getId();
            Object other$id = other.getId();
            if (this$id == null ? other$id == null : this$id.equals(other$id)) {
                Object this$removeTotal = this.getRemoveTotal();
                Object other$removeTotal = other.getRemoveTotal();
                if (this$removeTotal == null ? other$removeTotal == null : this$removeTotal.equals(other$removeTotal)) {
                    Object this$uploadRemoveTotal = this.getUploadRemoveTotal();
                    Object other$uploadRemoveTotal = other.getUploadRemoveTotal();
                    if (this$uploadRemoveTotal == null ? other$uploadRemoveTotal == null : this$uploadRemoveTotal.equals(other$uploadRemoveTotal)) {
                        Object this$lineNo = this.getLineNo();
                        Object other$lineNo = other.getLineNo();
                        if (this$lineNo == null ? other$lineNo == null : this$lineNo.equals(other$lineNo)) {
                            Object this$faceNo = this.getFaceNo();
                            Object other$faceNo = other.getFaceNo();
                            if (this$faceNo == null ? other$faceNo == null : this$faceNo.equals(other$faceNo)) {
                                Object this$time = this.getTime();
                                Object other$time = other.getTime();
                                if (this$time == null ? other$time == null : this$time.equals(other$time)) {
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
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof LineDayRecord;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getRightCount();
        result = result * 59 + this.getErrorCount();
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $removeTotal = this.getRemoveTotal();
        result = result * 59 + ($removeTotal == null ? 43 : $removeTotal.hashCode());
        Object $uploadRemoveTotal = this.getUploadRemoveTotal();
        result = result * 59 + ($uploadRemoveTotal == null ? 43 : $uploadRemoveTotal.hashCode());
        Object $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        Object $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        Object $time = this.getTime();
        result = result * 59 + ($time == null ? 43 : $time.hashCode());
        Object $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : $updateTime.hashCode());
        Object $createTime = this.getCreateTime();
        return result * 59 + ($createTime == null ? 43 : $createTime.hashCode());
    }

    @Override
    public String toString() {
        return "LineDayRecord(id="
            + this.getId()
            + ", rightCount="
            + this.getRightCount()
            + ", errorCount="
            + this.getErrorCount()
            + ", lineNo="
            + this.getLineNo()
            + ", faceNo="
            + this.getFaceNo()
            + ", removeTotal="
            + this.getRemoveTotal()
            + ", uploadRemoveTotal="
            + this.getUploadRemoveTotal()
            + ", time="
            + this.getTime()
            + ", updateTime="
            + this.getUpdateTime()
            + ", createTime="
            + this.getCreateTime()
            + ")";
    }
}
