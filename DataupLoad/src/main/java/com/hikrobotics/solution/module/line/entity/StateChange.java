package com.hikrobotics.solution.module.line.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DataupLoad state_change 表映射实体（沿用 PSM StateChangePO 字段）。
 */
@TableName("state_change")
public class StateChange implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer lineId;
    private Integer type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime changeTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public Integer getId() { return id; }
    public Integer getLineId() { return lineId; }
    public Integer getType() { return type; }
    public LocalDateTime getChangeTime() { return changeTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public LocalDateTime getCreateTime() { return createTime; }

    public StateChange setId(Integer id) { this.id = id; return this; }
    public StateChange setLineId(Integer lineId) { this.lineId = lineId; return this; }
    public StateChange setType(Integer type) { this.type = type; return this; }
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public StateChange setChangeTime(LocalDateTime changeTime) { this.changeTime = changeTime; return this; }
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public StateChange setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; return this; }
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public StateChange setCreateTime(LocalDateTime createTime) { this.createTime = createTime; return this; }
}
