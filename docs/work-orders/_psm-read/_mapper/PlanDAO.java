/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  com.hikrobotics.solution.module.line.dto.ClientPlanResultDTO
 *  com.hikrobotics.solution.module.line.dto.WebLineBindPlanResultDTO
 *  com.hikrobotics.solution.module.line.mapper.PlanDAO
 *  com.hikrobotics.solution.module.line.model.PlanPO
 *  org.apache.ibatis.annotations.Param
 */
package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.line.dto.ClientPlanResultDTO;
import com.hikrobotics.solution.module.line.dto.WebLineBindPlanResultDTO;
import com.hikrobotics.solution.module.line.model.PlanPO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PlanDAO
extends BaseMapper<PlanPO> {
    public List<ClientPlanResultDTO> selectClientPlan(@Param(value="lineNo") String var1, @Param(value="faceNo") String var2);

    public List<WebLineBindPlanResultDTO> selectPlanByLineId(@Param(value="lineId") Integer var1);
}

