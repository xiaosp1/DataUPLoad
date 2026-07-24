package com.hikrobotics.solution.module.detect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PG 表 workshop_day_record 映射（PSM 1:1 WorkshopDayRecordPO）。
 */
@TableName("workshop_day_record")
public class WorkshopDayRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer rightCount;
    private Integer errorCount;
    private Integer needCount;
    private String time;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public Integer getId() { return id; }
    public WorkshopDayRecord setId(Integer id) { this.id = id; return this; }

    public Integer getRightCount() { return rightCount; }
    public WorkshopDayRecord setRightCount(Integer rightCount) { this.rightCount = rightCount; return this; }

    public Integer getErrorCount() { return errorCount; }
    public WorkshopDayRecord setErrorCount(Integer errorCount) { this.errorCount = errorCount; return this; }

    public Integer getNeedCount() { return needCount; }
    public WorkshopDayRecord setNeedCount(Integer needCount) { this.needCount = needCount; return this; }

    public String getTime() { return time; }
    public WorkshopDayRecord setTime(String time) { this.time = time; return this; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public WorkshopDayRecord setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; return this; }

    public LocalDateTime getCreateTime() { return createTime; }
    public WorkshopDayRecord setCreateTime(LocalDateTime createTime) { this.createTime = createTime; return this; }
}
