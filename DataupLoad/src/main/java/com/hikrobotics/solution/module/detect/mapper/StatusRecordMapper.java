package com.hikrobotics.solution.module.detect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * DataupLoad status_record 表 MyBatis-Plus Mapper（沿用 PSM StatusRecordDAO 语义）。
 */
@Mapper
public interface StatusRecordMapper extends BaseMapper<StatusRecord> {
}
