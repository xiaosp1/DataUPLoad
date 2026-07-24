package com.hikrobotics.solution.module.alarm.constant;

public enum AlarmLevelEnum {
   NORMAL("normal", 1),
   HIGH("high", 2);

   private final String level;
   private final Integer value;

   AlarmLevelEnum(String level, Integer value) {
      this.level = level;
      this.value = value;
   }

   public String getLevel() {
      return this.level;
   }

   public Integer getValue() {
      return this.value;
   }
}
