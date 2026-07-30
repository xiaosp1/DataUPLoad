/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.hutool.core.date.LocalDateTimeUtil
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.baomidou.mybatisplus.core.toolkit.CollectionUtils
 *  com.baomidou.mybatisplus.core.toolkit.Wrappers
 *  com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Multimap
 *  com.hikrobotics.solution.framework.common.base.BaseResult
 *  com.hikrobotics.solution.framework.util.HikDateUtil
 *  com.hikrobotics.solution.module.detect.enums.DeviceStatus
 *  com.hikrobotics.solution.module.detect.model.StatusRecordPO
 *  com.hikrobotics.solution.module.detect.service.IStatusRecordService
 *  com.hikrobotics.solution.module.detect.util.TimeRange
 *  com.hikrobotics.solution.module.detect.util.TimeRange$TimePattern
 *  com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm
 *  com.hikrobotics.solution.module.line.event.StateChangeEvent
 *  com.hikrobotics.solution.module.line.mapper.StateChangeDAO
 *  com.hikrobotics.solution.module.line.model.LinePO
 *  com.hikrobotics.solution.module.line.model.StateChangePO
 *  com.hikrobotics.solution.module.line.model.StateStatisticPO
 *  com.hikrobotics.solution.module.line.service.ILineService
 *  com.hikrobotics.solution.module.line.service.IStateChangeService
 *  com.hikrobotics.solution.module.line.service.IStateStatisticService
 *  com.hikrobotics.solution.module.line.service.imp.StateChangeServiceImpl
 *  org.assertj.core.util.Lists
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.context.event.EventListener
 *  org.springframework.scheduling.annotation.Async
 *  org.springframework.stereotype.Service
 */
package com.hikrobotics.solution.module.line.service.imp;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.detect.enums.DeviceStatus;
import com.hikrobotics.solution.module.detect.model.StatusRecordPO;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import com.hikrobotics.solution.module.detect.util.TimeRange;
import com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm;
import com.hikrobotics.solution.module.line.event.StateChangeEvent;
import com.hikrobotics.solution.module.line.mapper.StateChangeDAO;
import com.hikrobotics.solution.module.line.model.LinePO;
import com.hikrobotics.solution.module.line.model.StateChangePO;
import com.hikrobotics.solution.module.line.model.StateStatisticPO;
import com.hikrobotics.solution.module.line.service.ILineService;
import com.hikrobotics.solution.module.line.service.IStateChangeService;
import com.hikrobotics.solution.module.line.service.IStateStatisticService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class StateChangeServiceImpl
extends ServiceImpl<StateChangeDAO, StateChangePO>
implements IStateChangeService {
    @Autowired
    private ILineService lineService;
    @Autowired
    private IStatusRecordService statusRecordService;
    @Autowired
    private IStateStatisticService stateStatisticService;

    public BaseResult handleStateStatisticSearch(SearchStateStatisticForm form) {
        Set faceIds = form.getFaceIds();
        ArrayList lines = new ArrayList();
        if (CollectionUtils.isNotEmpty((Collection)faceIds)) {
            lines.addAll(this.lineService.listByIds((Collection)faceIds));
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
        List statistics = this.stateStatisticService.listDailyStatisticDataBetween(faceIds, start, end);
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
        ConcurrentHashMap sortStatisticByLineAndTime = new ConcurrentHashMap();
        statistics.forEach(statistic -> {
            Integer lineId = statistic.getLineId();
            Map sortStatisticByTime = sortStatisticByLineAndTime.getOrDefault(lineId, Maps.newHashMap());
            sortStatisticByTime.put(statistic.getStatisticTime(), statistic);
            sortStatisticByLineAndTime.put(lineId, sortStatisticByTime);
        });
        ((Stream)lines.stream().parallel()).forEach(line -> {
            TimeRange range = new TimeRange(form.getStartTime().atStartOfDay(), form.getEndTime().atStartOfDay().plusDays(1L), TimeRange.TimePattern.MM_DD);
            Map sortByTime = sortStatisticByLineAndTime.getOrDefault(line.getId(), new HashMap());
            while (range.hasNext()) {
                LocalDateTime time = range.next();
                LocalDateTime aShift = LocalDateTime.of(time.toLocalDate(), eight);
                StateStatisticPO aShiftData = sortByTime.getOrDefault(aShift, new StateStatisticPO().setLineId(line.getId()).setStatisticTime(aShift));
                aShiftData.setLineNo(line.getLineNo()).setFaceNo(line.getFaceNo()).setTime(time);
                sortByTime.put(aShift, aShiftData);
                LocalDateTime bShift = LocalDateTime.of(time.toLocalDate(), twenty);
                StateStatisticPO bShiftData = sortByTime.getOrDefault(bShift, new StateStatisticPO().setLineId(line.getId()).setStatisticTime(bShift));
                bShiftData.setLineNo(line.getLineNo()).setFaceNo(line.getFaceNo()).setTime(time);
                sortByTime.put(bShift, bShiftData);
            }
            sortStatisticByLineAndTime.put(line.getId(), sortByTime);
        });
        List data = sortStatisticByLineAndTime.values().stream().flatMap(s -> s.values().stream()).toList();
        return BaseResult.build().ok().data(data);
    }

    public List<StateStatisticPO> getStateStatistics(Set<Integer> lineIds, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper qw = (LambdaQueryWrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().in(StateChangePO::getLineId, lineIds)).between(StateChangePO::getChangeTime, (Object)start, (Object)end);
        HashMultimap sortChangeByLine = HashMultimap.create();
        this.list((Wrapper)qw).forEach(arg_0 -> StateChangeServiceImpl.lambda$getStateStatistics$4((Multimap)sortChangeByLine, arg_0));
        ArrayList<StateStatisticPO> statistics = new ArrayList<StateStatisticPO>();
        this.statusRecordService.listClientStatus(lineIds).forEach(arg_0 -> StateChangeServiceImpl.lambda$getStateStatistics$5((Multimap)sortChangeByLine, end, start, statistics, arg_0));
        sortChangeByLine.keySet().forEach(arg_0 -> StateChangeServiceImpl.lambda$getStateStatistics$6((Multimap)sortChangeByLine, start, end, statistics, arg_0));
        return statistics;
    }

    public void removeBefore(LocalDateTime time) {
        LambdaQueryWrapper qw = (LambdaQueryWrapper)Wrappers.lambdaQuery().le(StateChangePO::getChangeTime, (Object)time);
        this.remove((Wrapper)qw);
    }

    @Async
    @EventListener(value={StateChangeEvent.class})
    public void handleStateChange(StateChangeEvent event) {
        LinePO line = this.lineService.getByLineNoAndFaceNo(event.getLineNo(), event.getFaceNo());
        LambdaQueryWrapper qw = (LambdaQueryWrapper)((LambdaQueryWrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(StateChangePO::getLineId, (Object)line.getId())).le(StateChangePO::getChangeTime, (Object)event.getTime())).last("limit 1");
        StateChangePO record = (StateChangePO)this.getOne((Wrapper)qw);
        if (record == null || !record.getType().equals(event.getStatus().getValue())) {
            record = new StateChangePO().setChangeTime(event.getTime()).setLineId(line.getId()).setType(event.getStatus().getValue());
            this.save((Object)record);
        }
    }

    private static /* synthetic */ void lambda$getStateStatistics$6(Multimap sortChangeByLine, LocalDateTime start, LocalDateTime end, List statistics, Integer line) {
        Collection changeRecords = sortChangeByLine.get((Object)line);
        if (CollectionUtils.isNotEmpty((Collection)changeRecords)) {
            ArrayList sortedRecord = Lists.newArrayList((Iterable)changeRecords);
            sortedRecord.sort(Comparator.comparing(StateChangePO::getChangeTime));
            StateChangePO firstChange = (StateChangePO)sortedRecord.get(0);
            StateChangePO lastChange = (StateChangePO)sortedRecord.get(sortedRecord.size() - 1);
            if (!firstChange.getChangeTime().isEqual(start)) {
                DeviceStatus state = firstChange.getType().equals(DeviceStatus.OUTLINE.getValue()) ? DeviceStatus.ONLINE : DeviceStatus.OUTLINE;
                StateChangePO startState = new StateChangePO().setChangeTime(start).setType(state.getValue());
                sortedRecord.add(startState);
            }
            if (!lastChange.getChangeTime().isEqual(end)) {
                sortedRecord.add(new StateChangePO().setChangeTime(end).setType(lastChange.getType()));
            }
            sortedRecord.sort(Comparator.comparing(StateChangePO::getChangeTime));
            long offlineTime = 0L;
            long onlineTime = 0L;
            for (int i = 0; i < sortedRecord.size() - 1; ++i) {
                StateChangePO s1 = (StateChangePO)sortedRecord.get(i);
                StateChangePO s2 = (StateChangePO)sortedRecord.get(i + 1);
                long diff = LocalDateTimeUtil.between((LocalDateTime)s1.getChangeTime(), (LocalDateTime)s2.getChangeTime(), (ChronoUnit)ChronoUnit.MILLIS);
                if (s1.getType().equals(DeviceStatus.OUTLINE.getValue())) {
                    offlineTime += diff;
                    continue;
                }
                onlineTime += diff;
            }
            StateStatisticPO statistic = new StateStatisticPO().setLineId(line).setErrorTime(offlineTime).setOkTime(onlineTime).setStatisticTime(start);
            statistics.add(statistic);
        }
    }

    private static /* synthetic */ void lambda$getStateStatistics$5(Multimap sortChangeByLine, LocalDateTime end, LocalDateTime start, List statistics, StatusRecordPO status) {
        if (!sortChangeByLine.containsKey((Object)status.getLineId())) {
            if (status.getCreateTime().isBefore(end)) {
                LocalDateTime stateChgTime = HikDateUtil.transformTime((String)status.getTime());
                if (stateChgTime.isBefore(start)) {
                    sortChangeByLine.put((Object)status.getLineId(), (Object)new StateChangePO().setType(status.getStatus()).setChangeTime(start));
                } else if (stateChgTime.isAfter(end)) {
                    DeviceStatus ds = status.getStatus().equals(DeviceStatus.OUTLINE.getValue()) ? DeviceStatus.ONLINE : DeviceStatus.OUTLINE;
                    sortChangeByLine.put((Object)status.getLineId(), (Object)new StateChangePO().setType(ds.getValue()).setChangeTime(start));
                } else {
                    sortChangeByLine.put((Object)status.getLineId(), (Object)new StateChangePO().setType(status.getStatus()).setChangeTime(stateChgTime));
                }
            } else {
                statistics.add(new StateStatisticPO().setErrorTime(0L).setOkTime(0L).setLineId(status.getLineId()).setStatisticTime(start));
            }
        }
    }

    private static /* synthetic */ void lambda$getStateStatistics$4(Multimap sortChangeByLine, StateChangePO state) {
        sortChangeByLine.put((Object)state.getLineId(), (Object)state);
    }
}

