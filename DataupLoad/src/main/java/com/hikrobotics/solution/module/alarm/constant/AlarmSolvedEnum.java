package com.hikrobotics.solution.module.alarm.constant;

public enum AlarmSolvedEnum {
   SOLVED("已处理", 1),
   UNSOLVED("未处理", 2),
   IGNORE("已忽略", 3);

   private final String status;
   private final Integer value;

   AlarmSolvedEnum(String status, Integer value) {
      this.status = status;
      this.value = value;
   }

   public String getStatus() {
      return this.status;
   }

   public Integer getValue() {
      return this.value;
   }
}
