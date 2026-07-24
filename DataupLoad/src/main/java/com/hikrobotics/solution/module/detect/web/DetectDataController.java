package com.hikrobotics.solution.module.detect.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.validation.ValidateUtils;
import com.hikrobotics.solution.framework.common.validation.group.GroupA;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.detect.dto.DetectDataUploadDTO;
import com.hikrobotics.solution.module.detect.dto.StatusRecordDTO;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.detect.service.IDefectDayRecordService;
import com.hikrobotics.solution.module.detect.service.IDefectRecordService;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import com.hikrobotics.solution.module.detect.util.ExcelUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
 * <p>工单 W-DET-05c：</p>
 * <ul>
 *   <li>{@code /web/detect/statistic/export} 由 PSM 风格的「service 委托 + form 绑定」改为
 *       「controller 直接调用 ExcelUtils.exportToExcel + @RequestParam 入参」；
 *       service 路径仍保留（{@code defectRecordService.handleStatisticDataExport}），
 *       但本工单绕开它（避免 W-B03 占位 UOE）。</li>
 *   <li>新增 {@code /web/detect/list} 端点：按 lineNo / faceNo / startTime / endTime 条件
 *       分页查询 {@link DefectDayRecord}，返回 {@link IPage}。</li>
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

   @PostMapping("/client/data/detect")
   public BaseResult upload(@RequestBody @Validated DetectDataUploadDTO form) {
      return this.defectRecordService.handleDetectData(form);
   }

   @PostMapping("/client/data/status")
   public BaseResult status(@RequestBody List<StatusRecordDTO> list) {
      // DTO → Entity 转换，方便 service 复用 PSM 风格的 PO 操作；
      // 入参 group=GroupA 校验 @NotEmpty/@NotNull 字段
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
    * 工单 W-DET-05c：缺陷统计 Excel 导出。
    *
    * <p>与 PSM 反编译产物的差异（任务硬性要求，不再走 service 委托）：</p>
    * <ul>
    *   <li>PSM 用 {@code @Validated ExportDefectStatisticForm} 接收 startTime/endTime/lineNo；
    *       本工单改为 {@code @RequestParam} 直接接收 5 个参数：
    *       {@code startTime / endTime / lineNo / faceNo / defects（可选）}。</li>
    *   <li>PSM 由 service 端 {@code DefectRecordServiceImpl.handleStatisticDataExport}
    *       实现 150+ 行的 sheet × table（早/晚班）组装逻辑；
    *       本工单由 controller 直接调 {@link ExcelUtils#exportToExcel}，数据源走
    *       {@link IDefectDayRecordService#listByLineAndTime}（行级），不做班次拆分。</li>
    *   <li>Content-Type / 文件名 / Content-Disposition 由 {@link ExcelUtils#exportToExcel} 统一设置。</li>
    * </ul>
    *
    * @param resp      HTTP 响应；{@link ExcelUtils#exportToExcel} 直接写入
    * @param startTime 起始时间（{@code yyyy-MM-dd HH:mm:ss} 或 ISO-8601）
    * @param endTime   截止时间（同上）
    * @param lineNo    产线号（可选；为 null/空 时不按 lineNo 过滤）
    * @param faceNo    面号（可选；为 null/空 时不按 faceNo 过滤）
    * @param defects   缺陷名集合（可选；{@code ?defects=A&defects=B} 多值；为 null/空 时不过滤）
    */
   @GetMapping("/web/detect/statistic/export")
   public void exportStatisticData(HttpServletResponse resp,
                                    @RequestParam(name = "startTime") String startTime,
                                    @RequestParam(name = "endTime") String endTime,
                                    @RequestParam(name = "lineNo", required = false) String lineNo,
                                    @RequestParam(name = "faceNo", required = false) String faceNo,
                                    @RequestParam(name = "defects", required = false) List<String> defects) {
      log.info("exportStatisticData start. startTime={}, endTime={}, lineNo={}, faceNo={}, defects={}",
         startTime, endTime, lineNo, faceNo, defects);

      // 1) 时间解析：兼容 "yyyy-MM-dd HH:mm:ss" 与 ISO-8601（"yyyy-MM-ddTHH:mm:ss"）
      LocalDateTime startDt = parseLocalDateTime(startTime);
      LocalDateTime endDt = parseLocalDateTime(endTime);
      if (startDt == null || endDt == null) {
         throw new IllegalArgumentException(
            "startTime/endTime must be yyyy-MM-dd HH:mm:ss or ISO-8601");
      }

      // 2) 数据源：单产线 + 单面 + 时间区间；lineNo/faceNo 空时 listByLineAndTime 走 lambdaQuery 时过滤掉空条件
      //    （IDefectDayRecordService.listByLineAndTime 等值过滤，若 lineNo 为 null 会返回空集）
      //    → 此处先按 lineNo/faceNo 是否为空选择不同入口
      List<DefectDayRecord> rows;
      if (StringUtils.isBlank(lineNo) && StringUtils.isBlank(faceNo)) {
         // 全部产线 → 用 listBetween
         rows = this.defectDayRecordService.listBetween(
            HikDateUtil.formatLocalDate(startDt),
            HikDateUtil.formatLocalDate(endDt));
      } else {
         rows = this.defectDayRecordService.listByLineAndTime(
            StringUtils.isBlank(lineNo) ? "" : lineNo,
            StringUtils.isBlank(faceNo) ? "" : faceNo,
            startDt, endDt);
      }

      // 3) 内存层过滤 defects（listByLineAndTime / listBetween 不接 defects 入参）
      if (CollectionUtils.isNotEmpty(defects)) {
         rows = rows.stream()
            .filter(r -> r.getType() != null && defects.contains(r.getType()))
            .collect(Collectors.toList());
      }

      // 4) 构造表头 / 数据
      List<List<String>> headers = buildExportHeaders();
      List<List<Object>> data = buildExportRows(rows);

      // 5) 文件名 + 调用 ExcelUtils
      String fileName = "缺陷统计_" + startTime.replaceAll("[^0-9]", "").substring(0, 8)
         + "_" + endTime.replaceAll("[^0-9]", "").substring(0, 8);
      // 同列相邻同值合并：lineNo / faceNo / 时间 / type
      List<String> mergeColumns = Arrays.asList("产线", "面", "时间", "缺陷类型");

      ExcelUtils.exportToExcel(resp, headers, data, mergeColumns, fileName);
   }

   /**
    * 工单 W-DET-05c：缺陷日记录条件分页查询。
    *
    * <p>按 {@code lineNo / faceNo / startTime / endTime} 任意子集条件分页查
    * {@link DefectDayRecord}，返回 {@link IPage}。PSM 反编译产物没有此端点，
    * 是 DataupLoad 自增端点（用于大屏 / 后台 list 页面）。</p>
    *
    * @param lineNo    产线号（可选）
    * @param faceNo    面号（可选）
    * @param startTime 起始时间（可选；{@code yyyy-MM-dd HH:mm:ss} 或 ISO-8601；与 endTime 同时给才生效）
    * @param endTime   截止时间（可选；同上）
    * @param page      页码（默认 1）
    * @param size      每页大小（默认 20）
    * @return {@link BaseResult#data(IPage)} 包裹的分页结果
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
    * 工单 W-DET-03：保留 PSM 反编译端点；{@code @RequestParam} 全部显式声明 {@code name}
    * 属性，避免依赖编译参数 {@code -parameters}。
    */
   @Deprecated
   @GetMapping("/web/detect/realtime")
   public BaseResult getRealtimeData(@RequestParam(name = "lineNo") String lineNo,
                                     @RequestParam(name = "faceNo") String faceNo) {
      return this.defectRecordService.handleRealtimeDetectDataSearch(lineNo, faceNo);
   }

   // ============================================================
   // 工单 W-DET-05c 私有辅助
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
         // fallback：spring 默认 ISO-8601
         try {
            return LocalDateTime.parse(raw);
         } catch (RuntimeException ex2) {
            log.warn("parseLocalDateTime fail raw={}", raw);
            return null;
         }
      }
   }

   /**
    * 构造导出表头（两行表头模拟 PSM 风格：分组行 + 子标题行）。
    *
    * <p>为简化 controller 内联实现，本工单采用单行表头：
    * {@code [产线, 面, 时间, 缺陷类型, 数量, 更新时间, 创建时间]}。
    * 若后续工单需要恢复 PSM 早/晚班双行表头，可把 {@code buildExportHeaders} 改为
    * {@code List.of(List.of("白班","产线"), List.of("白班",""), ...)} 形式。</p>
    */
   private static List<List<String>> buildExportHeaders() {
      List<String> header = Arrays.asList("产线", "面", "时间", "缺陷类型", "数量", "更新时间", "创建时间");
      return Collections.singletonList(header);
   }

   /**
    * 把 {@link DefectDayRecord} 行转换为 Excel 行（每行 List&lt;Object&gt;）。
    */
   private static List<List<Object>> buildExportRows(List<DefectDayRecord> rows) {
      if (rows == null || rows.isEmpty()) {
         return Collections.emptyList();
      }
      List<List<Object>> out = new ArrayList<>(rows.size());
      for (DefectDayRecord r : rows) {
         List<Object> row = new ArrayList<>(7);
         row.add(r.getLineNo());
         row.add(r.getFaceNo());
         row.add(r.getTime());
         row.add(r.getType());
         row.add(r.getCount());
         row.add(r.getUpdateTime());
         row.add(r.getCreateTime());
         out.add(row);
      }
      return out;
   }
}
