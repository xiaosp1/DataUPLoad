package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.line.entity.PlanToLine;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PlanToLineMapper extends BaseMapper<PlanToLine> {

    @Select("SELECT l.client_no FROM plan_to_line ptl " +
            "INNER JOIN line l ON l.id = ptl.line_id " +
            "WHERE ptl.plan_id = #{planId}")
    List<String> selectPlanClient(@Param("planId") Integer planId);
}
