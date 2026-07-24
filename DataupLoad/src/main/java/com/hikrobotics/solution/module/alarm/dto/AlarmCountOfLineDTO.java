package com.hikrobotics.solution.module.alarm.dto;

/**
 * PSM 1:1 AlarmCountOfLineDTO — 产线维度报警计数。
 */
public class AlarmCountOfLineDTO {
    private Integer count;
    private String lineNo;
    private String faceNo;
    private String defectName;

    public String getKey() { return this.lineNo + ":" + this.faceNo + ":" + this.defectName; }

    public Integer getCount() { return count; }
    public AlarmCountOfLineDTO setCount(Integer count) { this.count = count; return this; }

    public String getLineNo() { return lineNo; }
    public AlarmCountOfLineDTO setLineNo(String lineNo) { this.lineNo = lineNo; return this; }

    public String getFaceNo() { return faceNo; }
    public AlarmCountOfLineDTO setFaceNo(String faceNo) { this.faceNo = faceNo; return this; }

    public String getDefectName() { return defectName; }
    public AlarmCountOfLineDTO setDefectName(String defectName) { this.defectName = defectName; return this; }
}
