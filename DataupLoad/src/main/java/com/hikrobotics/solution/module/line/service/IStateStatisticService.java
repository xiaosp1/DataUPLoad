package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.line.entity.StateStatistic;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface IStateStatisticService extends IService<StateStatistic> {
    void saveStatisticBatch(List<StateStatistic> statistics);
    List<StateStatistic> listDailyStatisticDataBetween(Set<Integer> lineIds, LocalDateTime start, LocalDateTime end);
    void removeBefore(LocalDateTime time);
}
