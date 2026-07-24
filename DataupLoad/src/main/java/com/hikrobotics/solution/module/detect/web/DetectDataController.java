package com.hikrobotics.solution.module.detect.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.validation.ValidateUtils;
import com.hikrobotics.solution.framework.common.validation.group.GroupA;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.detect.dto.DetectDataUploadDTO;
import com.hikrobotics.solution.module.detect.dto.ExportDefectStatisticForm;
import com.hikrobotics.solution.module.detect.dto.StatusRecordDTO;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.detect.service.IDefectDayRecordService;
import com.hikrobotics.solution.module.detect.service.IDefectRecordService;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单 W-B03 detect 模块 HTTP 入口；
 * DetectDataUploadDTO / StatusRecordDTO 校验通过 ValidateUtils + @Validated 兜底，
 * 与 PSM 同款：/client/** 走 hik-security 白名单，不需要鉴权。
 *
 * <p>工单 W-DET-03：保留 PSM 反编译端点；{@code @RequestParam} 全部显式声明 {@code name}
 * 属性，避免依赖编译参数 {@code -parameters}。</p>
 *
 * <p>工单 W-DET-05c：新增 {@code /web/detect/list} 端点：按 lineNo / faceNo / startTime / endTime 条件
 * 分页查询 {@link DefectDayRecord}，返回 {@link IPage}。</p>
 *
 * <p>工单 W-DET-08（返工）：</p>
 * <ul>
 *   <li>{@code /web/detect/statistic/export} 1:1 改回 PSM 风格：
 *       {@code @Validated ExportDefectStatisticForm} 表单绑定 + service 委托，删除 controller 内
 *       {@code buildExportHeaders} / {@code buildExportRows} / 直接调 ExcelUtils 的逻辑；</li>
 *   <li>PSM 同款 5 个端点 1:1 保留（uploadDetectData / searchDetectDetail / exportStatisticData /
 *       getRealtimeData / receiveStatus）；W-DET-05c 自增的 {@code /web/detect/list} 保留。</li>
 *   <li>DataupLoad 端点名称 vs PSM：</li>
 *   <li>　- PSM {@code POST /client/data/detect uploadDetectData} → 当前 {@code upload}（W-B03
 *       沿用，不改）；</li>
 *   <li>　- PSM {@code POST /client/data/status receiveStatus(List<StatusRecordPO>)} → 当前
 *       {@code status(List<StatusRecordDTO>)}（DTO 兜底校验，W-B03 沿用）；</li>
 *   <li>　- PSM {@code GET /web/detect/detail} / {@code GET /web/detect/statistic/export} /
 *       {@code GET /web/detect/realtime} → 1:1 对齐（{@code @RequestParam} 显式 name）。</li>
 * </ul>
 */
@RestController
@RequestMapping
public class DetectDataController {

   private static final Logger log = LoggerFactory.getLogger(DetectDataController.class);

   @Autowired
   private IDefectRecordService defectRecordService;

   @Autowired
   private IStatusRecordService statusRecordService;

   @Autowired
   private IDefectDayRecordService defectDayRecordService;

   /**
    * PSM 同款：{@code POST /client/data/detect} → {@link DefectRecordServiceImpl#handleDetectData}。
    */
   @PostMapping("/client/data/detect")
   public BaseResult upload(@RequestBody @Validated DetectDataUploadDTO form) {
      return this.defectRecordService.handleDetectData(form);
   }

   /**
    * PSM 同款：{@code POST /client/data/status} → {@link IStatusRecordService#receiveStatus}。
    *
    * <p>DataupLoad 改造：DTO → Entity 转换，方便 service 复用 PSM 风格的 PO 操作；
    * 入参 group=GroupA 校验 @NotEmpty/@NotNull 字段。</p>
    */
   @PostMapping("/client/data/status")
   public BaseResult status(@RequestBody List<StatusRecordDTO> list) {
      List<com.hikrobotics.solution.module.detect.entity.StatusRecord> records = list.stream()
         .map(StatusRecordDTO::toEntity)
         .toList();
      ValidateUtils.validateEntity("DataController.status", records, new Class[]{GroupA.class});
      return this.statusRecordService.receiveStatus(records);
   }

   /**
    * 工单 W-DET-03：保留 PSM 反编译端点；{@code @RequestParam} 全部显式声明 {@code name}
    * 属性，避免依赖编译参数 {@code -parameters}。
    */
   @GetMapping("/web/detect/detail")
   public BaseResult searchDetectDetail(@RequestParam(name = "faceId") Integer faceId,
                                        @RequestParam(name = "startTime") String startTime,
                                        @RequestParam(name = "endTime") String endTime) {
      return this.defectRecordService.handleDetectDetailSearch(faceId, startTime, endTime);
   }

   /**
    * 工单 W-DET-08（返工）：缺陷统计 Excel 导出 1:1 改回 PSM 风格。
    *
    * <p>PSM 反编译产物 1:1：
    * {@code @GetMapping("/web/detect/statistic/export") public void exportStatisticData(HttpServletResponse resp, @Validated ExportDefectStatisticForm form)}。
    * 完全由 {@link IDefectRecordService#handleStatisticDataExport} 负责组装多 sheet × 多 table 的 xlsx，
    * controller 不再自建表头 / 不再直接调 {@code ExcelUtils}。</p>
    *
    * <p>{@code @Validated} 触发 {@link ExportDefectStatisticForm#getStartTime()} /
    * {@link ExportDefectStatisticForm#getEndTime()} 上的 {@code @NotBlank} 校验：
    * startTime / endTime 缺失时 {@code ConstraintViolationException}（错误 10500），
    * 避免 W-DET-07 发现的 {@code faceNo=""} / 缺失时间透传到 SQL 的问题。</p>
    */
   @GetMapping("/web/detect/statistic/export")
   public void exportStatisticData(HttpServletResponse resp,
                                    @Validated ExportDefectStatisticForm form) {
      this.defectRecordService.handleStatisticDataExport(resp, form);
   }

   /**
    * 工单 W-DET-05c：缺陷日记录条件分页查询。
    *
    * <p>按 {@code lineNo / faceNo / startTime / endTime} 任意子集条件分页查
    * {@link DefectDayRecord}，返回 {@link IPage}。PSM 反编译产物没有此端点，
    * 是 DataupLoad 自增端点（用于大屏 / 后台 list 页面）。</p>
    */
   @GetMapping("/web/detect/list")
   public BaseResult detectList(@RequestParam(name = "lineNo", required = false) String lineNo,
                                @RequestParam(name = "faceNo", required = false) String faceNo,
                                @RequestParam(name = "startTime", required = false) String startTime,
                                @RequestParam(name = "endTime", required = false) String endTime,
                                @RequestParam(name = "page", defaultValue = "1") Integer page,
                                @RequestParam(name = "size", defaultValue = "20") Integer size) {
      log.info("detectList lineNo={}, faceNo={}, startTime={}, endTime={}, page={}, size={}",
         lineNo, faceNo, startTime, endTime, page, size);

      int pageNum = (page == null || page < 1) ? 1 : page;
      int pageSize = (size == null || size < 1) ? 20 : size;

      Page<DefectDayRecord> pageReq = new Page<>(pageNum, pageSize);
      var wrapper = Wrappers.<DefectDayRecord>lambdaQuery();
      if (StringUtils.isNotBlank(lineNo)) {
         wrapper.eq(DefectDayRecord::getLineNo, lineNo);
      }
      if (StringUtils.isNotBlank(faceNo)) {
         wrapper.eq(DefectDayRecord::getFaceNo, faceNo);
      }
      if (StringUtils.isNotBlank(startTime)) {
         LocalDateTime startDt = parseLocalDateTime(startTime);
         if (startDt != null) {
            wrapper.ge(DefectDayRecord::getTime, HikDateUtil.formatLocalDate(startDt));
         }
      }
      if (StringUtils.isNotBlank(endTime)) {
         LocalDateTime endDt = parseLocalDateTime(endTime);
         if (endDt != null) {
            wrapper.le(DefectDayRecord::getTime, HikDateUtil.formatLocalDate(endDt));
         }
      }
      wrapper.orderByDesc(DefectDayRecord::getTime);

      IPage<DefectDayRecord> result = this.defectDayRecordService.page(pageReq, wrapper);
      return BaseResult.build().data(result);
   }

   /**
    * PSM 同款（带 {@code @Deprecated}）：{@code GET /web/detect/realtime} →
    * {@link IDefectRecordService#handleRealtimeDetectDataSearch}。
    */
   @Deprecated
   @GetMapping("/web/detect/realtime")
   public BaseResult getRealtimeData(@RequestParam(name = "lineNo") String lineNo,
                                     @RequestParam(name = "faceNo") String faceNo) {
      return this.defectRecordService.handleRealtimeDetectDataSearch(lineNo, faceNo);
   }

   // ============================================================
   // 私有辅助
   // ============================================================

   /**
    * 解析 {@code yyyy-MM-dd HH:mm:ss} / ISO-8601 / ISO-8601 with offset 字符串为
    * {@link LocalDateTime}。解析失败返回 {@code null}（由 caller 抛 IAE）。
    */
   private static LocalDateTime parseLocalDateTime(String raw) {
      if (raw == null || raw.isEmpty()) {
         return null;
      }
      String normalized = raw.contains("T") ? raw.replace('T', ' ') : raw;
      try {
         return HikDateUtil.transformTime(normalized);
      } catch (RuntimeException ex) {
         try {
            return LocalDateTime.parse(raw);
         } catch (RuntimeException ex2) {
            log.warn("parseLocalDateTime fail raw={}", raw);
            return null;
         }
      }
   }
}
