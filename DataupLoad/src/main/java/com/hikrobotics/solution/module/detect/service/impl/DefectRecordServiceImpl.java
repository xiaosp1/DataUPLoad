package com.hikrobotics.solution.module.detect.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.defect.service.ILineDefectTypeService;
import com.hikrobotics.solution.module.detect.dto.DetectDataUploadDTO;
import com.hikrobotics.solution.module.detect.dto.ExportDefectStatisticForm;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.detect.entity.DefectRecord;
import com.hikrobotics.solution.module.detect.mapper.DefectDayRecordMapper;
import com.hikrobotics.solution.module.detect.mapper.DefectRecordMapper;
import com.hikrobotics.solution.module.detect.service.IDefectRecordService;
import com.hikrobotics.solution.module.detect.service.SearchDefectRecordDTO;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import com.hikrobotics.solution.module.line.dto.RealTimeDetectData;
import com.hikrobotics.solution.module.line.dto.TodayDetectDataDTO;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import com.hikrobotics.solution.module.line.mapper.LineDayRecordMapper;
import com.hikrobotics.solution.module.line.service.ILineService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工单 W-B03：handleDetectData() 1:1 翻译自反编译 DefectRecordServiceImpl.handleDetectData()。
 *
 * <p>取舍：</p>
 * <ul>
 *   <li>使用项目已有的 line.entity.Line / line.entity.LineDayRecord（不再单独搞 LinePO / LineDayRecord PO，W-CLEAN-03 起 LinePO 已删除），保证 DataupLoad 单一表映射源；</li>
 *   <li>handleDetectData 内联 defect_day_record / line_day_record 的查+改，
 *       不再单独拆 IDefectDayRecordService / ILineDayRecordService，避免 PSM 模块拆分粒度直接搬运到本工单；</li>
 *   <li>其它接口方法（handleDetectDetailSearch / handleStatisticDataExport / handleRealtimeDetectDataSearch）
 *       暂以 UnsupportedOperationException / 最小实现占位，等后续工单展开。</li>
 * </ul>
 */
@Service
public class DefectRecordServiceImpl
       extends ServiceImpl<DefectRecordMapper, DefectRecord>
       implements IDefectRecordService {

   private static final Logger log = LoggerFactory.getLogger(DefectRecordServiceImpl.class);

   @Autowired
   private ILineDefectTypeService lineDefectTypeService;

   @Autowired
   private DefectDayRecordMapper defectDayRecordMapper;

   @Autowired
   private LineDayRecordMapper lineDayRecordMapper;

   @Autowired
   private ILineService lineService;

   @Transactional(rollbackFor = Exception.class)
   @Override
   public BaseResult handleDetectData(DetectDataUploadDTO form) {
      Line line = this.lineService.getByLineNoAndFaceNo(form.getLineNo(), form.getFaceNo());
      if (line == null) {
         return BaseResult.build().error("20204");
      }

      List<DefectCountDTO> defects = form.getTodayData().getDefects();
      if (CollectionUtils.isNotEmpty(defects)) {
         this.lineDefectTypeService.addDefectTypeIfNotExist(line, defects);
         String statisticTime = HikDateUtil.formatLocalDate(form.getTodayData().getStatisticTime(), "yyyy-MM-dd HH") + ":00:00";

         // 1) 按 (time, line_no, face_no, type) 查已存在的 defect_day_record 行
         Map<String, DefectDayRecord> sortDefectRecordByType = Maps.newHashMap();
         LambdaQueryWrapper<DefectDayRecord> existQw = (LambdaQueryWrapper<DefectDayRecord>)((LambdaQueryWrapper<DefectDayRecord>)((LambdaQueryWrapper<DefectDayRecord>)Wrappers
                  .lambdaQuery(DefectDayRecord.class).eq(DefectDayRecord::getTime, statisticTime))
               .eq(DefectDayRecord::getLineNo, form.getLineNo()))
            .eq(DefectDayRecord::getFaceNo, form.getFaceNo());
         this.defectDayRecordMapper.selectList(existQw).forEach(r -> sortDefectRecordByType.put(r.getType(), r));

         List<DefectDayRecord> needUpdateRecord = Lists.newArrayList();
         for (DefectCountDTO defectCount : defects) {
            DefectDayRecord record = sortDefectRecordByType.get(defectCount.getType());
            if (record == null) {
               record = new DefectDayRecord()
                  .setTime(statisticTime)
                  .setType(defectCount.getType())
                  .setCount(0)
                  .setLineNo(form.getLineNo())
                  .setFaceNo(form.getFaceNo());
            }
            record.setCount(defectCount.getCount());
            needUpdateRecord.add(record);
         }
         for (DefectDayRecord rec : needUpdateRecord) {
            if (rec.getId() == null) {
               this.defectDayRecordMapper.insert(rec);
            } else {
               this.defectDayRecordMapper.updateById(rec);
            }
         }

         // 2) line_day_record upsert
         TodayDetectDataDTO todayData = form.getTodayData();
         String lineDayTime = HikDateUtil.formatLocalDate(form.getTodayData().getStatisticTime(), "yyyy-MM-dd HH") + ":00:00";
         LambdaQueryWrapper<LineDayRecord> lineQw = (LambdaQueryWrapper<LineDayRecord>)((LambdaQueryWrapper<LineDayRecord>)((LambdaQueryWrapper<LineDayRecord>)Wrappers
                  .lambdaQuery(LineDayRecord.class).eq(LineDayRecord::getTime, lineDayTime))
               .eq(LineDayRecord::getLineNo, form.getLineNo()))
            .eq(LineDayRecord::getFaceNo, form.getFaceNo());
         LineDayRecord lineDayRecord = this.lineDayRecordMapper.selectOne(lineQw);
         if (lineDayRecord == null) {
            lineDayRecord = new LineDayRecord()
               .setLineNo(line.getLineNo())
               .setTime(lineDayTime)
               .setErrorCount(0)
               .setRightCount(0)
               .setFaceNo(form.getFaceNo())
               .setRemoveTotal(0);
         }

         lineDayRecord.setErrorCount(todayData.getNgNum())
            .setRightCount(todayData.getTotalNum() - todayData.getNgNum())
            .setUploadRemoveTotal(form.getRealTimeData().getRemoveTotal());

         LocalDateTime lastHoursTime = form.getTodayData().getStatisticTime().minusHours(1L);
         LambdaQueryWrapper<LineDayRecord> lastQw = (LambdaQueryWrapper<LineDayRecord>)((LambdaQueryWrapper<LineDayRecord>)((LambdaQueryWrapper<LineDayRecord>)Wrappers
                  .lambdaQuery(LineDayRecord.class).eq(LineDayRecord::getTime,
                     HikDateUtil.formatLocalDate(lastHoursTime, "yyyy-MM-dd HH") + ":00:00"))
               .eq(LineDayRecord::getLineNo, line.getLineNo()))
            .eq(LineDayRecord::getFaceNo, line.getFaceNo());
         LineDayRecord lastHoursData = this.lineDayRecordMapper.selectOne(lastQw);

         Integer removeTotal;
         if (lastHoursData != null) {
            removeTotal = form.getRealTimeData().getRemoveTotal() - lastHoursData.getUploadRemoveTotal();
         } else if (lineDayRecord.getId() == null) {
            removeTotal = form.getRealTimeData().getRemoveTotal();
         } else if (form.getTodayData().getStatisticTime().getHour() != LocalDateTime.now().getHour()) {
            removeTotal = form.getRealTimeData().getRemoveTotal() - lineDayRecord.getRemoveTotal();
         } else {
            removeTotal = lineDayRecord.getRemoveTotal();
         }

         lineDayRecord.setRemoveTotal(removeTotal);
         if (lineDayRecord.getId() == null) {
            this.lineDayRecordMapper.insert(lineDayRecord);
         } else {
            this.lineDayRecordMapper.updateById(lineDayRecord);
         }
      }

      // 3) line.realtime_data 缓存实时数据
      line.setRealtimeData(JSONUtil.toJsonStr(form.getRealTimeData()));
      this.lineService.updateById(line);
      return BaseResult.build().ok();
   }

   @Override
   public BaseResult handleDetectDetailSearch(Integer faceId, String starTime, String endTime) {
      throw new UnsupportedOperationException("handleDetectDetailSearch 不在本工单 W-B03 范围内，后续工单补齐");
   }

   @Override
   public void handleStatisticDataExport(HttpServletResponse resp, ExportDefectStatisticForm form) {
      throw new UnsupportedOperationException("handleStatisticDataExport 不在本工单 W-B03 范围内");
   }

   @Override
   public BaseResult handleRealtimeDetectDataSearch(String lineNo, String faceNo) {
      Line line = this.lineService.getByLineNoAndFaceNo(lineNo, faceNo);
      if (line == null) {
         return BaseResult.build().error("20204");
      }
      BaseResult result = BaseResult.build();
      if (StringUtils.isNotBlank(line.getRealtimeData())) {
         RealTimeDetectData realData = JSONUtil.toBean(line.getRealtimeData(), RealTimeDetectData.class);
         result.data(realData);
      }
      return result;
   }

   @Override
   public List<DefectDayRecord> searchDefectRecord(SearchDefectRecordDTO cond) {
      String time = HikDateUtil.formatLocalDate(cond.getStartTime(), "yyyy-MM-dd HH") + ":00:00";
      LambdaQueryWrapper<DefectDayRecord> qw = (LambdaQueryWrapper<DefectDayRecord>)((LambdaQueryWrapper<DefectDayRecord>)((LambdaQueryWrapper<DefectDayRecord>)((LambdaQueryWrapper<DefectDayRecord>)Wrappers
                  .lambdaQuery(DefectDayRecord.class).eq(DefectDayRecord::getTime, time))
               .in(CollectionUtils.isNotEmpty(cond.getLindGroup()), DefectDayRecord::getLineNo, cond.getLindGroup()))
            .in(CollectionUtils.isNotEmpty(cond.getFaceGroup()), DefectDayRecord::getFaceNo, cond.getFaceGroup()))
         .in(CollectionUtils.isNotEmpty(cond.getDefectGroup()), DefectDayRecord::getType, cond.getDefectGroup());
      return this.defectDayRecordMapper.selectList(qw);
   }
}
