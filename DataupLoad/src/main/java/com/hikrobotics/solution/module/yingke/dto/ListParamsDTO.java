package com.hikrobotics.solution.module.yingke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/** yingke 列表参数 DTO（用于 pushAlarm 接口的 Parameters[0].Value 包装）。 */
public class ListParamsDTO<T> {
   @JsonProperty("Value")
   private List<T> Value;

   public List<T> getValue() {
      if (this.Value == null) {
         this.Value = new ArrayList<>();
      }
      return this.Value;
   }

   @JsonProperty("Value")
   public void setValue(List<T> Value) {
      this.Value = Value;
   }
}
