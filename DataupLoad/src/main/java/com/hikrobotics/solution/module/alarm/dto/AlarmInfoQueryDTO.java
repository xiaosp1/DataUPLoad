package com.hikrobotics.solution.module.alarm.dto;

import com.hikrobotics.solution.framework.common.query.TimePageQuery;

/**
 * 报警详情+关联设备查询 DTO（PSM AlarmInfoQueryDTO 1:1 对齐）。
 *
 * <p>W-ALM-02 修复：补齐 {@code faceId} 字段 + 继承 {@link TimePageQuery}。
 * PSM 端 {@code AlarmRecordServiceImpl.getAlarmListInfo} 用
 * {@code getStartTime()/getEndTime()} 做时间窗 + 可选 {@code getFaceId()} 锁产线工位。</p>
 */
public class AlarmInfoQueryDTO extends TimePageQuery {
    private Integer faceId;

    public Integer getFaceId() { return this.faceId; }
    public void setFaceId(Integer faceId) { this.faceId = faceId; }
}
