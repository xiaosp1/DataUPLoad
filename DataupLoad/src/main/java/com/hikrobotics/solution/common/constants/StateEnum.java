package com.hikrobotics.solution.common.constants;

public enum StateEnum {
   YES(1),
   NO(0);

   private final Integer value;

   public Integer getValue() {
      return this.value;
   }

   StateEnum(Integer value) {
      this.value = value;
   }
}
