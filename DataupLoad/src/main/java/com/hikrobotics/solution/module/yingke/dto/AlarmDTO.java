package com.hikrobotics.solution.module.yingke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hikrobotics.solution.common.utils.EnumUtil;
import com.hikrobotics.solution.module.alarm.constant.AlarmLevelEnum;
import com.hikrobotics.solution.module.alarm.constant.AlarmSolvedEnum;
import com.hikrobotics.solution.module.alarm.constant.AlarmTypeEnum;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;

/**
 * yingke 推送给 MES 的报警 DTO（PSM 同名字段命名沿用 1:1；字段名 PascalCase 以匹配 MES 协议）。
 */
public class AlarmDTO {
   @JsonProperty("WorkShop")
   private String WorkShop;
   @JsonProperty("Line")
   private String Line;
   @JsonProperty("Face")
   private String Face;
   @JsonProperty("AlarmTime")
   private String AlarmTime;
   @JsonProperty("AlarmType")
   private String AlarmType;
   @JsonProperty("AlarmLevel")
   private String AlarmLevel;
   @JsonProperty("AlarmDetails")
   private String AlarmDetails;
   @JsonProperty("AlarmResult")
   private String AlarmResult;
   @JsonProperty("AlarmCount")
   private Integer AlarmCount;

   public static AlarmDTO convertFromPO(AlarmRecord record) {
      AlarmLevelEnum level = EnumUtil.getEnumByValue(record.getLevel(), AlarmLevelEnum::getValue, AlarmLevelEnum.class);
      AlarmTypeEnum type = EnumUtil.getEnumByValue(record.getType(), AlarmTypeEnum::getCode, AlarmTypeEnum.class);
      AlarmSolvedEnum solvedFlag = EnumUtil.getEnumByValue(record.getSolve(), AlarmSolvedEnum::getValue, AlarmSolvedEnum.class);
      return new AlarmDTO()
         .setAlarmDetails(record.getMessage())
         .setAlarmResult(solvedFlag == null ? "" : solvedFlag.getStatus())
         .setAlarmLevel(level == null ? "" : level.getLevel())
         .setAlarmTime(record.getTime())
         .setAlarmType(type == null ? "" : type.getDescription())
         .setFace(record.getFaceNo())
         .setLine(record.getLineNo());
   }

   public String getWorkShop() {
      return this.WorkShop;
   }

   public String getLine() {
      return this.Line;
   }

   public String getFace() {
      return this.Face;
   }

   public String getAlarmTime() {
      return this.AlarmTime;
   }

   public String getAlarmType() {
      return this.AlarmType;
   }

   public String getAlarmLevel() {
      return this.AlarmLevel;
   }

   public String getAlarmDetails() {
      return this.AlarmDetails;
   }

   public String getAlarmResult() {
      return this.AlarmResult;
   }

   public Integer getAlarmCount() {
      return this.AlarmCount;
   }

   @JsonProperty("WorkShop")
   public AlarmDTO setWorkShop(String WorkShop) {
      this.WorkShop = WorkShop;
      return this;
   }

   @JsonProperty("Line")
   public AlarmDTO setLine(String Line) {
      this.Line = Line;
      return this;
   }

   @JsonProperty("Face")
   public AlarmDTO setFace(String Face) {
      this.Face = Face;
      return this;
   }

   @JsonProperty("AlarmTime")
   public AlarmDTO setAlarmTime(String AlarmTime) {
      this.AlarmTime = AlarmTime;
      return this;
   }

   @JsonProperty("AlarmType")
   public AlarmDTO setAlarmType(String AlarmType) {
      this.AlarmType = AlarmType;
      return this;
   }

   @JsonProperty("AlarmLevel")
   public AlarmDTO setAlarmLevel(String AlarmLevel) {
      this.AlarmLevel = AlarmLevel;
      return this;
   }

   @JsonProperty("AlarmDetails")
   public AlarmDTO setAlarmDetails(String AlarmDetails) {
      this.AlarmDetails = AlarmDetails;
      return this;
   }

   @JsonProperty("AlarmResult")
   public AlarmDTO setAlarmResult(String AlarmResult) {
      this.AlarmResult = AlarmResult;
      return this;
   }

   @JsonProperty("AlarmCount")
   public AlarmDTO setAlarmCount(Integer AlarmCount) {
      this.AlarmCount = AlarmCount;
      return this;
   }
}
