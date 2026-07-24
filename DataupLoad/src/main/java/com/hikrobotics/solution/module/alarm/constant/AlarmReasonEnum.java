package com.hikrobotics.solution.module.alarm.constant;

public enum AlarmReasonEnum {
   DISCONNECT("客户端掉线", 1);

   private final String level;
   private final Integer value;

   public String getLevel() {
      return this.level;
   }

   public Integer getValue() {
      return this.value;
   }

   AlarmReasonEnum(String level, Integer value) {
      this.level = level;
      this.value = value;
   }
}
