package com.hikrobotics.solution.common.constants;

public enum WsTypeEnum {
   SCREEN("screen"),
   ALARM("alarm"),
   ALARM_SOUND("sound"),
   PLAN_CHANGE("planChange");

   private final String Value;

   WsTypeEnum(String value) {
      this.Value = value;
   }

   public String getValue() {
      return this.Value;
   }
}
