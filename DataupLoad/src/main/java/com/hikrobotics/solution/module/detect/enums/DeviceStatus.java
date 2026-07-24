package com.hikrobotics.solution.module.detect.enums;

public enum DeviceStatus {
   ONLINE("在线", 1),
   OUTLINE("掉线", 2);

   private final String status;
   private final Integer value;

   DeviceStatus(String status, Integer value) {
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
