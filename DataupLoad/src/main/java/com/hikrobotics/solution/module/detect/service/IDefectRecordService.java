package com.hikrobotics.solution.module.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.detect.dto.ExportDefectStatisticForm;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.detect.entity.DefectRecord;
import com.hikrobotics.solution.module.detect.dto.DetectDataUploadDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 1:1 抄自反编译 IDefectRecordService；
 * PO 类在本项目里换成了 entity 包下的实体。
 */
public interface IDefectRecordService extends IService<DefectRecord> {
   BaseResult handleDetectData(DetectDataUploadDTO form);

   BaseResult handleDetectDetailSearch(Integer faceId, String starTime, String endTime);

   void handleStatisticDataExport(HttpServletResponse resp, ExportDefectStatisticForm form);

   BaseResult handleRealtimeDetectDataSearch(String lineNo, String faceNo);

   List<DefectDayRecord> searchDefectRecord(SearchDefectRecordDTO cond);
}
