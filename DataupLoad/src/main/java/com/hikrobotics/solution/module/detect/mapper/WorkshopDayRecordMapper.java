package com.hikrobotics.solution.module.detect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.detect.entity.WorkshopDayRecord;
import org.apache.ibatis.annotations.Param;

/**
 * PSM 1:1 WorkshopDayRecordDAO — 车间日统计 Mapper。
 */
public interface WorkshopDayRecordMapper extends BaseMapper<WorkshopDayRecord> {
    boolean updateCount(@Param("rightCount") Integer rightCount,
                        @Param("errorCount") Integer errorCount,
                        @Param("time") String time);
}
