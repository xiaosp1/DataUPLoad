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

    @PostMapping("/web/plan")
    public BaseResult add(@RequestBody PlanDTO planDTO) {
        ValidateUtils.validateEntity("PlanController.add", planDTO, GroupA.class);
        return planService.add(planDTO);
    }

    @DeleteMapping("/web/plan")
    public BaseResult del(IdQuery idQuery) {
        ValidateUtils.validateEntity("PlanController.del", idQuery);
        return planService.del(idQuery);
    }

    @PutMapping("/web/plan")
    public BaseResult mod(@RequestBody PlanDTO planDTO) {
        ValidateUtils.validateEntity("PlanController.mod", planDTO, GroupB.class);
        return planService.mod(planDTO);
    }

    @GetMapping("/web/plan")
    public BaseResult search(PlanQueryDTO planQueryDTO) {
        ValidateUtils.validateEntity("PlanController.search", planQueryDTO);
        return planService.search(planQueryDTO);
    }

    @GetMapping("/web/plan-bind")
    public BaseResult getClientBindPlan(LinePlanBindQueryDTO linePlanBindQueryDTO) {
        ValidateUtils.validateEntity("PlanController.getClientBindPlan", linePlanBindQueryDTO);
        return planService.getClientBindPlan(linePlanBindQueryDTO);
    }

    @GetMapping("/client/plan")
    public BaseResult clientPlan(ClientPlanQueryDTO clientPlanQueryDTO) {
        ValidateUtils.validateEntity("PlanController.clientPlan", clientPlanQueryDTO);
        return planService.clientPlan(clientPlanQueryDTO);
    }
}
