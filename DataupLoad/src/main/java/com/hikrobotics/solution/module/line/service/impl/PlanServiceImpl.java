package com.hikrobotics.solution.module.line.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.common.constants.CommonMethod;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.IdQuery;
import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.module.line.dto.ClientPlanQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindQueryDTO;
import com.hikrobotics.solution.module.line.dto.PlanDTO;
import com.hikrobotics.solution.module.line.dto.PlanQueryDTO;
import com.hikrobotics.solution.module.line.entity.Plan;
import com.hikrobotics.solution.module.line.entity.PlanToLine;
import com.hikrobotics.solution.module.line.mapper.PlanMapper;
import com.hikrobotics.solution.module.line.mapper.PlanToLineMapper;
import com.hikrobotics.solution.module.line.service.IPlanService;
import java.io.Serializable;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlanServiceImpl extends ServiceImpl<PlanMapper, Plan> implements IPlanService {
    @Autowired
    private PlanMapper planMapper;
    @Autowired
    private PlanToLineMapper planToLineMapper;
    @Autowired
    private WebSocketHandler webSocketHandler;

    @Override
    public BaseResult add(PlanDTO planDTO) {
        Long count = this.planMapper.selectCount(Wrappers.<Plan>lambdaQuery().eq(Plan::getName, planDTO.getName()));
        if (count > 0L) {
            return BaseResult.build().error("20301").log("add plan error,same name", planDTO.toString());
        }
        Plan plan = new Plan();
        BeanUtil.copyProperties(planDTO, plan);
        this.planMapper.insert(plan);
        return BaseResult.build();
    }

    @Override
    public BaseResult del(IdQuery idQuery) {
        List<PlanToLine> links = this.planToLineMapper.selectList(
            Wrappers.<PlanToLine>lambdaQuery().eq(PlanToLine::getPlanId, idQuery.getId()));
        if (links.size() > 0) {
            return BaseResult.build().error("20302").log("del plan error, plan was linked", idQuery.toString());
        }
        this.planMapper.deleteById((Serializable) idQuery.getId());
        return BaseResult.build();
    }

    @Override
    public BaseResult mod(PlanDTO planDTO) {
        Long count = this.planMapper.selectCount(
            Wrappers.<Plan>lambdaQuery()
                .eq(StringUtils.isNotBlank(planDTO.getName()), Plan::getName, planDTO.getName())
                .ne(Plan::getId, planDTO.getId()));
        if (count > 0L) {
            return BaseResult.build().error("20301").log("mod plan error, same name", planDTO.toString());
        }
        Plan plan = new Plan();
        BeanUtil.copyProperties(planDTO, plan);
        this.planMapper.updateById(plan);
        List<String> clientNos = this.planToLineMapper.selectPlanClient(plan.getId());
        for (String clientNo : clientNos) {
            CommonMethod.sendPlanChange(this.webSocketHandler, clientNo);
        }
        return BaseResult.build();
    }

    @Override
    public BaseResult search(PlanQueryDTO planQueryDTO) {
        String name = planQueryDTO.getName();
        String startTime = planQueryDTO.getStartTime();
        String endTime = planQueryDTO.getEndTime();
        LambdaQueryWrapper<Plan> qw = Wrappers.<Plan>lambdaQuery()
            .like(StringUtils.isNotBlank(name), Plan::getName, name);
        if (StringUtils.isNotBlank(startTime)) {
            qw.ge(Plan::getCreateTime, DateUtil.parse(startTime, "yyyy-MM-dd HH:mm:ss"));
        }
        if (StringUtils.isNotBlank(endTime)) {
            qw.ge(Plan::getCreateTime, DateUtil.parse(endTime, "yyyy-MM-dd HH:mm:ss"));
        }
        qw.orderByDesc(Plan::getCreateTime);
        if (planQueryDTO.isPaged()) {
            Page<Plan> planPage = this.planMapper.selectPage(planQueryDTO.getPage(), qw);
            return BaseResult.build().data(planPage);
        }
        List<Plan> plans = this.planMapper.selectList(qw);
        return BaseResult.build().data(plans);
    }

    @Override
    public BaseResult getClientBindPlan(LinePlanBindQueryDTO linePlanBindQueryDTO) {
        List<?> result = this.planMapper.selectPlanByLineId(linePlanBindQueryDTO.getLineId());
        return BaseResult.build().data(result);
    }

    @Override
    public BaseResult clientPlan(ClientPlanQueryDTO clientPlanQueryDTO) {
        List<?> result = this.planMapper.selectClientPlan(clientPlanQueryDTO.getLineNo(), clientPlanQueryDTO.getFaceNo());
        return BaseResult.build().data(result);
    }
}
