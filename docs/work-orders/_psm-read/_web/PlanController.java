/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.framework.common.base.BaseResult
 *  com.hikrobotics.solution.framework.common.query.IdQuery
 *  com.hikrobotics.solution.framework.common.validation.ValidateUtils
 *  com.hikrobotics.solution.framework.common.validation.group.GroupA
 *  com.hikrobotics.solution.framework.common.validation.group.GroupB
 *  com.hikrobotics.solution.module.line.dto.ClientPlanQueryDTO
 *  com.hikrobotics.solution.module.line.dto.LinePlanBindQueryDTO
 *  com.hikrobotics.solution.module.line.dto.PlanDTO
 *  com.hikrobotics.solution.module.line.dto.PlanQueryDTO
 *  com.hikrobotics.solution.module.line.service.IPlanService
 *  com.hikrobotics.solution.module.line.web.PlanController
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.hikrobotics.solution.module.line.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.IdQuery;
import com.hikrobotics.solution.framework.common.validation.ValidateUtils;
import com.hikrobotics.solution.framework.common.validation.group.GroupA;
import com.hikrobotics.solution.framework.common.validation.group.GroupB;
import com.hikrobotics.solution.module.line.dto.ClientPlanQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindQueryDTO;
import com.hikrobotics.solution.module.line.dto.PlanDTO;
import com.hikrobotics.solution.module.line.dto.PlanQueryDTO;
import com.hikrobotics.solution.module.line.service.IPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class PlanController {
    @Autowired
    private IPlanService planService;

    @PostMapping(value={"/web/plan"})
    public BaseResult add(@RequestBody PlanDTO planDTO) {
        ValidateUtils.validateEntity((String)"PlanController.add", (Object)planDTO, (Class[])new Class[]{GroupA.class});
        return this.planService.add(planDTO);
    }

    @DeleteMapping(value={"/web/plan"})
    public BaseResult del(IdQuery idQuery) {
        ValidateUtils.validateEntity((String)"PlanController.del", (Object)idQuery, (Class[])new Class[0]);
        return this.planService.del(idQuery);
    }

    @PutMapping(value={"/web/plan"})
    public BaseResult mod(@RequestBody PlanDTO planDTO) {
        ValidateUtils.validateEntity((String)"PlanController.mod", (Object)planDTO, (Class[])new Class[]{GroupB.class});
        return this.planService.mod(planDTO);
    }

    @GetMapping(value={"/web/plan"})
    public BaseResult search(PlanQueryDTO planQueryDTO) {
        ValidateUtils.validateEntity((String)"PlanController.search", (Object)planQueryDTO, (Class[])new Class[0]);
        return this.planService.search(planQueryDTO);
    }

    @GetMapping(value={"/web/plan-bind"})
    public BaseResult getClientBindPlan(LinePlanBindQueryDTO linePlanBindQueryDTO) {
        ValidateUtils.validateEntity((String)"PlanController.getClientBindPlan", (Object)linePlanBindQueryDTO, (Class[])new Class[0]);
        return this.planService.getClientBindPlan(linePlanBindQueryDTO);
    }

    @GetMapping(value={"/client/plan"})
    public BaseResult clientPlan(ClientPlanQueryDTO clientPlanQueryDTO) {
        ValidateUtils.validateEntity((String)"PlanController.clientPlan", (Object)clientPlanQueryDTO, (Class[])new Class[0]);
        return this.planService.clientPlan(clientPlanQueryDTO);
    }
}

