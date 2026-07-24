package com.hikrobotics.solution.module.yingke.event;

import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import org.springframework.context.ApplicationEvent;

/**
 * yingke 报警推送事件（PSM 同名；PO→实体）。AlarmRecordServiceImpl 通过
 * EventUtil.publish() 发布，由 YKServiceImpl.pushAlarm2YK() 异步消费。
 */
public class PushAlarmEvent extends ApplicationEvent {
   private final AlarmRecord alarmRecord;

   public PushAlarmEvent(Object source, AlarmRecord record) {
      super(source);
      this.alarmRecord = record;
   }

   public AlarmRecord getAlarmRecord() {
      return this.alarmRecord;
   }
}
