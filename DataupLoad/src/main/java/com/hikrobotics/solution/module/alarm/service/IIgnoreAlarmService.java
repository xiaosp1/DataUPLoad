package com.hikrobotics.solution.module.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.dto.IgnoreAlarmDTO;
import com.hikrobotics.solution.module.alarm.entity.IgnoreAlarm;
import java.util.List;

/**
 * DataupLoad 忽略报警服务接口（沿用 PSM IIgnoreAlarmService 语义）。
 */
public interface IIgnoreAlarmService extends IService<IgnoreAlarm> {
   BaseResult handleAlarmIgnore(IgnoreAlarmDTO var1);

   /**
    * 判断指定 (type + defectName + lineNo + faceNo) 当前是否在忽略白名单内。
    * <p>
    * PSM 原版此处仅有方法签名但 AlarmRecordServiceImpl.sendAlarmMessage() 中并未调用，
    * 而是硬编码 {@code boolean isIgnore = false;}，导致白名单永远失效 —— 详见 W-B04 工单。
    * <p>
    * DataupLoad 实现：实际查询 ignore_alarm 表，过滤 ignore_time &gt; now() 的记录。
    */
   boolean isIgnore(Integer var1, String var2, String var3, String var4);

   void removeExpire();

   List<IgnoreAlarm> getIgnoreDefect();
}
