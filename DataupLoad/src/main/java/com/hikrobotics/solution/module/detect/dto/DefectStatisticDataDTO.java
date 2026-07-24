package com.hikrobotics.solution.module.detect.dto;

public class DefectStatisticDataDTO {
    private String type;
    private String lineNo;
    private String faceNo;
    private String time;
    private Integer totalCount;
    private String position;

    public String getPosition() { return lineNo + "_" + faceNo; }

    public String getType() { return type; }
    public String getLineNo() { return lineNo; }
    public String getFaceNo() { return faceNo; }
    public String getTime() { return time; }
    public Integer getTotalCount() { return totalCount; }

    public DefectStatisticDataDTO setType(String type) { this.type = type; return this; }
    public DefectStatisticDataDTO setLineNo(String lineNo) { this.lineNo = lineNo; return this; }
    public DefectStatisticDataDTO setFaceNo(String faceNo) { this.faceNo = faceNo; return this; }
    public DefectStatisticDataDTO setTime(String time) { this.time = time; return this; }
    public DefectStatisticDataDTO setTotalCount(Integer totalCount) { this.totalCount = totalCount; return this; }
    public DefectStatisticDataDTO setPosition(String position) { this.position = position; return this; }
}
