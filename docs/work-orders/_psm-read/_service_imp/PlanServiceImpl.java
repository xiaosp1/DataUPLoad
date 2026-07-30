/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.hutool.core.bean.BeanUtil
 *  cn.hutool.core.date.DateUtil
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.baomidou.mybatisplus.core.metadata.IPage
 *  com.baomidou.mybatisplus.core.toolkit.StringUtils
 *  com.baomidou.mybatisplus.core.toolkit.Wrappers
 *  com.baomidou.mybatisplus.extension.plugins.pagination.Page
 *  com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
 *  com.hikrobotics.solution.common.constants.CommonMethod
 *  com.hikrobotics.solution.framework.common.base.BaseResult
 *  com.hikrobotics.solution.framework.common.query.IdQuery
 *  com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler
 *  com.hikrobotics.solution.module.line.dto.ClientPlanQueryDTO
 *  com.hikrobotics.solution.module.line.dto.LinePlanBindQueryDTO
 *  com.hikrobotics.solution.module.line.dto.PlanDTO
 *  com.hikrobotics.solution.module.line.dto.PlanQueryDTO
 *  com.hikrobotics.solution.module.line.mapper.PlanDAO
 *  com.hikrobotics.solution.module.line.mapper.PlanToLineDAO
 *  com.hikrobotics.solution.module.line.model.PlanPO
 *  com.hikrobotics.solution.module.line.model.PlanToLinePO
 *  com.hikrobotics.solution.module.line.service.IPlanService
 *  com.hikrobotics.solution.module.line.service.imp.PlanServiceImpl
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package com.hikrobotics.solution.module.line.service.imp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import com.hikrobotics.solution.module.line.mapper.PlanDAO;
import com.hikrobotics.solution.module.line.mapper.PlanToLineDAO;
import com.hikrobotics.solution.module.line.model.PlanPO;
import com.hikrobotics.solution.module.line.model.PlanToLinePO;
import com.hikrobotics.solution.module.line.service.IPlanService;
import java.io.Serializable;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlanServiceImpl
extends ServiceImpl<PlanDAO, PlanPO>
implements IPlanService {
    @Autowired
    private PlanDAO planDAO;
    @Autowired
    private PlanToLineDAO planToLineDAO;
    @Autowired
    private WebSocketHandler webSocketHandler;

    public BaseResult add(PlanDTO planDTO) {
        Long count = this.planDAO.selectCount((Wrapper)Wrappers.lambdaQuery().eq(PlanPO::getName, (Object)planDTO.getName()));
        if (count > 0L) {
            return BaseResult.build().error("20301").log("add plan error,same name", planDTO.toString());
        }
        PlanPO planPO = new PlanPO();
        BeanUtil.copyProperties((Object)planDTO, (Object)planPO, (String[])new String[0]);
        this.planDAO.insert((Object)planPO);
        return BaseResult.build();
    }

    public BaseResult del(IdQuery idQuery) {
        List planToLinePOS = this.planToLineDAO.selectList((Wrapper)Wrappers.lambdaQuery().eq(PlanToLinePO::getPlanId, (Object)idQuery.getId()));
        if (planToLinePOS.size() > 0) {
            return BaseResult.build().error("20302").log("del plan error, plan was linked", idQuery.toString());
        }
        this.planDAO.deleteById((Serializable)idQuery.getId());
        return BaseResult.build();
    }

    public BaseResult mod(PlanDTO planDTO) {
        Long count = this.planDAO.selectCount((Wrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(StringUtils.isNotBlank((CharSequence)planDTO.getName()), PlanPO::getName, (Object)planDTO.getName())).ne(PlanPO::getId, (Object)planDTO.getId()));
        if (count > 0L) {
            return BaseResult.build().error("20301").log("mod plan error, same name", planDTO.toString());
        }
        PlanPO planPO = new PlanPO();
        BeanUtil.copyProperties((Object)planDTO, (Object)planPO, (String[])new String[0]);
        this.planDAO.updateById((Object)planPO);
        List clientNos = this.planToLineDAO.selectPlanClient(planPO.getId());
        for (String clientNo : clientNos) {
            CommonMethod.sendPlanChange((WebSocketHandler)this.webSocketHandler, (String)clientNo);
        }
        return BaseResult.build();
    }

    public BaseResult search(PlanQueryDTO planQueryDTO) {
        String name = planQueryDTO.getName();
        String startTime = planQueryDTO.getStartTime();
        String endTime = planQueryDTO.getEndTime();
        LambdaQueryWrapper lambdaQuery = (LambdaQueryWrapper)Wrappers.lambdaQuery().like(StringUtils.isNotBlank((CharSequence)name), PlanPO::getName, (Object)name);
        if (StringUtils.isNotBlank((CharSequence)startTime)) {
            lambdaQuery.ge(StringUtils.isNotBlank((CharSequence)startTime), PlanPO::getCreateTime, (Object)DateUtil.parse((CharSequence)startTime, (String)"yyyy-MM-dd HH:mm:ss"));
        }
        if (StringUtils.isNotBlank((CharSequence)endTime)) {
            lambdaQuery.ge(StringUtils.isNotBlank((CharSequence)endTime), PlanPO::getCreateTime, (Object)DateUtil.parse((CharSequence)endTime, (String)"yyyy-MM-dd HH:mm:ss"));
        }
        lambdaQuery.orderByDesc(PlanPO::getCreateTime);
        if (planQueryDTO.isPaged()) {
            Page planPOPage = (Page)this.planDAO.selectPage((IPage)planQueryDTO.getPage(), (Wrapper)lambdaQuery);
            return BaseResult.build().data((Object)planPOPage);
        }
        List planPOS = this.planDAO.selectList((Wrapper)lambdaQuery);
        return BaseResult.build().data((Object)planPOS);
    }

    public BaseResult getClientBindPlan(LinePlanBindQueryDTO linePlanBindQueryDTO) {
        List webLineBindPlanResultDTOS = this.planDAO.selectPlanByLineId(linePlanBindQueryDTO.getLineId());
        System.out.println(webLineBindPlanResultDTOS);
        return BaseResult.build().data((Object)webLineBindPlanResultDTOS);
    }

    public BaseResult clientPlan(ClientPlanQueryDTO clientPlanQueryDTO) {
        List clientPlanResultDTOS = this.planDAO.selectClientPlan(clientPlanQueryDTO.getLineNo(), clientPlanQueryDTO.getFaceNo());
        return BaseResult.build().data((Object)clientPlanResultDTOS);
    }
}

