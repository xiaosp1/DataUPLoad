package com.hikrobotics.solution.module.detect.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 工单 SCRN-1 补齐：DataupLoad 原本没有 defect_day_record 的服务接口（只有桩
 * {@link com.hikrobotics.solution.module.detect.service.impl.DefectDayRecordServiceImpl}），
 * 这里按反编译 PSM {@code IDefectDayRecordService} 接口形态建立接口。
 *
 * <p>实体类型用本项目已有的 {@link DefectDayRecord}（PSM 是 {@code DefectDayRecordPO}）。</p>
 *
 * <p>工单 W-DET-01：补齐 PSM 中 8 个缺失方法（保留已经实现的
 * {@link #removeRecordByTime(LocalDateTime)} 和 {@link #listByStartTimeAndDefect(Set, String)}）：
 * <ul>
 *   <li>{@link #addLineDayRecord(List, List)}</li>
 *   <li>{@link #listByAttribute(Object, SFunction)}</li>
 *   <li>{@link #listByStartTime(String)}</li>
 *   <li>{@link #searchDefectCount(String, String, String, List)}</li>
 *   <li>{@link #searchDefectCount(LocalDateTime, LocalDateTime, String, String, List)}</li>
 *   <li>{@link #listByLineAndTime(String, String, LocalDateTime, LocalDateTime)}</li>
 *   <li>{@link #removeByType(List)}</li>
 *   <li>{@link #listBetween(String, String)}</li>
 * </ul>
 * </p>
 *
 * <p>注意：方法名 + 参数列表 1:1 抄 PSM；返回类型按工单 W-DET-01 规范（{@code searchDefectCount}
 * 返回聚合后的 {@link DefectCountDTO}，{@code removeByType} 返回删除行数 int，
 * {@code addLineDayRecord} 返回 void）；Impl 内部用 MyBatis-Plus LambdaQueryWrapper 实现。</p>
 */
public interface IDefectDayRecordService extends IService<DefectDayRecord> {

   /**
    * 任务 SCRN-1：{@code HikDateUtil.formatLocalDate(now(), "yyyy-MM-dd HH") + ":00:00"} 已经在
    * DefectRecordServiceImpl 中实现；本接口仅声明 cron 调度删除入口。
    */
   void removeRecordByTime(LocalDateTime time);

   /**
    * 工单 SCRN-1 大屏模块所需：
    * 按缺陷名集合 + 时间下界，查询 defect_day_record 行（用于大屏聚合缺陷数）。
    *
    * <p>PSM 实现语义：{@code time >= startTime}（PSM 注释里也写 {@code ge(time)}），
    * 若 defects 为空直接返回空集合。</p>
    */
   List<DefectDayRecord> listByStartTimeAndDefect(Set<String> defects, String time);

   /**
    * 工单 W-DET-01：批量补齐产线下某天（"yyyy-MM-dd"）尚未在 defect_day_record 中出现的
    * 缺陷记录（count=0），用于产线/缺陷初次登记。
    *
    * <p>PSM 签名 1:1：{@code addLineDayRecord(List<String> lineNoList, List<String> defectNameList)}，
    * 返回类型按工单改为 {@code void}（PSM 是 boolean，PSM Impl 体内未实际保存，仅返回 true）。</p>
    *
    * @param lineNoList     产线号集合
    * @param defectNameList 缺陷名集合
    */
   void addLineDayRecord(List<String> lineNoList, List<String> defectNameList);

   /**
    * 工单 W-DET-01：按任意字段（{@code SFunction}）等值查询 defect_day_record 行。
    *
    * <p>PSM 签名 1:1：{@code <T> listByAttribute(T value, SFunction<DefectDayRecord, T> getter)}。</p>
    */
   <T> List<DefectDayRecord> listByAttribute(T value, SFunction<DefectDayRecord, T> getter);

   /**
    * 工单 W-DET-01：查询 {@code time >= startTime} 的 defect_day_record 行（小时粒度下界）。
    *
    * <p>PSM 签名 1:1：{@code listByStartTime(String startTime)}。</p>
    */
   List<DefectDayRecord> listByStartTime(String startTime);

   /**
    * 工单 W-DET-01：按精确 time + lineNo + faceNo + defectName 集合查询，**返回聚合后的
    * {@link DefectCountDTO}**（每个 {@code (time, type)} 一个 DTO，{@code count} 为同一
    * (time,type) 的 count 之和）。
    *
    * <p>PSM 签名 1:1：{@code searchDefectCount(String time, String lineNo, String faceNo, List<String> defects)}，
    * 返回类型按工单改为 {@code List<DefectCountDTO>}（PSM 返回 List&lt;DefectDayRecordPO&gt;）。</p>
    */
   List<DefectCountDTO> searchDefectCount(String time, String lineNo, String faceNo, List<String> defects);

   /**
    * 工单 W-DET-01：按时间范围 + lineNo + faceNo + defectName 集合查询，**返回聚合后的
    * {@link DefectCountDTO}**（按 {@code (time, type)} 聚合）。
    *
    * <p>PSM 签名 1:1：
    * {@code searchDefectCount(LocalDateTime start, LocalDateTime end, String lineNo, String faceNo, List<String> defects)}，
    * 返回类型按工单改为 {@code List<DefectCountDTO>}（PSM 返回 List&lt;DefectDayRecordPO&gt;）。</p>
    */
   List<DefectCountDTO> searchDefectCount(LocalDateTime start, LocalDateTime end,
                                          String lineNo, String faceNo, List<String> defects);

   /**
    * 工单 W-DET-01：单产线 + 面 + 时间范围查询 defect_day_record 行。
    *
    * <p>PSM 签名 1:1：
    * {@code listByLineAndTime(String lineNo, String faceNo, LocalDateTime start, LocalDateTime end)}。</p>
    */
   List<DefectDayRecord> listByLineAndTime(String lineNo, String faceNo, LocalDateTime start, LocalDateTime end);

   /**
    * 工单 W-DET-01：按缺陷类型批量删除 defect_day_record 行，**返回删除行数**。
    *
    * <p>PSM 签名 1:1：{@code removeByType(List<String> types)}，返回类型按工单改为 {@code int}
    * （PSM 返回 Boolean）。types 为空时返回 0。</p>
    */
   int removeByType(List<String> types);

   /**
    * 工单 W-DET-01：按时间闭区间 {@code [startTime, endTime]} 查询 defect_day_record，
    * 按 {@code time} 降序返回。
    *
    * <p>PSM 签名 1:1：{@code listBetween(String startTime, String endTime)}。</p>
    */
   List<DefectDayRecord> listBetween(String startTime, String endTime);
}
