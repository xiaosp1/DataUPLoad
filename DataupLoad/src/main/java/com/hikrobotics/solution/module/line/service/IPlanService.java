package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.IdQuery;
import com.hikrobotics.solution.module.line.dto.ClientPlanQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindQueryDTO;
import com.hikrobotics.solution.module.line.dto.PlanDTO;
import com.hikrobotics.solution.module.line.dto.PlanQueryDTO;
import com.hikrobotics.solution.module.line.entity.Plan;

public interface IPlanService extends IService<Plan> {
    BaseResult add(PlanDTO var1);
    BaseResult del(IdQuery var1);
    BaseResult mod(PlanDTO var1);
    BaseResult search(PlanQueryDTO var1);
    BaseResult clientPlan(ClientPlanQueryDTO var1);
    BaseResult getClientBindPlan(LinePlanBindQueryDTO var1);
}
