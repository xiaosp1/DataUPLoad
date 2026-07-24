package com.hikrobotics.solution.module.yingke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * yingke MES 响应 DTO（PSM 同名字段命名沿用 1:1）。
 */
public class YKResponseDTO {
   public static YKResponseDTO DEFAULT = new YKResponseDTO().setSuccess(false);
   @JsonProperty("Success")
   private Boolean Success;
   @JsonProperty("Message")
   private String Message;
   @JsonProperty("Result")
   private Object Result;
   @JsonProperty("Context")
   private ContextDTO Context;

   public Boolean getSuccess() {
      return this.Success;
   }

   public String getMessage() {
      return this.Message;
   }

   public Object getResult() {
      return this.Result;
   }

   public ContextDTO getContext() {
      return this.Context;
   }

   @JsonProperty("Success")
   public YKResponseDTO setSuccess(Boolean Success) {
      this.Success = Success;
      return this;
   }

   @JsonProperty("Message")
   public YKResponseDTO setMessage(String Message) {
      this.Message = Message;
      return this;
   }

   @JsonProperty("Result")
   public YKResponseDTO setResult(Object Result) {
      this.Result = Result;
      return this;
   }

   @JsonProperty("Context")
   public YKResponseDTO setContext(ContextDTO Context) {
      this.Context = Context;
      return this;
   }
}
