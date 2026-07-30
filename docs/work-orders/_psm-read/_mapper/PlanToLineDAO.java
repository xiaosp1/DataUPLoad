/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  com.hikrobotics.solution.module.line.mapper.PlanToLineDAO
 *  com.hikrobotics.solution.module.line.model.PlanToLinePO
 *  org.apache.ibatis.annotations.Param
 */
package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.line.model.PlanToLinePO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PlanToLineDAO
extends BaseMapper<PlanToLinePO> {
    public List<String> selectPlanClient(@Param(value="planId") Integer var1);
}

