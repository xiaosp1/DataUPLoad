package com.hikrobotics.solution.module.yingke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** yingke 字符串参数 DTO（用于 Login 接口的 username/password）。 */
public class StringParamDTO {
   @JsonProperty("Value")
   private String Value;

   public String getValue() {
      return this.Value;
   }

   @JsonProperty("Value")
   public void setValue(String Value) {
      this.Value = Value;
   }

   public StringParamDTO(String Value) {
      this.Value = Value;
   }
}
