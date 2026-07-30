/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.baomidou.mybatisplus.core.toolkit.CollectionUtils
 *  com.baomidou.mybatisplus.core.toolkit.Wrappers
 *  com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
 *  com.hikrobotics.solution.module.line.mapper.StateStatisticDAO
 *  com.hikrobotics.solution.module.line.model.StateStatisticPO
 *  com.hikrobotics.solution.module.line.service.IStateStatisticService
 *  com.hikrobotics.solution.module.line.service.imp.StateStatisticServiceImpl
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package com.hikrobotics.solution.module.line.service.imp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.module.line.mapper.StateStatisticDAO;
import com.hikrobotics.solution.module.line.model.StateStatisticPO;
import com.hikrobotics.solution.module.line.service.IStateStatisticService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StateStatisticServiceImpl
extends ServiceImpl<StateStatisticDAO, StateStatisticPO>
implements IStateStatisticService {
    @Autowired
    private StateStatisticDAO stateStatisticDAO;

    public void saveStatisticBatch(List<StateStatisticPO> statistics) {
        if (CollectionUtils.isNotEmpty(statistics)) {
            this.stateStatisticDAO.insertBatch(statistics);
        }
    }

    public List<StateStatisticPO> listDailyStatisticDataBetween(Set<Integer> lineIds, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper qw = (LambdaQueryWrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().in(StateStatisticPO::getLineId, lineIds)).between(StateStatisticPO::getStatisticTime, (Object)start, (Object)end);
        return this.list((Wrapper)qw);
    }

    public void removeBefore(LocalDateTime time) {
        LambdaQueryWrapper qw = (LambdaQueryWrapper)Wrappers.lambdaQuery().le(StateStatisticPO::getStatisticTime, (Object)time);
        this.remove((Wrapper)qw);
    }
}

