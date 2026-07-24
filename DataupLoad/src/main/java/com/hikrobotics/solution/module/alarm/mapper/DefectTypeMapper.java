package com.hikrobotics.solution.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.alarm.model.DefectType;
import org.apache.ibatis.annotations.Mapper;

/**
 * DataupLoad defect_type 表 MyBatis-Plus Mapper（沿用 PSM DefectTypeDAO 语义）。
 */
@Mapper
public interface DefectTypeMapper extends BaseMapper<DefectType> {
}
