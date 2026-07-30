/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.hutool.core.collection.CollectionUtil
 *  com.hikrobotics.solution.module.line.service.ILineService
 *  com.hikrobotics.solution.module.line.service.IStateChangeService
 *  com.hikrobotics.solution.module.line.service.IStateStatisticService
 *  com.hikrobotics.solution.module.line.task.LineTaskManager
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.scheduling.annotation.Scheduled
 *  org.springframework.stereotype.Component
 */
package com.hikrobotics.solution.module.line.task;

import cn.hutool.core.collection.CollectionUtil;
import com.hikrobotics.solution.module.line.service.ILineService;
import com.hikrobotics.solution.module.line.service.IStateChangeService;
import com.hikrobotics.solution.module.line.service.IStateStatisticService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LineTaskManager {
    private static final Logger log = LoggerFactory.getLogger(LineTaskManager.class);
    @Value(value="${data-retention-time.line-state:30}")
    private Integer lineStateRetentionTime;
    @Autowired
    private ILineService lineService;
    @Autowired
    private IStateStatisticService stateStatisticService;
    @Autowired
    private IStateChangeService stateChangeService;

    @Scheduled(cron="0 1 8,20 * * ?")
    public void getStatisticData() {
        List statistics;
        LocalDateTime end;
        LocalDateTime start;
        HashSet lineIds = new HashSet();
        this.lineService.listLine().forEach(line -> lineIds.add(line.getId()));
        LocalTime now = LocalTime.now();
        if (now.getHour() == 8) {
            start = LocalDateTime.of(LocalDate.now().minusDays(1L), LocalTime.parse("20:00:00"));
            end = LocalDateTime.of(LocalDate.now(), LocalTime.parse("08:00:00"));
        } else {
            start = LocalDateTime.of(LocalDate.now(), LocalTime.parse("08:00:00"));
            end = LocalDateTime.of(LocalDate.now(), LocalTime.parse("20:00:00"));
        }
        if (CollectionUtil.isNotEmpty(lineIds) && CollectionUtil.isNotEmpty((Collection)(statistics = this.stateChangeService.getStateStatistics(lineIds, start, end)))) {
            this.stateStatisticService.saveStatisticBatch(statistics);
        }
    }

    @Scheduled(cron="0 0 2 * * ?")
    public void clearExpireStateData() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(this.lineStateRetentionTime.intValue());
        this.stateStatisticService.removeBefore(expireTime);
        this.stateChangeService.removeBefore(expireTime);
    }
}

