package com.hikrobotics.solution.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.alarm.entity.IgnoreAlarm;
import org.apache.ibatis.annotations.Mapper;

/**
 * DataupLoad ignore_alarm 表 MyBatis-Plus Mapper（沿用 PSM IgnoreAlarmDAO 语义）。
 */
@Mapper
public interface IgnoreAlarmMapper extends BaseMapper<IgnoreAlarm> {
}
