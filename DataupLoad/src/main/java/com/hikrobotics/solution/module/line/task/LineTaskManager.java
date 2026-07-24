package com.hikrobotics.solution.module.line.task;

import cn.hutool.core.collection.CollectionUtil;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.service.ILineService;
import com.hikrobotics.solution.module.line.service.IStateChangeService;
import com.hikrobotics.solution.module.line.service.IStateStatisticService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 班次统计定时任务（1:1 抄 PSM LineTaskManager）。
 * <p>每日 08:01 和 20:01 统计上一班次数据，凌晨 2 点清理过期数据。</p>
 */
@Component
public class LineTaskManager {

    private static final Logger log = LoggerFactory.getLogger(LineTaskManager.class);

    @Value("${data-retention-time.line-state:30}")
    private Integer lineStateRetentionTime;

    @Autowired
    private ILineService lineService;

    @Autowired
    private IStateStatisticService stateStatisticService;

    @Autowired
    private IStateChangeService stateChangeService;

    @Scheduled(cron = "0 1 8,20 * * ?")
    public void getStatisticData() {
        LocalTime now = LocalTime.now();
        LocalDateTime start;
        LocalDateTime end;

        if (now.getHour() == 8) {
            // 统计前一天的 20:00 → 当天 08:00
            start = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.parse("20:00:00"));
            end = LocalDateTime.of(LocalDate.now(), LocalTime.parse("08:00:00"));
        } else {
            // 统计当天 08:00 → 20:00
            start = LocalDateTime.of(LocalDate.now(), LocalTime.parse("08:00:00"));
            end = LocalDateTime.of(LocalDate.now(), LocalTime.parse("20:00:00"));
        }

        Set<Integer> lineIds = new HashSet<>();
        List<Line> lines = this.lineService.listLine();
        for (Line line : lines) {
            lineIds.add(line.getId());
        }

        if (CollectionUtil.isNotEmpty(lineIds)) {
            List<?> statistics = this.stateChangeService.getStateStatistics(lineIds, start, end);
            if (CollectionUtil.isNotEmpty(statistics)) {
                this.stateStatisticService.saveStatisticBatch(
                    (List<com.hikrobotics.solution.module.line.entity.StateStatistic>) statistics);
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void clearExpireStateData() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(this.lineStateRetentionTime);
        this.stateStatisticService.removeBefore(expireTime);
        this.stateChangeService.removeBefore(expireTime);
    }
}
