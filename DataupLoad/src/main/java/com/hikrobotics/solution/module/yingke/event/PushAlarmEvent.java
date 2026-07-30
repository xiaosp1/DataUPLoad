package com.hikrobotics.solution.module.yingke.event;

import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import org.springframework.context.ApplicationEvent;

/**
 * yingke 报警推送事件（PSM 同名；PO→实体）。
 *
 * <h2>W-DEFECT-CFG 子单 B — 细粒度推送开关</h2>
 * <p>
 * 本工单 B 子单要求：把 {@code screenPublish / ykPublish / soundPublish} 三个布尔
 * 塞进事件，订阅者按开关处理。粗粒度 + 细粒度并存：
 * <ul>
 *   <li>粗粒度全局：{@code yk.uploadEnabled=false} 仍可全局关英科（{@code YKServiceImpl.pushAlarm2YK} 短路）</li>
 *   <li>细粒度：{@code yk.uploadEnabled=true} + {@code defect_type.send_yk_enable=0} → 该缺陷不推英科</li>
 *   <li>同理：{@code defect_type.alarm_enable=0} 不推大屏；{@code defect_type.sound_enable=0} 不推声音</li>
 * </ul>
 *
 * <p>DataupLoad 当前没有拆分三个独立事件（一个事件携带 3 个标志 + 一个 AlarmRecord 即可）；
 * YK 订阅者根据 {@code ykPublish} 决定是否真的推英科；WS 推送（screen + sound）由
 * {@code AlarmRecordServiceImpl.sendAlarmMessage} 在 publish 之前就根据
 * {@code screenPublish/soundPublish} 自行决定（更直接，不需要让 YK 监听器去推 WS）。
 * 这样 YK 监听器只管 YK 一件事，职责清晰。</p>
 *
 * <h3>默认值（defect_type 查不到时的向前兼容）</h3>
 * <ul>
 *   <li>{@code screenPublish = true}（默认推大屏，老 PSM 行为）</li>
 *   <li>{@code ykPublish = false}（默认不推英科，老板要求安全默认）</li>
 *   <li>{@code soundPublish = true}（默认推声音，老 PSM 行为）</li>
 * </ul>
 */
public class PushAlarmEvent extends ApplicationEvent {
   private final AlarmRecord alarmRecord;
   /** 是否推送大屏（WS broadcast ALARM）；由 alarm_record.defect_type.alarm_enable 决定；null = 默认 true */
   private final Boolean screenPublish;
   /** 是否推送英科（yk.uploadEnabled && defect_type.send_yk_enable=1）；null = 默认 false */
   private final Boolean ykPublish;
   /** 是否推送声音（WS broadcast ALARM_SOUND）；null = 默认 true */
   private final Boolean soundPublish;

   public PushAlarmEvent(Object source, AlarmRecord record) {
      this(source, record, null, null, null);
   }

   public PushAlarmEvent(Object source, AlarmRecord record,
                         Boolean screenPublish, Boolean ykPublish, Boolean soundPublish) {
      super(source);
      this.alarmRecord = record;
      this.screenPublish = screenPublish;
      this.ykPublish = ykPublish;
      this.soundPublish = soundPublish;
   }

   public AlarmRecord getAlarmRecord() {
      return this.alarmRecord;
   }

   /** null 表示默认值 true（向前兼容：原 PSM 行为推送大屏） */
   public Boolean getScreenPublish() {
      return this.screenPublish;
   }

   /** null 表示默认值 false（向前兼容：原 PSM 行为不推英科） */
   public Boolean getYkPublish() {
      return this.ykPublish;
   }

   /** null 表示默认值 true（向前兼容：原 PSM 行为推声音） */
   public Boolean getSoundPublish() {
      return this.soundPublish;
   }
}
