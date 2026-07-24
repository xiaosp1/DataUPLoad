package com.hikrobotics.solution.module.line.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DataupLoad state_statistic 表映射实体（沿用 PSM StateStatisticPO 字段）。
 */
@TableName("state_statistic")
public class StateStatistic implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer lineId;
    private Long okTime;
    private Long errorTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime statisticTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // LineNo / FaceNo / Time fields — 运行时填充的展示字段，DB 无对应列
    @TableField(exist = false)
    private String lineNo;
    @TableField(exist = false)
    private String faceNo;
    @TableField(exist = false)
    private LocalDateTime time;

    public Integer getId() { return id; }
    public Integer getLineId() { return lineId; }
    public Long getOkTime() { return okTime; }
    public Long getErrorTime() { return errorTime; }
    public LocalDateTime getStatisticTime() { return statisticTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public String getLineNo() { return lineNo; }
    public String getFaceNo() { return faceNo; }
    public LocalDateTime getTime() { return time; }

    public StateStatistic setId(Integer id) { this.id = id; return this; }
    public StateStatistic setLineId(Integer lineId) { this.lineId = lineId; return this; }
    public StateStatistic setOkTime(Long okTime) { this.okTime = okTime; return this; }
    public StateStatistic setErrorTime(Long errorTime) { this.errorTime = errorTime; return this; }
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public StateStatistic setStatisticTime(LocalDateTime statisticTime) { this.statisticTime = statisticTime; return this; }
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public StateStatistic setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; return this; }
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public StateStatistic setCreateTime(LocalDateTime createTime) { this.createTime = createTime; return this; }
    public StateStatistic setLineNo(String lineNo) { this.lineNo = lineNo; return this; }
    public StateStatistic setFaceNo(String faceNo) { this.faceNo = faceNo; return this; }
    public StateStatistic setTime(LocalDateTime time) { this.time = time; return this; }
}
