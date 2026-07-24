package com.hikrobotics.solution.module.detect.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.detect.mapper.DefectDayRecordMapper;
import com.hikrobotics.solution.module.detect.service.IDefectDayRecordService;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工单 W-D 临时桩：实现 DetectDataTaskManager 调度所需的
 * {@link IDefectDayRecordService#removeRecordByTime(LocalDateTime)}。
 *
 * <p>工单 SCRN-1 大屏模块：实现 {@link IDefectDayRecordService#listByStartTimeAndDefect(Set, String)}，
 * 1:1 抄自反编译 PSM DefectDayRecordServiceImpl.listByStartTimeAndDefect。</p>
 *
 * <p>工单 W-DET-01：补齐 PSM 中剩余 8 个方法。Impl 用 MyBatis-Plus LambdaQueryWrapper + baseMapper
 * 实现；{@link DefectCountDTO} 用本项目 line.dto 包已有类型（PSM 返回 PO）。</p>
 */
@Service
public class DefectDayRecordServiceImpl
       extends ServiceImpl<DefectDayRecordMapper, DefectDayRecord>
       implements IDefectDayRecordService {

    private static final Logger log = LoggerFactory.getLogger(DefectDayRecordServiceImpl.class);

    @Override
    public void removeRecordByTime(LocalDateTime time) {
        log.info("begin to delete expire defect day record.[time={}]", time);
        int count = this.baseMapper.delete(
            Wrappers.<DefectDayRecord>lambdaQuery()
                .le(DefectDayRecord::getTime, time.toLocalDate().toString()));
        log.info("end delete expire defect day record.[count={}]", count);
    }

    @Override
    public List<DefectDayRecord> listByStartTimeAndDefect(Set<String> defects, String time) {
        List<DefectDayRecord> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(defects)) {
            result.addAll(this.list(
                Wrappers.<DefectDayRecord>lambdaQuery()
                    .ge(DefectDayRecord::getTime, time)
                    .in(DefectDayRecord::getType, defects)));
        }
        return result;
    }

    // ============================== 工单 W-DET-01 ==============================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void addLineDayRecord(List<String> lineNoList, List<String> defectNameList) {
        if (CollectionUtils.isEmpty(lineNoList) || CollectionUtils.isEmpty(defectNameList)) {
            return;
        }
        // 工单 W-DET-01：hour 切片（"yyyy-MM-dd HH:00:00"），与 DefectRecordServiceImpl.handleDetectData 对齐
        String currentTime = HikDateUtil.formatLocalDate(LocalDateTime.now(), "yyyy-MM-dd HH") + ":00:00";

        // 1) 查已存在的 (time, lineNo, type) 行
        List<DefectDayRecord> savedRecord = this.list(
            Wrappers.<DefectDayRecord>lambdaQuery()
                .eq(DefectDayRecord::getTime, currentTime)
                .in(DefectDayRecord::getType, defectNameList)
                .in(DefectDayRecord::getLineNo, lineNoList));

        Map<String, Set<String>> savedLineRecordMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(savedRecord)) {
            for (DefectDayRecord r : savedRecord) {
                savedLineRecordMap
                    .computeIfAbsent(r.getLineNo(), k -> new HashSet<>())
                    .add(r.getType());
            }
        }

        // 2) 找出缺失的 (line, type) 组合，新建 count=0 的记录
        List<DefectDayRecord> addList = new ArrayList<>();
        for (String lineNo : lineNoList) {
            Set<String> existTypes = savedLineRecordMap.getOrDefault(lineNo, new HashSet<>());
            for (String defectName : defectNameList) {
                if (existTypes.contains(defectName)) {
                    continue;
                }
                addList.add(new DefectDayRecord()
                    .setTime(currentTime)
                    .setLineNo(lineNo)
                    .setType(defectName)
                    .setCount(0));
            }
        }

        // 3) PSM Impl 原代码没有 saveBatch（编译产物本身有缺陷），工单 W-DET-01 补齐
        if (!addList.isEmpty()) {
            this.saveBatch(addList);
        }
        log.info("addLineDayRecord done. currentTime={}, addSize={}", currentTime, addList.size());
    }

    @Override
    public <T> List<DefectDayRecord> listByAttribute(T value, SFunction<DefectDayRecord, T> getter) {
        return this.list(
            Wrappers.<DefectDayRecord>lambdaQuery().eq(getter, value));
    }

    @Override
    public List<DefectDayRecord> listByStartTime(String startTime) {
        return this.list(
            Wrappers.<DefectDayRecord>lambdaQuery()
                .ge(DefectDayRecord::getTime, startTime));
    }

    @Override
    public List<DefectCountDTO> searchDefectCount(String time, String lineNo, String faceNo, List<String> defects) {
        if (CollectionUtils.isEmpty(defects)) {
            return new ArrayList<>();
        }
        List<DefectDayRecord> rows = this.list(
            Wrappers.<DefectDayRecord>lambdaQuery()
                .eq(DefectDayRecord::getTime, time)
                .eq(DefectDayRecord::getLineNo, lineNo)
                .eq(DefectDayRecord::getFaceNo, faceNo)
                .in(DefectDayRecord::getType, defects));
        return aggregateToDefectCountDTO(rows);
    }

    @Override
    public List<DefectCountDTO> searchDefectCount(LocalDateTime start, LocalDateTime end,
                                                  String lineNo, String faceNo, List<String> defects) {
        if (CollectionUtils.isEmpty(defects)) {
            return new ArrayList<>();
        }
        List<DefectDayRecord> rows = this.list(
            Wrappers.<DefectDayRecord>lambdaQuery()
                .le(DefectDayRecord::getTime, HikDateUtil.formatLocalDate(end))
                .ge(DefectDayRecord::getTime, HikDateUtil.formatLocalDate(start))
                .eq(DefectDayRecord::getLineNo, lineNo)
                .eq(DefectDayRecord::getFaceNo, faceNo)
                .in(DefectDayRecord::getType, defects));
        return aggregateToDefectCountDTO(rows);
    }

    @Override
    public List<DefectDayRecord> listByLineAndTime(String lineNo, String faceNo,
                                                   LocalDateTime start, LocalDateTime end) {
        return this.list(
            Wrappers.<DefectDayRecord>lambdaQuery()
                .le(DefectDayRecord::getTime, HikDateUtil.formatLocalDate(end))
                .ge(DefectDayRecord::getTime, HikDateUtil.formatLocalDate(start))
                .eq(DefectDayRecord::getLineNo, lineNo)
                .eq(DefectDayRecord::getFaceNo, faceNo));
    }

    @Override
    public int removeByType(List<String> types) {
        if (CollectionUtils.isEmpty(types)) {
            return 0;
        }
        return this.baseMapper.delete(
            Wrappers.<DefectDayRecord>lambdaQuery()
                .in(DefectDayRecord::getType, types));
    }

    @Override
    public List<DefectDayRecord> listBetween(String startTime, String endTime) {
        return this.list(
            Wrappers.<DefectDayRecord>lambdaQuery()
                .ge(DefectDayRecord::getTime, startTime)
                .le(DefectDayRecord::getTime, endTime)
                .orderByDesc(DefectDayRecord::getTime));
    }

    // ============================== 私有辅助 ==============================

    /**
     * 将 defect_day_record 行按 (time, type) 聚合为 DefectCountDTO。
     *
     * <p>每个 {@code (time, type)} 分组求 count 之和；showFlag 留 null（保持 DTO 默认）。</p>
     */
    private static List<DefectCountDTO> aggregateToDefectCountDTO(List<DefectDayRecord> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return new ArrayList<>();
        }
        Map<String, DefectCountDTO> bucket = new HashMap<>();
        for (DefectDayRecord r : rows) {
            String key = r.getTime() + "|" + Objects.toString(r.getType(), "");
            DefectCountDTO dto = bucket.computeIfAbsent(key, k -> new DefectCountDTO()
                .setTime(r.getTime())
                .setType(r.getType())
                .setCount(0));
            Integer cur = dto.getCount();
            Integer add = r.getCount() == null ? 0 : r.getCount();
            dto.setCount((cur == null ? 0 : cur) + add);
        }
        return bucket.values().stream()
            .sorted((a, b) -> {
                int c = Objects.compare(b.getTime(), a.getTime(), String::compareTo);
                if (c != 0) {
                    return c;
                }
                return Objects.compare(
                    Objects.toString(a.getType(), ""),
                    Objects.toString(b.getType(), ""),
                    String::compareTo);
            })
            .collect(Collectors.toList());
    }
}
