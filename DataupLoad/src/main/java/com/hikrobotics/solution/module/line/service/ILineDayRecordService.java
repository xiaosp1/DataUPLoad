package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单 W-D 临时桩：DetectDataTaskManager 调度需要 {@code removeRecordByTime(LocalDateTime)}，
 * 工单 SCRN-1 扩展：补齐 {@link #listByTime(String)} 供大屏模块聚合剔除数。
 *
 * <p>工单 W-DET-02：补齐 PSM 反编译 4 个缺失方法
 * （{@link #listByStartTime(String)} /
 * {@link #listByTimeAndLineNo(LocalDateTime, String, String)} /
 * {@link #listOfLineBetween(LocalDateTime, LocalDateTime, String, String)} /
 * {@link #listLineDayBetween(String, String)}），接口签名与 PSM
 * {@code com.hikrobotics.solution.module.detect.service.ILineDayRecordService}
 * 1:1 对齐。</p>
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

    /**
     * PSM 1:1 listByStartTime — 按 time 字段 {@code >=} 查询（小时粒度入口，
     * 入参是 {@code HikDateUtil.formatLocalDate(time, "yyyy-MM-dd HH")} 这类
     * 整点字符串）。
     */
    List<LineDayRecord> listByStartTime(String time);

    /**
     * PSM 1:1 listByTimeAndLineNo — 按整点 time + lineNo + faceNo 三键精确查询，
     * 返回唯一一条 {@code LineDayRecord}（小时粒度的某产线/面汇总）。
     */
    LineDayRecord listByTimeAndLineNo(LocalDateTime time, String lineNo, String faceNo);

    /**
     * PSM 1:1 listOfLineBetween — 按产线+面维度查询某段时间内
     * 的 {@code LineDayRecord}，内部按天归并成 00:00:00 / 23:59:59。
     */
    List<LineDayRecord> listOfLineBetween(LocalDateTime start, LocalDateTime end, String lineNo, String faceNo);

    /**
     * PSM 1:1 listLineDayBetween — 按日维度（{@code yyyy-MM-dd HH:mm:ss} 字符串）
     * 查询一段时间内所有产线/面的 {@code LineDayRecord}，并按 time 倒序。
     */
    List<LineDayRecord> listLineDayBetween(String startTime, String endTime);
}
