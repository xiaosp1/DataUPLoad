/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.service.IService
 *  com.hikrobotics.solution.framework.common.base.BaseResult
 *  com.hikrobotics.solution.framework.common.query.IdQuery
 *  com.hikrobotics.solution.module.line.dto.ClientPlanQueryDTO
 *  com.hikrobotics.solution.module.line.dto.LinePlanBindQueryDTO
 *  com.hikrobotics.solution.module.line.dto.PlanDTO
 *  com.hikrobotics.solution.module.line.dto.PlanQueryDTO
 *  com.hikrobotics.solution.module.line.model.PlanPO
 *  com.hikrobotics.solution.module.line.service.IPlanService
 */
package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.IdQuery;
import com.hikrobotics.solution.module.line.dto.ClientPlanQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindQueryDTO;
import com.hikrobotics.solution.module.line.dto.PlanDTO;
import com.hikrobotics.solution.module.line.dto.PlanQueryDTO;
import com.hikrobotics.solution.module.line.model.PlanPO;

public interface IPlanService
extends IService<PlanPO> {
    public BaseResult add(PlanDTO var1);

    public BaseResult del(IdQuery var1);

    public BaseResult mod(PlanDTO var1);

    public BaseResult search(PlanQueryDTO var1);

    public BaseResult clientPlan(ClientPlanQueryDTO var1);

    public BaseResult getClientBindPlan(LinePlanBindQueryDTO var1);
}

