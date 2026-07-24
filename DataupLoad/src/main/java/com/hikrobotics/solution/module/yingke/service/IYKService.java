package com.hikrobotics.solution.module.yingke.service;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import com.hikrobotics.solution.module.yingke.event.PushAlarmEvent;

/**
 * yingke 服务接口（沿用 PSM IYKService 语义）。
 * <p>
 * DataupLoad 当前实现重点：
 * <ul>
 *   <li>{@link #pushAlarm2YK(PushAlarmEvent)} —— 报警事件异步推 MES（核心）</li>
 *   <li>{@link #pushAlarm(AlarmRecord)} —— 同步入口包装，便于业务侧直接调用</li>
 * </ul>
 * 其余 PSM 接口（handleLineAndDefectSearch / searchDefectRecord）保留签名占位。
 */
public interface IYKService {
   BaseResult handleLineAndDefectSearch();

   BaseResult searchDefectRecord(com.hikrobotics.solution.module.yingke.dto.SearchDefectRecordDTO var1);

   /**
    * 异步推报警到 MES（PSM @EventListener 同名方法 pushAlarm2YK）。
    */
   void pushAlarm2YK(PushAlarmEvent var1);

   /**
    * 同步入口：发布 PushAlarmEvent，事件由 {@link #pushAlarm2YK(PushAlarmEvent)} 异步消费。
    * <p>
    * 提供此方法是为了让 AlarmRecordServiceImpl 可以直接传入 AlarmRecord，而不必自行 new 事件。
    */
   void pushAlarm(AlarmRecord record);
}
