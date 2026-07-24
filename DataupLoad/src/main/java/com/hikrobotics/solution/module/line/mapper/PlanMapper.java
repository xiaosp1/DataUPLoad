package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.line.dto.ClientPlanResultDTO;
import com.hikrobotics.solution.module.line.dto.WebLineBindPlanResultDTO;
import com.hikrobotics.solution.module.line.entity.Plan;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

public interface PlanMapper extends BaseMapper<Plan> {

    @Select("SELECT p.name, p.uri, p.description, ptl.status, p.update_time, p.create_time " +
            "FROM plan p INNER JOIN plan_to_line ptl ON p.id = ptl.plan_id " +
            "INNER JOIN line l ON l.id = ptl.line_id " +
            "WHERE l.line_no = #{lineNo} AND l.face_no = #{faceNo}")
    @Results(id = "clientPlanResult", value = {
        @Result(column = "name", property = "name"),
        @Result(column = "uri", property = "uri"),
        @Result(column = "description", property = "description"),
        @Result(column = "status", property = "status"),
        @Result(column = "update_time", property = "updateTime"),
        @Result(column = "create_time", property = "createTime")
    })
    List<ClientPlanResultDTO> selectClientPlan(@Param("lineNo") String lineNo, @Param("faceNo") String faceNo);

    @Select("SELECT p.id, p.name, p.uri, p.description, ptl.status, p.update_time, p.create_time " +
            "FROM plan p INNER JOIN plan_to_line ptl ON p.id = ptl.plan_id " +
            "WHERE ptl.line_id = #{lineId}")
    @Results(id = "webLineBindPlanResult", value = {
        @Result(column = "id", property = "id"),
        @Result(column = "name", property = "name"),
        @Result(column = "uri", property = "uri"),
        @Result(column = "description", property = "description"),
        @Result(column = "status", property = "status"),
        @Result(column = "update_time", property = "updateTime"),
        @Result(column = "create_time", property = "createTime")
    })
    List<WebLineBindPlanResultDTO> selectPlanByLineId(@Param("lineId") Integer lineId);
}
