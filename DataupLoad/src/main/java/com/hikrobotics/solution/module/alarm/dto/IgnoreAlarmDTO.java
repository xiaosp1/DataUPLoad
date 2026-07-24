package com.hikrobotics.solution.module.alarm.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 忽略报警请求 DTO（PSM IgnoreAlarmDTO 1:1 对齐 + DataupLoad 兼容字段）。
 *
 * <p>W-ALM-02 修复：
 * <ul>
 *   <li>补齐 PSM 端 {@code startTime/endTime} 两个字段（用于
 *       {@code AlarmRecordServiceImpl.handleAlarmIgnore} 的
 *       {@code between(time, startTime, endTime)} 时间窗过滤 alarm_record）。</li>
 *   <li>保留 DataupLoad 自有的 {@code id} 字段 + {@code ignoreTime} 单点忽略语义，
 *       不破坏现有 {@code IgnoreAlarmServiceImpl.handleAlarmIgnore}（写 ignore_alarm 表）的功能。</li>
 *   <li>setter 全部改为 fluent 返回 {@code this}，匹配 PSM 风格
 *       （PSM 在 {@code handleAlarmIgnore} 中链式调用 {@code form.setLineNo(...).setFaceNo(...)}）。</li>
 * </ul>
 *
 * <p>字段语义：
 * <ul>
 *   <li>{@code ignoreAll == 0}（StateEnum.NO）—— 按本 DTO 的 line/face/type/defectName/startTime/endTime 过滤</li>
 *   <li>{@code ignoreAll == 1}（StateEnum.YES）—— 忽略所有未处理报警（listNotResolveDefectAlarmRecord）</li>
 *   <li>{@code faceId} —— 当不为空时，从 line 表查出 lineNo/faceNo 回填</li>
 *   <li>{@code startTime/endTime} —— 报警发生时间窗口（PSM AlarmRecord.time 是 String）</li>
 *   <li>{@code ignoreTime} —— DataupLoad 拓展：单点 ignore_alarm 写入时间（长期有效可设 2099-12-31）</li>
 * </ul>
 */
public class IgnoreAlarmDTO {
    /** DataupLoad 拓展：主键（IgnoreAlarmServiceImpl 暂未使用，保留以备扩展） */
    private Integer id;
    private Integer type;
    private String defectName;
    private String lineNo;
    private String faceNo;
    /** 0 = 按条件忽略，1 = 全部忽略（PSM 端 int；这里 Integer 与 DPL 原实现一致） */
    private Integer ignoreAll;
    /** PSM 端 String；通过 lineService.getById(faceId) 反查 lineNo/faceNo */
    private String faceId;
    /** PSM 同款：报警发生时间窗下界（alarm_record.time 是 String） */
    private String startTime;
    /** PSM 同款：报警发生时间窗上界 */
    private String endTime;
    /** DataupLoad 拓展：单点忽略的 ignore_alarm.ignore_time 值（yyyy-MM-dd HH:mm:ss） */
    private String ignoreTime;

    public Integer getId() { return id; }
    public IgnoreAlarmDTO setId(Integer id) { this.id = id; return this; }

    public Integer getType() { return type; }
    public IgnoreAlarmDTO setType(Integer type) { this.type = type; return this; }

    public String getDefectName() { return defectName; }
    public IgnoreAlarmDTO setDefectName(String defectName) { this.defectName = defectName; return this; }

    public String getLineNo() { return lineNo; }
    public IgnoreAlarmDTO setLineNo(String lineNo) { this.lineNo = lineNo; return this; }

    public String getFaceNo() { return faceNo; }
    public IgnoreAlarmDTO setFaceNo(String faceNo) { this.faceNo = faceNo; return this; }

    public Integer getIgnoreAll() { return ignoreAll; }
    public IgnoreAlarmDTO setIgnoreAll(Integer ignoreAll) { this.ignoreAll = ignoreAll; return this; }

    public String getFaceId() { return faceId; }
    public IgnoreAlarmDTO setFaceId(String faceId) { this.faceId = faceId; return this; }

    public String getStartTime() { return startTime; }
    public IgnoreAlarmDTO setStartTime(String startTime) { this.startTime = startTime; return this; }

    public String getEndTime() { return endTime; }
    public IgnoreAlarmDTO setEndTime(String endTime) { this.endTime = endTime; return this; }

    public String getIgnoreTime() { return ignoreTime; }
    public IgnoreAlarmDTO setIgnoreTime(String ignoreTime) { this.ignoreTime = ignoreTime; return this; }

    /**
     * DataupLoad 拓展：把 {@code ignoreTime} 字符串解析成 {@link LocalDateTime}，
     * 供 {@code IgnoreAlarmServiceImpl.handleAlarmIgnore} 调用。
     */
    public LocalDateTime getIgnoreTimeAsLocalDateTime() {
        if (ignoreTime == null || ignoreTime.isEmpty()) return null;
        return LocalDateTime.parse(ignoreTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
