package com.hikrobotics.solution.module.yingke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * yingke 请求上下文 DTO（PSM 同名），用于携带 ticket 与 InvOrgId。
 */
public class ContextDTO {
   @JsonProperty("Ticket")
   private String Ticket;
   @JsonProperty("InvOrgId")
   private Integer InvOrgId;

   public String getTicket() {
      return this.Ticket;
   }

   public Integer getInvOrgId() {
      return this.InvOrgId;
   }

   @JsonProperty("Ticket")
   public void setTicket(String Ticket) {
      this.Ticket = Ticket;
   }

   @JsonProperty("InvOrgId")
   public void setInvOrgId(Integer InvOrgId) {
      this.InvOrgId = InvOrgId;
   }

   public ContextDTO() {
   }

   public ContextDTO(String Ticket, Integer InvOrgId) {
      this.Ticket = Ticket;
      this.InvOrgId = InvOrgId;
   }
}
