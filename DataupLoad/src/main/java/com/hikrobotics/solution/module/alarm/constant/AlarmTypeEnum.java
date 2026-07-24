package com.hikrobotics.solution.module.alarm.constant;

import java.util.Objects;

public enum AlarmTypeEnum {
   DEFECT(1, "缺陷报警", "defect_alarm_sound_uri"),
   SYSTEM(2, "系统报警", "system_alarm_sound_uri"),
   DEVICE(3, "设备报警", "device_alarm_sound_uri");

   private final Integer code;
   public final String description;
   private final String soundConfigKey;

   public static AlarmTypeEnum getByCode(Integer code) {
      for (AlarmTypeEnum type : values()) {
         if (Objects.equals(type.getCode(), code)) {
            return type;
         }
      }

      return null;
   }

   public static AlarmTypeEnum getByConfigKey(String soundConfigKey) {
      for (AlarmTypeEnum type : values()) {
         if (Objects.equals(type.getSoundConfigKey(), soundConfigKey)) {
            return type;
         }
      }

      return null;
   }

   public Integer getCode() {
      return this.code;
   }

   public String getDescription() {
      return this.description;
   }

   public String getSoundConfigKey() {
      return this.soundConfigKey;
   }

   AlarmTypeEnum(Integer code, String description, String soundConfigKey) {
      this.code = code;
      this.description = description;
      this.soundConfigKey = soundConfigKey;
   }
}
