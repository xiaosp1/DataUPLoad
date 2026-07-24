package com.hikrobotics.solution.module.alarm.dto;

/**
 * 报警多条件搜索 DTO（PSM SearchAlarmDTO 1:1 对齐）。
 *
 * <p>W-ALM-02 修复：补齐 PSM 端 3 个字段 {@code type/lineNo/faceNo}。
 * PSM 端 {@code AlarmRecordServiceImpl.handleAlarmSearch} 的语义：
 * <ul>
 *   <li>{@code type != 4} → 走 {@code IStatusRecordService.searchOffLineClient(...)}（离线客户端查询）</li>
 *   <li>{@code type == 4} → 走 {@code alarm_record} 表查 {@code (lineNo, faceNo, type=DEFECT, solve=UNSOLVED)}</li>
 * </ul>
 * </p>
 */
public class SearchAlarmDTO {
    private Integer type;
    private String lineNo;
    private String faceNo;

    public Integer getType() { return this.type; }
    public String getLineNo() { return this.lineNo; }
    public String getFaceNo() { return this.faceNo; }

    public void setType(Integer type) { this.type = type; }
    public void setLineNo(String lineNo) { this.lineNo = lineNo; }
    public void setFaceNo(String faceNo) { this.faceNo = faceNo; }
}
