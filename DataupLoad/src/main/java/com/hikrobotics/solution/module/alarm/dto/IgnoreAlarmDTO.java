package com.hikrobotics.solution.module.alarm.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 忽略报警请求 DTO（对齐 PSM 同名字段）。
 */
public class IgnoreAlarmDTO {
   private Integer id;
   private Integer type;
   private String defectName;
   private String lineNo;
   private String faceNo;
   private Integer ignoreAll;
   private String faceId;
   /** ignore_time 的字符串形式，格式 "yyyy-MM-dd HH:mm:ss" */
   private String ignoreTime;

   public Integer getId() { return id; }
   public void setId(Integer id) { this.id = id; }
   public Integer getType() { return type; }
   public void setType(Integer type) { this.type = type; }
   public String getDefectName() { return defectName; }
   public void setDefectName(String defectName) { this.defectName = defectName; }
   public String getLineNo() { return lineNo; }
   public void setLineNo(String lineNo) { this.lineNo = lineNo; }
   public String getFaceNo() { return faceNo; }
   public void setFaceNo(String faceNo) { this.faceNo = faceNo; }
   public Integer getIgnoreAll() { return ignoreAll; }
   public void setIgnoreAll(Integer ignoreAll) { this.ignoreAll = ignoreAll; }
   public String getFaceId() { return faceId; }
   public void setFaceId(String faceId) { this.faceId = faceId; }
   public String getIgnoreTime() { return ignoreTime; }
   public void setIgnoreTime(String ignoreTime) { this.ignoreTime = ignoreTime; }

   public LocalDateTime getIgnoreTimeAsLocalDateTime() {
      if (ignoreTime == null || ignoreTime.isEmpty()) return null;
      return LocalDateTime.parse(ignoreTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
   }
}
