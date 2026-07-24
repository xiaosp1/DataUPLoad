package com.hikrobotics.solution.module.alarm.dto;

/** 报警数量 DTO（PSM 同名）。DataupLoad 当前在 handleAlarmNumGet 占位返回。 */
public class AlarmNumDTO {
   private Integer totalNum = 0;
   private Integer highNum = 0;

   public Integer getTotalNum() { return this.totalNum; }
   public Integer getHighNum() { return this.highNum; }
   public AlarmNumDTO setTotalNum(Integer totalNum) { this.totalNum = totalNum; return this; }
   public AlarmNumDTO setHighNum(Integer highNum) { this.highNum = highNum; return this; }
}
