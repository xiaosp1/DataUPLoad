package com.hikrobotics.solution.module.alarm.event;

import org.springframework.context.ApplicationEvent;

/**
 * 客户端掉线告警处理事件（W-X30b：1:1 抄自 PSM DealAlarmEvent）。
 * <p>
 * PSM 在客户端从 OUTLINE 切换到 ONLINE 时发布此事件，
 * 触发 {@code dealClientAlarmListener} 清理旧的 UNSOLVED 掉线告警。
 */
public class DealAlarmEvent extends ApplicationEvent {
   private String lineNo;
   private String faceNo;
   private Integer reason;

   public DealAlarmEvent(Object source) {
      super(source);
   }

   public DealAlarmEvent setLineNo(String lineNo) {
      this.lineNo = lineNo;
      return this;
   }

   public DealAlarmEvent setFaceNo(String faceNo) {
      this.faceNo = faceNo;
      return this;
   }

   public DealAlarmEvent setReason(Integer reason) {
      this.reason = reason;
      return this;
   }

   public String getLineNo() {
      return this.lineNo;
   }

   public String getFaceNo() {
      return this.faceNo;
   }

   public Integer getReason() {
      return this.reason;
   }
}
