package com.hikrobotics.solution.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.dto.IgnoreAlarmDTO;
import com.hikrobotics.solution.module.alarm.entity.IgnoreAlarm;
import com.hikrobotics.solution.module.alarm.mapper.IgnoreAlarmMapper;
import com.hikrobotics.solution.module.alarm.service.IIgnoreAlarmService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * ignore_alarm Service 实现（对齐 PSM IgnoreAlarmServiceImpl 语义）。
 *
 * <p>PSM 方式：
 * <ul>
 *   <li>比较用 {@code ignore_time} 列和 {@link LocalDateTime#now()}，lambda 写法直接比较，无 SQL 注入风险。 </li>
 *   <li>IGNORE 写入时设 ignore_time = 指定时间戳（长期有效设为 2099-12-31 23:59:59）。</li>
 * </ul>
 */
@Service
public class IgnoreAlarmServiceImpl extends ServiceImpl<IgnoreAlarmMapper, IgnoreAlarm> implements IIgnoreAlarmService {

   @Override
   public BaseResult handleAlarmIgnore(IgnoreAlarmDTO form) {
      if (form == null) {
         return BaseResult.build().error("20101");
      }
      if (form.getType() == null || form.getDefectName() == null) {
         return BaseResult.build().error("20102");
      }
      IgnoreAlarm entity = new IgnoreAlarm()
         .setType(form.getType())
         .setDefectName(form.getDefectName())
         .setLineNo(form.getLineNo())
         .setFaceNo(form.getFaceNo());
      // PSM: ignore_time 为指定时间戳；DTO 未传则默认长期有效
      if (form.getIgnoreTime() != null && !form.getIgnoreTime().isEmpty()) {
         entity.setIgnoreTimeByString(form.getIgnoreTime());
      } else {
         entity.setIgnoreTime(LocalDateTime.of(2099, 12, 31, 23, 59, 59));
      }
      this.save(entity);
      return BaseResult.build().ok();
   }

   @Override
   public boolean isIgnore(Integer type, String defectName, String lineNo, String faceNo) {
      if (type == null || defectName == null || lineNo == null || faceNo == null) {
         return false;
      }
      // PSM 1:1: lambda 直接比 LocalDateTime
      LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
         .eq(IgnoreAlarm::getType, type)
         .eq(IgnoreAlarm::getDefectName, defectName)
         .eq(IgnoreAlarm::getLineNo, lineNo)
         .eq(IgnoreAlarm::getFaceNo, faceNo)
         .gt(IgnoreAlarm::getIgnoreTime, LocalDateTime.now());
      return this.count(qw) != 0L;
   }

   @Override
   public void removeExpire() {
      // PSM 1:1: 删 ignore_time < now 的过期记录
      LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
         .lt(IgnoreAlarm::getIgnoreTime, LocalDateTime.now());
      this.remove(qw);
   }

   @Override
   public List<IgnoreAlarm> getIgnoreDefect() {
      // PSM 1:1: 查 ignore_time > now 的生效记录
      LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
         .gt(IgnoreAlarm::getIgnoreTime, LocalDateTime.now());
      List<IgnoreAlarm> list = this.list(qw);
      return list == null ? Collections.emptyList() : list;
   }
}
