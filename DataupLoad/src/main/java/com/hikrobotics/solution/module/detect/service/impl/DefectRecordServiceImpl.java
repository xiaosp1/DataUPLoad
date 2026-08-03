package com.hikrobotics.solution.module.detect.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.locale.LocaleUtil;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.defect.service.ILineDefectTypeService;
import com.hikrobotics.solution.module.detect.dto.DetectDataUploadDTO;
import com.hikrobotics.solution.module.detect.dto.ExportDefectStatisticForm;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.detect.entity.DefectRecord;
import com.hikrobotics.solution.module.detect.mapper.DefectDayRecordMapper;
import com.hikrobotics.solution.module.detect.mapper.DefectRecordMapper;
import com.hikrobotics.solution.module.detect.service.IDefectDayRecordService;
import com.hikrobotics.solution.module.detect.service.IDefectRecordService;
import com.hikrobotics.solution.module.detect.service.SearchDefectRecordDTO;
import com.hikrobotics.solution.module.detect.util.ExcelUtils;
import com.hikrobotics.solution.module.detect.util.TimeRange;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import com.hikrobotics.solution.module.line.dto.RealTimeDetectData;
import com.hikrobotics.solution.module.line.dto.TodayDetectDataDTO;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import com.hikrobotics.solution.module.line.mapper.LineDayRecordMapper;
import com.hikrobotics.solution.module.line.service.ILineDayRecordService;
import com.hikrobotics.solution.module.line.service.ILineService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工单 W-B03：handleDetectData() 1:1 翻译自反编译 DefectRecordServiceImpl.handleDetectData()。
 *
 * <p>工单 W-DET-08（返工）：
 * {@link #handleStatisticDataExport} 1:1 抄 PSM 反编译产物 —— 早/晚班双行表头 + 按产线 pos
 * 聚合 + 按 (time, lineNo, faceNo) 三键同值纵向合并；不再走 controller 内联导出。
 * 同步注入 {@link IDefectDayRecordService} / {@link ILineDayRecordService}（PSM 同款字段名），
 * 不再直接走 Mapper。</p>
 *
 * <p>取舍：</p>
 * <ul>
 *   <li>使用项目已有的 line.entity.Line / line.entity.LineDayRecord（不再单独搞 LinePO / LineDayRecord PO，W-CLEAN-03 起 LinePO 已删除），保证 DataupLoad 单一表映射源；</li>
 *   <li>{@link #handleDetectData} 内联 defect_day_record / line_day_record 的查+改
 *       （W-B03 既有实现，不动）；</li>
 *   <li>{@link #handleStatisticDataExport} 按 PSM 反编译产物 1:1 抄（service 入口），依赖
 *       {@link IDefectDayRecordService#listBetween} 和
 *       {@link ILineDayRecordService#listLineDayBetween}；</li>
 *   <li>其它接口方法（handleDetectDetailSearch）暂以 UnsupportedOperationException 占位，
 *       等后续工单展开。</li>
 * </ul>
 */
@Service
public class DefectRecordServiceImpl
       extends ServiceImpl<DefectRecordMapper, DefectRecord>
       implements IDefectRecordService {

   private static final Logger log = LoggerFactory.getLogger(DefectRecordServiceImpl.class);

   // PSM 反编译产物字段名：iDefectDayRecordService / iLineDayRecordService（驼峰 i 前缀）
   // —— 1:1 抄 PSM 命名风格。

   @Autowired
   private ILineDefectTypeService lineDefectTypeService;

   @Autowired
   private IDefectDayRecordService iDefectDayRecordService;

   @Autowired
   private ILineDayRecordService iLineDayRecordService;

   @Autowired
   private DefectDayRecordMapper defectDayRecordMapper;

   @Autowired
   private LineDayRecordMapper lineDayRecordMapper;

   @Autowired
   private ILineService lineService;

   /**
    * PSM 同款：早班分界点 {@code LocalTime.of(8, 0, 0)}。{@code time.isBefore(Eight)} 视为夜班。
    */
   private final LocalTime Eight = LocalTime.of(8, 0, 0);

   /**
    * PSM 同款：晚班分界点 {@code LocalTime.of(20, 0, 0)}。
    * {@code !time.isBefore(Eight) && time.isBefore(TWENTY)} 视为白班。
    */
   private final LocalTime TWENTY = LocalTime.of(20, 0, 0);

   // W-FLASH-01 (2026-08-02 深夜)：根治连接池泄漏。
   // 原 @Transactional(rollbackFor=Exception.class) 把 line_defect_type / defect_day_record /
   // line_day_record / line 四张表的几十条 SQL 包进一个长事务。38 条产线每 5s 并发上传 detect，
   // 事务互相锁等待 + idle in transaction 不提交 → 连接被占用超过 Hikari 60s × 50 连接全被占 →
   // sendScreen/login 拿不到连接 → 页面无数据。
   // 改为取消方法级长事务，各写操作单条 SQL 由 JDBC autocommit 自己提交，连接用完即还，
   // 避免跨表持锁。统计按小时，单条失败重传即可，无需跨表事务原子性。
   // 若个别写需原子性，用内部 REQUIRES_NEW 短事务承接（当前各写本身单条原子，accepted）。
   @Override
   public BaseResult handleDetectData(DetectDataUploadDTO form) {
      Line line = this.lineService.getByLineNoAndFaceNo(form.getLineNo(), form.getFaceNo());
      if (line == null) {
         // W-LIVE-DATA-FIX Bug B：传 lineNo/faceNo 给 i18n message 的 {0}/{1} 占位符，
         // 否则 MessageFormat 抛 IllegalArgumentException → 又被 GlobalExceptionHandler 兜底为 10500
         return BaseResult.build().error("20204", form.getLineNo(), form.getFaceNo());
      }

      List<DefectCountDTO> defects = form.getTodayData().getDefects();
      if (CollectionUtils.isNotEmpty(defects)) {
         this.lineDefectTypeService.addDefectTypeIfNotExist(line, defects);
         String statisticTime = HikDateUtil.formatLocalDate(form.getTodayData().getStatisticTime(), "yyyy-MM-dd HH") + ":00:00";

         // 1) 按 (time, line_no, face_no, type) 查已存在的 defect_day_record 行
         java.util.Map<String, DefectDayRecord> sortDefectRecordByType = Maps.newHashMap();
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
            .eq(LineDayRecord::getFaceNo, form.getFaceNo())
            // 防抖：历史存在同 (time,line_no,face_no) 重复行，selectOne 会抛 TooManyResults → 10500。
            // 改为按 id 倒序取最新一条，重复时也用最新值，不再炸。
            .orderByDesc(LineDayRecord::getId)
            .last("LIMIT 1");
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
            .eq(LineDayRecord::getFaceNo, line.getFaceNo())
            .orderByDesc(LineDayRecord::getId)
            .last("LIMIT 1");
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
      // W-RT-4：补齐 PSM 多出来的 KPI 字段（successCount / removeFailRate），
      // 客户端不一定会上送，服务侧兜底计算后写入 JSON，避免前端 KPI 卡恒为 0。
      this.enrichRealtimeDataKpiFields(form.getRealTimeData());
      line.setRealtimeData(JSONUtil.toJsonStr(form.getRealTimeData()));
      this.lineService.updateById(line);
      return BaseResult.build().ok();
   }

   /**
    * W-RT-4：补齐 PSM 多出来的 KPI 字段。
    * <ul>
    *   <li>{@code successCount} = total - ngCount；null/越界兜底为 0</li>
    *   <li>{@code removeFailRate} = removeFail / removeTotal * 100（百分比）；removeTotal=0 兜底 0.0</li>
    * </ul>
    * 仅在原值为 null 时计算；上游若显式传值（即使为 0），保留上游值（PSM 1:1 兼容）。
    */
   private void enrichRealtimeDataKpiFields(RealTimeDetectData real) {
      if (real == null) {
         return;
      }
      // successCount
      if (real.getSuccessCount() == null) {
         Integer total = real.getTotal();
         Integer ng = real.getNgCount();
         int success = 0;
         if (total != null && ng != null) {
            success = Math.max(0, total - ng);
         } else if (total != null) {
            success = Math.max(0, total);
         }
         real.setSuccessCount(success);
      }
      // removeFailRate
      if (real.getRemoveFailRate() == null) {
         Integer removeTotal = real.getRemoveTotal();
         Integer removeFail = real.getRemoveFail();
         double rate = 0.0D;
         if (removeTotal != null && removeTotal > 0 && removeFail != null) {
            rate = (removeFail.doubleValue() / removeTotal.doubleValue()) * 100.0D;
            // 保留 2 位小数，避免浮点尾巴
            rate = Math.round(rate * 100.0D) / 100.0D;
         }
         real.setRemoveFailRate(rate);
      }
   }

   @Override
   public BaseResult handleDetectDetailSearch(Integer faceId, String starTime, String endTime) {
      throw new UnsupportedOperationException("handleDetectDetailSearch 不在本工单 W-B03 范围内，后续工单补齐");
   }

   // ============================== W-DET-08 — handleStatisticDataExport 1:1 抄 PSM ==============================

   /**
    * W-DET-08（返工）：缺陷统计 Excel 导出 1:1 抄自反编译
    * {@code DefectRecordServiceImpl.handleStatisticDataExport}。
    *
    * <p>核心逻辑：</p>
    * <ol>
    *   <li>按 {@code form.getStartTime()} / {@code form.getEndTime()} 拉
    *       {@link LineDayRecord} 和 {@link DefectDayRecord}；</li>
    *   <li>按天循环（{@link TimeRange#TimePattern#YYYY_MM_DD}）：</li>
    *   <li>　- 当天 + 次日早 8 点前 → 夜班；</li>
    *   <li>　- 当天 8~20 点 → 白班；</li>
    *   <li>　- 其余时段 → 夜班；</li>
    *   <li>　- 按 {@code pos} (=lineNo:faceNo) 聚合到 sortDay/sortNight 的
    *       {@code sortCountByTypeAndPos}；</li>
    *   <li>　- 按 {@code type} 累加到 sortDay/sortNight 的 {@code sortTotalCountByDefect}；</li>
    *   <li>　- 同样对 {@link LineDayRecord#getRemoveTotal()} 按 lineNo 聚合；</li>
    *   <li>　- 每个产线一行，列 = [lineNo, faceNo, ...各缺陷 type, removeTotal]，
    *       最后追加汇总行 [汇总, "", ...各缺陷 type 总和, totalRemoval]；</li>
    *   <li>　- 表头两行：[[白班, 线别], [白班, ""], [白班, 缺陷1], ...,
    *       [白班, 剔除数]]（夜班同型）；</li>
    *   <li>　- 创建 {@link ExcelUtils.Table} 时 {@code mergeColumns=[线别, 剔除数]}，
    *       创建 {@link ExcelUtils.SheetConfig} 时 {@code name=today}（yyyy-MM-dd），
    *       {@code tables=[dayTable, nightTable]}；</li>
    *   <li>最终调用
    *       {@link ExcelUtils#export(HttpServletResponse, List, String)}
    *       写出多 sheet 多 table 流式 xlsx。</li>
    * </ol>
    *
    * <p>PSM 1:1 细节：</p>
    * <ul>
    *   <li>{@code HashMultimap} 用 {@code defect.getTime().substring(0, 10)} 做 key，
    *       即 yyyy-MM-dd；</li>
    *   <li>表头 2 行：第 0 列 = [白班/夜班, 线别]，第 1 列 = [白班/夜班, ""]，
    *       后续每个 defect 一个 cell [白班/夜班, defect]；</li>
    *   <li>汇总行加在所有 line 行之后；</li>
    *   <li>合并列 = [线别, 剔除数] —— 由 {@link com.hikrobotics.solution.module.detect.excel.DataMergeStrategy}
    *       按"首列同值分组 + 目标列同值"纵向合并（PSM 同款逻辑）。</li>
    * </ul>
    */
   @Override
   public void handleStatisticDataExport(HttpServletResponse resp, ExportDefectStatisticForm form) {
      // 1) 拉取时间区间内的剔除数（line_day_record）+ 缺陷数（defect_day_record）
      HashMultimap<String, LineDayRecord> sortRemovalByTime = HashMultimap.create();
      this.iLineDayRecordService.listLineDayBetween(form.getStartTime(), form.getEndTime())
         .forEach(data -> sortRemovalByTime.put(data.getTime().substring(0, 10), data));

      HashMultimap<String, DefectDayRecord> sortDefectByTime = HashMultimap.create();
      this.iDefectDayRecordService.listBetween(form.getStartTime(), form.getEndTime())
         .forEach(defect -> sortDefectByTime.put(defect.getTime().substring(0, 10), defect));

      // 2) 所有产线按 lineNo 升序
      List<Line> lines = this.lineService.listLine().stream()
         .sorted(Comparator.comparing(Line::getLineNo))
         .toList();

      // 3) 按天循环
      String today = null;
      String tomorrow = null;
      HashSet<String> defects = new HashSet<>();
      ArrayList<ExcelUtils.SheetConfig> sheets = new ArrayList<>();

      TimeRange range = new TimeRange(
         HikDateUtil.transformTime(form.getStartTime()),
         HikDateUtil.transformTime(form.getEndTime()),
         TimeRange.TimePattern.YYYY_MM_DD);

      while (range.hasNext()) {
         LocalDateTime current = range.next();
         today = HikDateUtil.formatLocalDate(current, range.getPattern());
         tomorrow = current.plusDays(1L).toLocalDate().toString();

         // 3.1) 按 (pos, type) 聚合白班 / 夜班
         HashMap<String, Integer> sortDayTotalCountByDefect = new HashMap<>();
         HashMap<String, Integer> sortNightTotalCountByDefect = new HashMap<>();
         HashMap<String, java.util.Map<String, Integer>> sortDayCountByTypeAndPos = new HashMap<>();
         HashMap<String, java.util.Map<String, Integer>> sortNightCountByTypeAndPos = new HashMap<>();

         // 当天白班（>=8 点 & <20 点）+ 夜班（其它）
         sortDefectByTime.get(today).forEach(defect -> {
            if (!defect.getLocalTime().isBefore(this.Eight) && defect.getLocalTime().isBefore(this.TWENTY)) {
               // 白班
               defects.add(defect.getType());
               java.util.Map<String, Integer> sortCountByType =
                  sortDayCountByTypeAndPos.getOrDefault(defect.getPos(), new HashMap<>());
               sortCountByType.put(defect.getType(),
                  sortCountByType.getOrDefault(defect.getType(), 0) + defect.getCount());
               sortDayCountByTypeAndPos.put(defect.getPos(), sortCountByType);
               sortDayTotalCountByDefect.put(defect.getType(),
                  sortDayTotalCountByDefect.getOrDefault(defect.getType(), 0) + defect.getCount());
            } else {
               // 夜班
               defects.add(defect.getType());
               java.util.Map<String, Integer> sortDefectCountByType =
                  sortNightCountByTypeAndPos.getOrDefault(defect.getPos(), new HashMap<>());
               sortDefectCountByType.put(defect.getType(),
                  sortDefectCountByType.getOrDefault(defect.getType(), 0) + defect.getCount());
               sortNightCountByTypeAndPos.put(defect.getPos(), sortDefectCountByType);
               sortNightTotalCountByDefect.put(defect.getType(),
                  sortNightTotalCountByDefect.getOrDefault(defect.getType(), 0) + defect.getCount());
            }
         });

         // 次日凌晨（< 8 点）算前一天夜班
         sortDefectByTime.get(tomorrow).forEach(defect -> {
            if (defect.getLocalTime().isBefore(this.Eight)) {
               defects.add(defect.getType());
               java.util.Map<String, Integer> sortDefectCountByType =
                  sortNightCountByTypeAndPos.getOrDefault(defect.getPos(), new HashMap<>());
               sortDefectCountByType.put(defect.getType(),
                  sortDefectCountByType.getOrDefault(defect.getType(), 0) + defect.getCount());
               sortNightCountByTypeAndPos.put(defect.getPos(), sortDefectCountByType);
               sortNightTotalCountByDefect.put(defect.getType(),
                  sortNightTotalCountByDefect.getOrDefault(defect.getType(), 0) + defect.getCount());
            }
         });

         // 3.2) 按 lineNo 聚合剔除数（白班 / 夜班）
         AtomicInteger dayTotalRemoval = new AtomicInteger(0);
         AtomicInteger nightTotalRemoval = new AtomicInteger(0);
         HashMap<String, Integer> sortDayRemovalCountByLine = new HashMap<>();
         HashMap<String, Integer> sortNightRemovalCountByLine = new HashMap<>();

         sortRemovalByTime.get(today).forEach(lineData -> {
            if (!lineData.getLocalTime().isBefore(this.Eight) && lineData.getLocalTime().isBefore(this.TWENTY)) {
               Integer count = sortDayRemovalCountByLine.getOrDefault(lineData.getLineNo(), 0);
               sortDayRemovalCountByLine.put(lineData.getLineNo(), count + lineData.getRemoveTotal());
               dayTotalRemoval.set(dayTotalRemoval.get() + lineData.getRemoveTotal());
            } else {
               Integer count = sortNightRemovalCountByLine.getOrDefault(lineData.getLineNo(), 0);
               sortNightRemovalCountByLine.put(lineData.getLineNo(), count + lineData.getRemoveTotal());
               nightTotalRemoval.set(nightTotalRemoval.get() + lineData.getRemoveTotal());
            }
         });

         sortRemovalByTime.get(tomorrow).forEach(lineData -> {
            if (lineData.getLocalTime().isBefore(this.Eight)) {
               Integer count = sortNightRemovalCountByLine.getOrDefault(lineData.getLineNo(), 0);
               sortNightRemovalCountByLine.put(lineData.getLineNo(), count + lineData.getRemoveTotal());
               nightTotalRemoval.set(nightTotalRemoval.get() + lineData.getRemoveTotal());
            }
         });

         // 3.3) 表头 + 行数据
         boolean initHeader = false;
         CollUtil.sort(defects, String::compareTo);
         ArrayList<List<String>> dayHeaders = new ArrayList<>();
         dayHeaders.add(new ArrayList<>(List.of("白班", "线别")));
         dayHeaders.add(new ArrayList<>(List.of("白班", "")));
         ArrayList<List<String>> nightHeaders = new ArrayList<>();
         nightHeaders.add(new ArrayList<>(List.of("夜班", "线别")));
         nightHeaders.add(new ArrayList<>(List.of("夜班", "")));
         ArrayList<List<Object>> dayValues = new ArrayList<>();
         ArrayList<List<Object>> nightValues = new ArrayList<>();

         for (Line line : lines) {
            ArrayList<Object> dayVal = Lists.newArrayList((Object[]) new Object[] { line.getLineNo(), line.getFaceNo() });
            ArrayList<Object> nightVal = Lists.newArrayList((Object[]) new Object[] { line.getLineNo(), line.getFaceNo() });
            java.util.Map<String, Integer> dayDefectCountOfLine = sortDayCountByTypeAndPos.getOrDefault(line.getPos(), new HashMap<>());
            java.util.Map<String, Integer> nightDefectCountOfLine = sortNightCountByTypeAndPos.getOrDefault(line.getPos(), new HashMap<>());
            for (String defectName : defects) {
               dayVal.add(dayDefectCountOfLine.getOrDefault(defectName, 0));
               nightVal.add(nightDefectCountOfLine.getOrDefault(defectName, 0));
               if (!initHeader) {
                  dayHeaders.add(new ArrayList<>(List.of("白班", defectName)));
                  nightHeaders.add(new ArrayList<>(List.of("夜班", defectName)));
               }
            }
            initHeader = true;
            dayVal.add(sortDayRemovalCountByLine.getOrDefault(line.getLineNo(), 0));
            nightVal.add(sortNightRemovalCountByLine.getOrDefault(line.getLineNo(), 0));
            dayValues.add(dayVal);
            nightValues.add(nightVal);
         }

         // 汇总行
         ArrayList<Object> dayDefectSummary = Lists.newArrayList((Object[]) new Object[] { "汇总", "" });
         defects.forEach(defectName -> dayDefectSummary.add(sortDayTotalCountByDefect.getOrDefault(defectName, 0)));
         dayDefectSummary.add(dayTotalRemoval.get());
         dayValues.add(dayDefectSummary);

         ArrayList<Object> nightDefectSummary = Lists.newArrayList((Object[]) new Object[] { "汇总", "" });
         defects.forEach(defectName -> nightDefectSummary.add(sortNightTotalCountByDefect.getOrDefault(defectName, 0)));
         nightDefectSummary.add(nightTotalRemoval.get());
         nightValues.add(nightDefectSummary);

         dayHeaders.add(new ArrayList<>(List.of("白班", "剔除数")));
         nightHeaders.add(new ArrayList<>(List.of("夜班", "剔除数")));

         ExcelUtils.Table dayTable = new ExcelUtils.Table()
            .setHeaders((List) dayHeaders)
            .setValues(dayValues)
            .setMergeColumns(List.of("线别", "剔除数"));
         ExcelUtils.Table nightTable = new ExcelUtils.Table()
            .setHeaders((List) nightHeaders)
            .setValues(nightValues)
            .setMergeColumns(List.of("线别", "剔除数"));

         ExcelUtils.SheetConfig config = new ExcelUtils.SheetConfig()
            .setName(today)
            .setTables(List.of(dayTable, nightTable));
         sheets.add(config);

         defects.clear();
      }

      // 4) 文件名 + 调用 ExcelUtils.export
      String timeRange = form.getStartTime().substring(0, 10).replace("-", "")
         + "_" + form.getEndTime().substring(0, 10).replace("-", "");
      ExcelUtils.export(resp, sheets, LocaleUtil.getMsg("defectSummary") + "(" + timeRange + ")");
   }

   @Override
   public BaseResult handleRealtimeDetectDataSearch(String lineNo, String faceNo) {
      Line line = this.lineService.getByLineNoAndFaceNo(lineNo, faceNo);
      if (line == null) {
         // W-LIVE-DATA-FIX Bug B：同上，传 lineNo/faceNo 给 i18n 占位符
         return BaseResult.build().error("20204", lineNo, faceNo);
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
