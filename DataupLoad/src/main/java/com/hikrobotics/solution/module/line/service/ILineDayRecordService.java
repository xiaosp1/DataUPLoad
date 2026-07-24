package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单 W-D 临时桩：DetectDataTaskManager 调度需要 {@code removeRecordByTime(LocalDateTime)}，
 * 工单 SCRN-1 扩展：补齐 {@link #listByTime(String)} 供大屏模块聚合剔除数。
 *
 * <p>完整方法签名按工单说明补齐（listByStartTime / listByTimeAndLineNo 等）由对应工单落地。</p>
 */
public interface ILineDayRecordService extends IService<LineDayRecord> {

    void removeRecordByTime(LocalDateTime time);

    /**
     * 工单 SCRN-1 大屏模块所需：按 time 字段精确查询（PSM 等价签名：
     * {@code listByTime(String)}，{@code eq(LineDayRecord::getTime, time)}）。
     */
    List<LineDayRecord> listByTime(String time);

    /**
     * PSM 1:1 searchLineDayRecord — 按时间范围查询 line_day_record。
     * 供 YKServiceImpl.searchDefectRecord 聚合剔除数。
     */
    List<LineDayRecord> searchLineDayRecord(com.hikrobotics.solution.module.yingke.dto.SearchDefectRecordDTO form);
}
