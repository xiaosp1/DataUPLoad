package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.line.entity.StateStatistic;

/**
 * 班次统计 Mapper
 * 对应 PG 表 state_statistic
 */
public interface StateStatisticMapper extends BaseMapper<StateStatistic> {

    void insertBatch(java.util.List<StateStatistic> list);
}
