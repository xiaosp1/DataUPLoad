package com.hikrobotics.solution.module.line.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import com.hikrobotics.solution.module.line.service.ILineDayRecordService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单 W-DET-03 line 模块 day-record HTTP 入口。
 *
 * <p>对应 PSM 反编译 {@code com.hikrobotics.solution.module.line.web.LineDayRecordController}
 * （PSM 反编译产物中本类不独立存在 —— LineDayRecord 相关 endpoint 在原 PSM 也未单独成 controller；
 * 本工单按 W-DET-03 spec 新建独立 controller，URL 命名 {@code /web/line/day-record/**}）。</p>
 *
 * <p>endpoint 与 service 方法 1:1 对齐：</p>
 * <ul>
 *   <li>{@code POST /web/line/day-record/list-by-start} → {@link ILineDayRecordService#listByStartTime}</li>
 *   <li>{@code POST /web/line/day-record/list-of-line-between} → {@link ILineDayRecordService#listOfLineBetween}</li>
 *   <li>{@code POST /web/line/day-record/list-line-day-between} → {@link ILineDayRecordService#listLineDayBetween}</li>
 * </ul>
 *
 * <p>按工单 W-DET-03 要求，{@code @RequestParam} 全部显式声明 {@code name} 属性，
 * 避免依赖 Spring 隐式反射（编译产物里很多 PSM 反编译类没保留参数名）。</p>
 *
 * <p>不做 service 层改动；本类仅做"参数 → service 调用 → BaseResult.data(...)"薄封装。</p>
 *
 * <p>时间入参约定：{@code startTime}/{@code endTime} 是 ISO-8601 字符串
 * （{@code yyyy-MM-dd'T'HH:mm:ss}），通过 Spring 的 {@link DateTimeFormat} 解析为
 * {@link LocalDateTime}，与 service 签名一致（service 接口用 LocalDateTime）。</p>
 */
@RestController
@RequestMapping("/web/line/day-record")
public class LineDayRecordController {

   private static final Logger log = LoggerFactory.getLogger(LineDayRecordController.class);

   @Autowired
   private ILineDayRecordService lineDayRecordService;

   /**
    * 按 time 下界字符串查询（"yyyy-MM-dd HH" 整点 / "yyyy-MM-dd HH:mm:ss" 全格式）。
    *
    * <p>对应 service 方法 {@link ILineDayRecordService#listByStartTime}。</p>
    *
    * <p>service 内部走 {@code ge(time, startTime)}；本 controller 直接透传字符串。</p>
    */
   @PostMapping("/list-by-start")
   public BaseResult listByStartTime(@RequestParam(name = "startTime") String startTime) {
      log.debug("listByStartTime startTime={}", startTime);
      List<LineDayRecord> data = this.lineDayRecordService.listByStartTime(startTime);
      return BaseResult.build().data(data);
   }

   /**
    * 按产线 + 面 + 时间范围查询 line_day_record。
    *
    * <p>对应 service 方法 {@link ILineDayRecordService#listOfLineBetween}。</p>
    *
    * <p>service 内部按日归并成 00:00:00 / 23:59:59：
    * {@code statisticStartTime = formatLocalDate(start, "yyyy-MM-dd") + " 00:00:00"}、
    * {@code statisticEndTime = formatLocalDate(end, "yyyy-MM-dd HH") + " 23:59:59"}。</p>
    *
    * <p>字段：</p>
    * <ul>
    *   <li>{@code start} — ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss} 字符串（service 内部仅取日期部分作下界）</li>
    *   <li>{@code end} — ISO-8601 字符串（service 内部取到小时作上界）</li>
    *   <li>{@code lineNo} — 产线号（精确匹配）</li>
    *   <li>{@code faceNo} — 面编号（精确匹配）</li>
    * </ul>
    */
   @PostMapping("/list-of-line-between")
   public BaseResult listOfLineBetween(
           @RequestParam(name = "start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String start,
           @RequestParam(name = "end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String end,
           @RequestParam(name = "lineNo") String lineNo,
           @RequestParam(name = "faceNo") String faceNo) {
      LocalDateTime startDt = parseLocalDateTime(start);
      LocalDateTime endDt = parseLocalDateTime(end);
      log.debug("listOfLineBetween start={}, end={}, lineNo={}, faceNo={}", startDt, endDt, lineNo, faceNo);
      List<LineDayRecord> data = this.lineDayRecordService.listOfLineBetween(startDt, endDt, lineNo, faceNo);
      return BaseResult.build().data(data);
   }

   /**
    * 按日维度（{@code yyyy-MM-dd HH:mm:ss} 字符串）查询一段时间内所有产线/面的
    * {@code LineDayRecord}，并按 time 倒序。
    *
    * <p>对应 service 方法 {@link ILineDayRecordService#listLineDayBetween}。</p>
    *
    * <p>字段：</p>
    * <ul>
    *   <li>{@code startTime} — 入参是已格式化好的字符串（"yyyy-MM-dd HH:mm:ss"），作为下界 ge</li>
    *   <li>{@code endTime} — 入参是已格式化好的字符串（"yyyy-MM-dd HH:mm:ss"），作为上界 le</li>
    * </ul>
    */
   @PostMapping("/list-line-day-between")
   public BaseResult listLineDayBetween(@RequestParam(name = "startTime") String startTime,
                                         @RequestParam(name = "endTime") String endTime) {
      log.debug("listLineDayBetween startTime={}, endTime={}", startTime, endTime);
      List<LineDayRecord> data = this.lineDayRecordService.listLineDayBetween(startTime, endTime);
      return BaseResult.build().data(data);
   }

   // ============================== 私有辅助 ==============================

   /**
    * 把 ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss} 字符串解析为 {@link LocalDateTime}。
    *
    * <p>优先用 {@link HikDateUtil#transformTime(String)}（PSM 风格，默认 pattern
    * {@code "yyyy-MM-dd HH:mm:ss"}），如果解析失败则降级到 Spring 自带的 ISO 解析。</p>
    */
   private static LocalDateTime parseLocalDateTime(String raw) {
      if (raw == null || raw.isEmpty()) {
         return null;
      }
      // HikDateUtil.transformTime 默认 pattern 是 "yyyy-MM-dd HH:mm:ss"，
      // HTTP 入参 ISO-8601 含 'T'，先做一次 strip
      String normalized = raw.contains("T") ? raw.replace('T', ' ') : raw;
      try {
         return HikDateUtil.transformTime(normalized);
      } catch (RuntimeException ex) {
         log.warn("parseLocalDateTime fail raw={}, fallback to ISO", raw, ex);
         return LocalDateTime.parse(raw);
      }
   }
}
