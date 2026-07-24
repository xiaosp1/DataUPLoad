package com.hikrobotics.solution.module.detect.enums;

public enum DeviceType {
   CAMERA("相机", 1),
   MACHINE("剔除机", 2),
   CLIENT("客户端", 3);

   private final String status;
   private final Integer value;

   DeviceType(String status, Integer value) {
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
