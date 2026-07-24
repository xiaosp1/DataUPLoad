package com.hikrobotics.solution.module.line.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.detect.enums.DeviceStatus;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.entity.StateChange;
import com.hikrobotics.solution.module.line.entity.StateStatistic;
import com.hikrobotics.solution.module.line.event.StateChangeEvent;
import com.hikrobotics.solution.module.line.mapper.StateChangeMapper;
import com.hikrobotics.solution.module.line.service.ILineService;
import com.hikrobotics.solution.module.line.service.IStateChangeService;
import com.hikrobotics.solution.module.line.service.IStateStatisticService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 状态变更服务实现（W-B05 StateChange）。
 *
 * <p>逻辑 1:1 抄自反编译 PSM StateChangeServiceImpl，差异如下：</p>
 * <ul>
 *   <li>PSM StateChangePO / StateStatisticPO / LinePO（PSM 反编译命名，DataupLoad 已统一为 entity/Line）→ DataupLoad entity 同名类（StateChange /
 *       StateStatistic / Line）</li>
 *   <li>PSM StateChangeDAO → DataupLoad StateChangeMapper</li>
 *   <li>PSM StatusRecordPO → DataupLoad StatusRecord</li>
 *   <li>PSM detect.util.TimeRange → framework TimeRangeUtil.TimeRange
 *       （TimePattern 在内嵌枚举中定义）</li>
 * </ul>
 */
@Service
public class StateChangeServiceImpl
        extends ServiceImpl<StateChangeMapper, StateChange>
        implements IStateChangeService {

    @Autowired
    private ILineService lineService;
    @Autowired
    private IStatusRecordService statusRecordService;
    @Autowired
    private IStateStatisticService stateStatisticService;

    @Override
    public BaseResult handleStateStatisticSearch(SearchStateStatisticForm form) {
        Set<Integer> faceIds = form.getFaceIds();
        List<Line> lines = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(faceIds)) {
            lines.addAll(this.lineService.listByIds(faceIds));
        } else {
            this.lineService.listLine().forEach(line -> {
                faceIds.add(line.getId());
                lines.add(line);
            });
        }
        if (CollectionUtils.isEmpty(lines)) {
            return BaseResult.build().ok();
        }

        LocalTime eight = LocalTime.parse("08:00:00");
        LocalTime twenty = LocalTime.parse("20:00:00");
        LocalDateTime start = LocalDateTime.of(form.getStartTime(), eight);
        LocalDateTime end = LocalDateTime.of(form.getEndTime().plusDays(1L), twenty);

        List<StateStatistic> statistics =
                this.stateStatisticService.listDailyStatisticDataBetween(faceIds, start, end);

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        if (form.getEndTime().isEqual(today) && now.getHour() >= 8) {
            LocalDateTime endOfCurrShift;
            LocalDateTime startOfCurrShift;
            if (now.getHour() < 20) {
                startOfCurrShift = LocalDateTime.of(today, eight);
                endOfCurrShift = LocalDateTime.of(today, twenty);
            } else {
                startOfCurrShift = LocalDateTime.of(today, twenty);
                endOfCurrShift = LocalDateTime.of(today.plusDays(1L), eight);
            }
            endOfCurrShift = endOfCurrShift.isAfter(LocalDateTime.now()) ? LocalDateTime.now() : startOfCurrShift;
            statistics.addAll(this.getStateStatistics(faceIds, startOfCurrShift, endOfCurrShift));
        }

        // 按 lineId 分组，每个 lineId 下再按 statisticTime 索引
        ConcurrentHashMap<Integer, Map<LocalDateTime, StateStatistic>> sortStatisticByLineAndTime = new ConcurrentHashMap<>();
        statistics.forEach(statistic -> {
            Integer lineId = statistic.getLineId();
            Map<LocalDateTime, StateStatistic> sortStatisticByTime =
                    sortStatisticByLineAndTime.getOrDefault(lineId, Maps.newHashMap());
            sortStatisticByTime.put(statistic.getStatisticTime(), statistic);
            sortStatisticByLineAndTime.put(lineId, sortStatisticByTime);
        });

        // 对每个 line，按天步进填充其班次（A 班 08:00 / B 班 20:00）槽位
        lines.stream().parallel().forEach(line -> {
            Map<LocalDateTime, StateStatistic> sortByTime =
                    sortStatisticByLineAndTime.getOrDefault(line.getId(), new HashMap<>());
            LocalDate cursor = form.getStartTime();
            LocalDate endDate = form.getEndTime();
            while (!cursor.isAfter(endDate)) {
                LocalDateTime time = cursor.atStartOfDay();
                LocalDateTime aShift = LocalDateTime.of(cursor, eight);
                StateStatistic aShiftData = sortByTime.getOrDefault(
                        aShift, new StateStatistic().setLineId(line.getId()).setStatisticTime(aShift));
                aShiftData.setLineNo(line.getLineNo()).setFaceNo(line.getFaceNo()).setTime(time);
                sortByTime.put(aShift, aShiftData);

                LocalDateTime bShift = LocalDateTime.of(cursor, twenty);
                StateStatistic bShiftData = sortByTime.getOrDefault(
                        bShift, new StateStatistic().setLineId(line.getId()).setStatisticTime(bShift));
                bShiftData.setLineNo(line.getLineNo()).setFaceNo(line.getFaceNo()).setTime(time);
                sortByTime.put(bShift, bShiftData);

                cursor = cursor.plusDays(1);
            }
            sortStatisticByLineAndTime.put(line.getId(), sortByTime);
        });

        List<StateStatistic> data = sortStatisticByLineAndTime.values().stream()
                .flatMap(s -> s.values().stream())
                .toList();
        return BaseResult.build().ok().data(data);
    }

    @Override
    public List<StateStatistic> getStateStatistics(Set<Integer> lineIds, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<StateChange> qw = Wrappers.<StateChange>lambdaQuery()
                .in(StateChange::getLineId, lineIds)
                .between(StateChange::getChangeTime, start, end);
        HashMultimap<Integer, StateChange> sortChangeByLine = HashMultimap.create();
        this.list(qw).forEach(state -> sortChangeByLine.put(state.getLineId(), state));

        List<StateStatistic> statistics = new ArrayList<>();
        this.statusRecordService.listClientStatus(lineIds).forEach(status -> {
            if (!sortChangeByLine.containsKey(status.getLineId())) {
                if (status.getCreateTime().isBefore(end)) {
                    LocalDateTime stateChgTime = HikDateUtil.transformTime(status.getTime());
                    if (stateChgTime.isBefore(start)) {
                        sortChangeByLine.put(status.getLineId(),
                                new StateChange().setType(status.getStatus()).setChangeTime(start));
                    } else if (stateChgTime.isAfter(end)) {
                        DeviceStatus ds = status.getStatus().equals(DeviceStatus.OUTLINE.getValue())
                                ? DeviceStatus.ONLINE : DeviceStatus.OUTLINE;
                        sortChangeByLine.put(status.getLineId(),
                                new StateChange().setType(ds.getValue()).setChangeTime(start));
                    } else {
                        sortChangeByLine.put(status.getLineId(),
                                new StateChange().setType(status.getStatus()).setChangeTime(stateChgTime));
                    }
                } else {
                    statistics.add(new StateStatistic()
                            .setErrorTime(0L)
                            .setOkTime(0L)
                            .setLineId(status.getLineId())
                            .setStatisticTime(start));
                }
            }
        });

        sortChangeByLine.keySet().forEach(line -> {
            List<StateChange> changeRecords = new ArrayList<>(sortChangeByLine.get(line));
            if (CollectionUtils.isNotEmpty(changeRecords)) {
                changeRecords.sort(Comparator.comparing(StateChange::getChangeTime));
                StateChange firstChange = changeRecords.get(0);
                StateChange lastChange = changeRecords.get(changeRecords.size() - 1);
                if (!firstChange.getChangeTime().isEqual(start)) {
                    DeviceStatus state = firstChange.getType().equals(DeviceStatus.OUTLINE.getValue())
                            ? DeviceStatus.ONLINE : DeviceStatus.OUTLINE;
                    changeRecords.add(new StateChange().setChangeTime(start).setType(state.getValue()));
                }
                if (!lastChange.getChangeTime().isEqual(end)) {
                    changeRecords.add(new StateChange().setChangeTime(end).setType(lastChange.getType()));
                }
                changeRecords.sort(Comparator.comparing(StateChange::getChangeTime));
                long offlineTime = 0L;
                long onlineTime = 0L;
                for (int i = 0; i < changeRecords.size() - 1; i++) {
                    StateChange s1 = changeRecords.get(i);
                    StateChange s2 = changeRecords.get(i + 1);
                    long diff = LocalDateTimeUtil.between(s1.getChangeTime(), s2.getChangeTime(), ChronoUnit.MILLIS);
                    if (s1.getType().equals(DeviceStatus.OUTLINE.getValue())) {
                        offlineTime += diff;
                    } else {
                        onlineTime += diff;
                    }
                }
                statistics.add(new StateStatistic()
                        .setLineId(line)
                        .setErrorTime(offlineTime)
                        .setOkTime(onlineTime)
                        .setStatisticTime(start));
            }
        });
        return statistics;
    }

    @Override
    public void removeBefore(LocalDateTime time) {
        LambdaQueryWrapper<StateChange> qw = Wrappers.<StateChange>lambdaQuery()
                .le(StateChange::getChangeTime, time);
        this.remove(qw);
    }

    /**
     * 异步监听 StateChangeEvent，写入 state_change 表。
     * 与 PSM 行为一致：取该线最后一条 change_record，若与事件 type 不同或不存在则写入。
     */
    @Async
    @EventListener(StateChangeEvent.class)
    public void handleStateChange(StateChangeEvent event) {
        Line line = this.lineService.getByLineNoAndFaceNo(event.getLineNo(), event.getFaceNo());
        LambdaQueryWrapper<StateChange> qw = Wrappers.<StateChange>lambdaQuery()
                .eq(StateChange::getLineId, line.getId())
                .le(StateChange::getChangeTime, event.getTime())
                .last("limit 1");
        StateChange record = this.getOne(qw);
        if (record == null || !record.getType().equals(event.getStatus().getValue())) {
            record = new StateChange()
                    .setChangeTime(event.getTime())
                    .setLineId(line.getId())
                    .setType(event.getStatus().getValue());
            this.save(record);
        }
    }
}
