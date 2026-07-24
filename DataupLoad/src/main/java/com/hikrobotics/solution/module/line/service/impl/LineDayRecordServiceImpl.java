package com.hikrobotics.solution.module.line.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import com.hikrobotics.solution.module.line.mapper.LineDayRecordMapper;
import com.hikrobotics.solution.module.line.service.ILineDayRecordService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 工单 W-D 临时桩：实现 DetectDataTaskManager 调度所需的
 * {@link ILineDayRecordService#removeRecordByTime(LocalDateTime)}。
 *
 * <p>反编译 PSM 中 LineDayRecordPO.time 是 String，且写入时使用
 * {@code HikDateUtil.formatLocalDate(time)}（默认格式
 * {@code "yyyy-MM-dd HH:mm:ss"}）；工单 W-DET-02 修正
 * {@link #removeRecordByTime(LocalDateTime)} 的删除上界字符串与 PSM 对齐。</p>
 *
 * <p>工单 SCRN-1 扩展：补齐 {@link ILineDayRecordService#listByTime(String)}，
 * 1:1 抄自反编译 PSM LineDayRecordServiceImpl.listByTime。</p>
 *
 * <p>工单 W-DET-02：补齐 4 个 PSM 缺失方法
 * （listByStartTime / listByTimeAndLineNo / listOfLineBetween /
 * listLineDayBetween），实现细节与 PSM 反编译一致。</p>
 */
@Service
public class LineDayRecordServiceImpl
       extends ServiceImpl<LineDayRecordMapper, LineDayRecord>
       implements ILineDayRecordService {

    @Override
    public void removeRecordByTime(LocalDateTime time) {
        // PSM 等价：删除上界用 HikDateUtil.formatLocalDate(time)（"yyyy-MM-dd HH:mm:ss"），
        // 与写入时格式一致，按字典序比较可正确删除 <= time 的全部记录；
        // 旧实现用 time.toLocalDate().toString()（"yyyy-MM-dd"）会把跨天记录一并删掉。
        this.remove(Wrappers.<LineDayRecord>lambdaQuery()
            .le(LineDayRecord::getTime, HikDateUtil.formatLocalDate(time)));
    }

    @Override
    public List<LineDayRecord> listByTime(String time) {
        return this.list(Wrappers.<LineDayRecord>lambdaQuery()
            .eq(LineDayRecord::getTime, time));
    }

    @Override
    public List<LineDayRecord> searchLineDayRecord(
            com.hikrobotics.solution.module.yingke.dto.SearchDefectRecordDTO form) {
        return this.list(Wrappers.<LineDayRecord>lambdaQuery()
            .ge(LineDayRecord::getTime, form.getStartTime().toLocalDate().toString())
            .le(LineDayRecord::getTime, form.getEndTime().toLocalDate().toString()));
    }

    @Override
    public List<LineDayRecord> listByStartTime(String time) {
        // PSM 1:1: time 形参是 "yyyy-MM-dd HH" 整点字符串（含 :00:00 后缀），
        // 用 ge(time) 即可匹配当前小时及之后的所有记录。
        return this.list(Wrappers.<LineDayRecord>lambdaQuery()
            .ge(LineDayRecord::getTime, time));
    }

    @Override
    public LineDayRecord listByTimeAndLineNo(LocalDateTime time, String lineNo, String faceNo) {
        // PSM 1:1: statisticTime = formatLocalDate(time, "yyyy-MM-dd HH") + ":00:00"
        String statisticTime = HikDateUtil.formatLocalDate(time, "yyyy-MM-dd HH") + ":00:00";
        return this.getOne(Wrappers.<LineDayRecord>lambdaQuery()
            .eq(LineDayRecord::getTime, statisticTime)
            .eq(LineDayRecord::getLineNo, lineNo)
            .eq(LineDayRecord::getFaceNo, faceNo));
    }

    @Override
    public List<LineDayRecord> listOfLineBetween(LocalDateTime start, LocalDateTime end, String lineNo, String faceNo) {
        // PSM 1:1: statisticStartTime = formatLocalDate(start, "yyyy-MM-dd") + " 00:00:00"
        //      statisticEndTime   = formatLocalDate(end,   "yyyy-MM-dd HH") + " 23:59:59"
        String statisticStartTime = HikDateUtil.formatLocalDate(start, "yyyy-MM-dd") + " 00:00:00";
        String statisticEndTime   = HikDateUtil.formatLocalDate(end,   "yyyy-MM-dd HH") + " 23:59:59";
        return this.list(Wrappers.<LineDayRecord>lambdaQuery()
            .between(LineDayRecord::getTime, statisticStartTime, statisticEndTime)
            .eq(LineDayRecord::getLineNo, lineNo)
            .eq(LineDayRecord::getFaceNo, faceNo));
    }

    @Override
    public List<LineDayRecord> listLineDayBetween(String startTime, String endTime) {
        // PSM 1:1: 入参是已格式化好的字符串（"yyyy-MM-dd HH:mm:ss"），按 time 倒序。
        return this.list(Wrappers.<LineDayRecord>lambdaQuery()
            .ge(LineDayRecord::getTime, startTime)
            .le(LineDayRecord::getTime, endTime)
            .orderByDesc(LineDayRecord::getTime));
    }
}
