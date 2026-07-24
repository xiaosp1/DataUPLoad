package com.hikrobotics.solution.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * DataupLoad alarm_record 表 MyBatis-Plus Mapper。
 * <p>
 * 沿用 PSM AlarmRecordDAO 接口语义；为遵循 PSM 单一 BaseMapper 风格，此处直接继承 MP BaseMapper，
 * 将原 DAO 中的 selectAlarmCountByType() 留待后续迁移。
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {
}
