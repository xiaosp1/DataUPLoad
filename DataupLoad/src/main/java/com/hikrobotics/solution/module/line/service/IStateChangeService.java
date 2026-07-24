package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm;
import com.hikrobotics.solution.module.line.entity.StateChange;
import com.hikrobotics.solution.module.line.entity.StateStatistic;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 状态变更服务接口（W-B05 StateChange）。
 *
 * <p>1:1 抄自反编译 PSM IStateChangeService，仅替换 PO → entity 命名（StateChangePO → StateChange
 * / StateStatisticPO → StateStatistic）。</p>
 */
public interface IStateChangeService extends IService<StateChange> {

    /**
     * 状态变更统计查询（PSM handleStateStatisticSearch）。
     */
    BaseResult handleStateStatisticSearch(SearchStateStatisticForm form);

    /**
     * 给定线体集合 + 时间区间，计算每条线的 okTime / errorTime 统计（PSM getStateStatistics）。
     */
    List<StateStatistic> getStateStatistics(Set<Integer> lineIds, LocalDateTime start, LocalDateTime end);

    /**
     * 删除指定时间之前的 state_change 记录（PSM removeBefore，定时清理用）。
     */
    void removeBefore(LocalDateTime time);
}
