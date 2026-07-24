package com.hikrobotics.solution.module.yingke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * yingke MES 请求 DTO（PSM 同名字段命名沿用 1:1）。
 */
public class YKRequestDTO<T> {
   @JsonProperty("ApiType")
   private String ApiType;
   @JsonProperty("Parameters")
   private List<T> Parameters;
   @JsonProperty("Method")
   private String Method;
   @JsonProperty("Context")
   private ContextDTO Context;

   public List<T> getParameters() {
      if (this.Parameters == null) {
         this.Parameters = new ArrayList<>();
      }
      return this.Parameters;
   }

   public String getApiType() {
      return this.ApiType;
   }

   public String getMethod() {
      return this.Method;
   }

   public ContextDTO getContext() {
      return this.Context;
   }

   @JsonProperty("ApiType")
   public YKRequestDTO<T> setApiType(String ApiType) {
      this.ApiType = ApiType;
      return this;
   }

   @JsonProperty("Parameters")
   public YKRequestDTO<T> setParameters(List<T> Parameters) {
      this.Parameters = Parameters;
      return this;
   }

   @JsonProperty("Method")
   public YKRequestDTO<T> setMethod(String Method) {
      this.Method = Method;
      return this;
   }

   @JsonProperty("Context")
   public YKRequestDTO<T> setContext(ContextDTO Context) {
      this.Context = Context;
      return this;
   }
}
