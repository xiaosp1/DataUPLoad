package com.hikrobotics.solution.module.line.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.module.line.entity.StateStatistic;
import com.hikrobotics.solution.module.line.mapper.StateStatisticMapper;
import com.hikrobotics.solution.module.line.service.IStateStatisticService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 班次统计 Service（1:1 抄 PSM StateStatisticServiceImpl）。
 * <p>使用 MP saveBatch 替代 PSM 的 stateStatisticDAO.insertBatch。</p>
 */
@Service
public class StateStatisticServiceImpl
    extends ServiceImpl<StateStatisticMapper, StateStatistic>
    implements IStateStatisticService {

    @Override
    public void saveStatisticBatch(List<StateStatistic> statistics) {
        if (CollectionUtils.isNotEmpty(statistics)) {
            this.saveBatch(statistics);
        }
    }

    @Override
    public List<StateStatistic> listDailyStatisticDataBetween(Set<Integer> lineIds,
                                                               LocalDateTime start,
                                                               LocalDateTime end) {
        LambdaQueryWrapper<StateStatistic> qw = Wrappers.<StateStatistic>lambdaQuery()
            .in(StateStatistic::getLineId, lineIds)
            .between(StateStatistic::getStatisticTime, start, end);
        return this.list(qw);
    }

    @Override
    public void removeBefore(LocalDateTime time) {
        LambdaQueryWrapper<StateStatistic> qw = Wrappers.<StateStatistic>lambdaQuery()
            .le(StateStatistic::getStatisticTime, time);
        this.remove(qw);
    }
}
