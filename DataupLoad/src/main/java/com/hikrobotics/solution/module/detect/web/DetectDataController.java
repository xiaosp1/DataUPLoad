package com.hikrobotics.solution.module.detect.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.validation.ValidateUtils;
import com.hikrobotics.solution.framework.common.validation.group.GroupA;
import com.hikrobotics.solution.module.detect.dto.DetectDataUploadDTO;
import com.hikrobotics.solution.module.detect.dto.ExportDefectStatisticForm;
import com.hikrobotics.solution.module.detect.dto.StatusRecordDTO;
import com.hikrobotics.solution.module.detect.service.IDefectRecordService;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import jakarta.servlet.http.HttpServletResponse;
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
 */
@RestController
@RequestMapping
public class DetectDataController {

   private static final Logger log = LoggerFactory.getLogger(DetectDataController.class);

   @Autowired
   private IDefectRecordService defectRecordService;

   @Autowired
   private IStatusRecordService statusRecordService;

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

   @GetMapping("/web/detect/statistic/export")
   public void exportStatisticData(HttpServletResponse resp,
                                    @Validated ExportDefectStatisticForm form) {
      this.defectRecordService.handleStatisticDataExport(resp, form);
   }

   @Deprecated
   @GetMapping("/web/detect/realtime")
   public BaseResult getRealtimeData(@RequestParam(name = "lineNo") String lineNo,
                                     @RequestParam(name = "faceNo") String faceNo) {
      return this.defectRecordService.handleRealtimeDetectDataSearch(lineNo, faceNo);
   }
}
