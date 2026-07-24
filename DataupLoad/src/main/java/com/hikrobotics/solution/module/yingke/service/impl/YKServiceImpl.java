package com.hikrobotics.solution.module.yingke.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.component.webclient.HikWebClient;
import com.hikrobotics.solution.framework.util.EventUtil;
import com.hikrobotics.solution.module.alarm.constant.AlarmSolvedEnum;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import com.hikrobotics.solution.module.alarm.mapper.AlarmRecordMapper;
import com.hikrobotics.solution.module.alarm.service.IDefectTypeService;
import com.hikrobotics.solution.module.line.service.ILineService;
import com.hikrobotics.solution.module.yingke.config.YKConfig;
import com.hikrobotics.solution.module.yingke.dto.AlarmDTO;
import com.hikrobotics.solution.module.yingke.dto.ContextDTO;
import com.hikrobotics.solution.module.yingke.dto.DetectDataDTO;
import com.hikrobotics.solution.module.yingke.dto.LineAndDefectDTO;
import com.hikrobotics.solution.module.yingke.dto.ListParamsDTO;
import com.hikrobotics.solution.module.yingke.dto.SearchDefectRecordDTO;
import com.hikrobotics.solution.module.line.service.ILineDayRecordService;
import com.hikrobotics.solution.module.yingke.dto.StringParamDTO;
import com.hikrobotics.solution.module.yingke.dto.YKRequestDTO;
import com.hikrobotics.solution.module.yingke.dto.YKResponseDTO;
import com.hikrobotics.solution.module.yingke.event.PushAlarmEvent;
import com.hikrobotics.solution.module.yingke.service.IYKService;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

/**
 * yingke Service 实现（沿用 PSM YKServiceImpl 1:1，关键方法 pushAlarm2YK 不变）。
 * <p>
 * <b>老板口径（W-B04）</b>：报警链路走 yk 推 MES，MES 转飞书；本类不做飞书 webhook。
 * <p>
 * <b>W-X13d 灰盒拆双开关</b>：
 * <ul>
 *   <li>{@link #updateTicket()} —— 判 {@code loginEnabled}：true 时调 MES login 拿 / 续 ticket</li>
 *   <li>{@link #pushAlarm2YK(PushAlarmEvent)} —— 判 {@code uploadEnabled}：true 才推 yk；false 则静默跳过（debug 日志）</li>
 *   <li>{@link #pushAlarm(AlarmRecord)} —— 业务侧同步入口，发布 {@link PushAlarmEvent}</li>
 * </ul>
 * <p>
 * 灰盒默认 {@code loginEnabled=true, uploadEnabled=false}：ticket 预热、推送静默。
 * 正式上线前必须改 {@code uploadEnabled=true}（铁则 42）。
 */
@Service
public class YKServiceImpl implements IYKService {
   private static final Logger log = LoggerFactory.getLogger(YKServiceImpl.class);
   @Autowired
   private YKConfig ykConfig;
   private volatile String ticket;
   /** W-X30: 内存去重 Set，同一 (lineNo, faceNo, defectName, type) 只推首条 */
   private final Set<String> pushAlarmDedupKeySet = ConcurrentHashMap.newKeySet();
   @Autowired
   private AlarmRecordMapper alarmRecordMapper;
   private final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

   public YKServiceImpl() {
      this.threadPoolTaskScheduler.setPoolSize(1);
      this.threadPoolTaskScheduler.setThreadNamePrefix("Update-Ticket-Thread-");
      this.threadPoolTaskScheduler.initialize();
   }

   @PostConstruct
   private void init() {
      this.updateTicket();
   }

   /**
    * 拉取 / 续约 ticket（PSM 同款）。MES 不可达时仅记 error log，不抛出。
    * <p>
    * <b>W-X13d</b>：判 {@link YKConfig#isLoginEnabled()}（不再判老 enable）。
    */
   private void updateTicket() {
      if (this.ykConfig.isLoginEnabled()) {
         try {
            YKRequestDTO<StringParamDTO> request = new YKRequestDTO<>();
            request.setApiType("AuthenticationController").setMethod("Login");
            request.getParameters().add(new StringParamDTO(this.ykConfig.getUsername()));
            request.getParameters().add(new StringParamDTO(this.ykConfig.getPassword()));
            HikWebClient<YKResponseDTO> client = HikWebClient.create(this.ykConfig.getBaseUrl());
            YKResponseDTO resp = client.uri(this.ykConfig.getUri()).body(request).blockPost(YKResponseDTO.class, YKResponseDTO.DEFAULT);
            if (resp != null && Boolean.TRUE.equals(resp.getSuccess())) {
               ContextDTO ctx = resp.getContext();
               this.ticket = ctx == null ? null : ctx.getTicket();
               log.info("success to get ticket from yk.[ticket={}]", this.ticket);
            } else {
               log.error("get ticket from yk system failed.[resp={}]", resp);
            }

            Instant overTimeInstant = LocalDateTime.now()
               .plusMinutes(this.ykConfig.getLoginInterval().intValue())
               .toInstant(ZoneOffset.ofTotalSeconds(TimeZone.getDefault().getRawOffset() / 1000));
            this.threadPoolTaskScheduler.schedule(this::updateTicket, overTimeInstant);
         } catch (Exception exception) {
            log.error("update ticket error.", exception);
         }
      }
   }

   @Autowired
   private ILineService lineService;

   @Autowired
   private IDefectTypeService defectTypeService;

   @Override
   public BaseResult handleLineAndDefectSearch() {
      LineAndDefectDTO data = new LineAndDefectDTO();
      this.lineService.list().forEach(line -> {
         data.getLineGroup().add(line.getLineNo());
         data.getFaceGroup().add(line.getFaceNo());
      });
      this.defectTypeService.list().forEach(defect -> data.getDefectGroup().add(defect.getName()));
      return BaseResult.build().data(data);
   }

   @Autowired
   private com.hikrobotics.solution.module.detect.service.IDefectRecordService defectRecordService;

   @Autowired
   private ILineDayRecordService lineDayRecordService;

   @Override
   public BaseResult searchDefectRecord(SearchDefectRecordDTO form) {
      // 转为 detect 模块的 SearchDefectRecordDTO
      com.hikrobotics.solution.module.detect.service.SearchDefectRecordDTO detectForm =
          new com.hikrobotics.solution.module.detect.service.SearchDefectRecordDTO();
      detectForm.setStartTime(form.getStartTime() == null ? null : form.getStartTime().toString());
      detectForm.setEndTime(form.getEndTime() == null ? null : form.getEndTime().toString());
      detectForm.setLindGroup(form.getLindGroup());
      detectForm.setDefectGroup(form.getDefectGroup());
      detectForm.setFaceGroup(form.getFaceGroup());

      List<DetectDataDTO.DefectDataDTO> defects = new ArrayList<>();
      this.defectRecordService.searchDefectRecord(detectForm).forEach(record ->
          defects.add(DetectDataDTO.DefectDataDTO.convert(record)));

      DetectDataDTO data = new DetectDataDTO().setDefects(defects);
      if (this.ykConfig.isSearchRemove()) {
         List<DetectDataDTO.RemoveCountDTO> removeCounts = new ArrayList<>();
         this.lineDayRecordService.searchLineDayRecord(form).forEach(record ->
             removeCounts.add(DetectDataDTO.RemoveCountDTO.convert(record)));
         data.setRemoveCounts(removeCounts);
      }
      return BaseResult.build().data(data);
   }

   /**
    * 异步推报警到 MES（PSM pushAlarm2YK 1:1）。
    * <p>
    * <b>W-X13d 双分支语义</b>：
    * <ul>
    *   <li>{@code uploadEnabled=true} + ticket 拿到 → 推 MES（与 PSM 一致）</li>
    *   <li>{@code uploadEnabled=false} → 静默跳过（debug 日志，不污染 ERROR）</li>
    *   <li>uploadEnabled=true 但 ticket 为 null → 报 ERROR（真 bug）</li>
    * </ul>
    */
   @Async
   @EventListener(PushAlarmEvent.class)
   @Override
   public void pushAlarm2YK(PushAlarmEvent event) {
      if (!this.ykConfig.isUploadEnabled()) {
         // W-X13d 灰盒默认关：不推 MES，但保留链路待上线切换
         log.debug("yk upload disabled, skip push.[alarm={}]", event.getAlarmRecord());
         return;
      }
      if (StringUtils.isBlank(this.ticket)) {
         // uploadEnabled=true 但 ticket 还没拿到 → 真 bug
         log.error("push alarm to yk error, ticket is null.[alarm={}]", event.getAlarmRecord());
         return;
      }
      {
         log.warn("success receive alarm event.[{}][{}]", event.getSource().getClass().getSimpleName(), event.getAlarmRecord());
         AlarmDTO alarm = AlarmDTO.convertFromPO(event.getAlarmRecord());
         alarm.setWorkShop(this.ykConfig.getWorkshop());
         AlarmRecord record = event.getAlarmRecord();
         if (StringUtils.isNotBlank(record.getDefectName())) {
            LambdaQueryWrapper<AlarmRecord> countWrapper = Wrappers.<AlarmRecord>lambdaQuery()
               .eq(AlarmRecord::getDefectName, record.getDefectName())
               .eq(AlarmRecord::getLineNo, record.getLineNo())
               .eq(AlarmRecord::getFaceNo, record.getFaceNo())
               .eq(AlarmRecord::getType, record.getType())
               .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue());
            long count = this.alarmRecordMapper.selectCount(countWrapper);
            alarm.setAlarmCount((int) count);
            alarm.setAlarmDetails(alarm.getAlarmDetails() + "(" + count + ")");
            // W-X30 dedup v3: 同一 (defectName, lineNo, faceNo, type) 从当前启动开始只推首条
            // 用内存 Set 追踪已推送的 key
            String dedupKey = record.getDefectName() + "|" + record.getLineNo() + "|" + record.getFaceNo() + "|" + record.getType();
            if (!pushAlarmDedupKeySet.add(dedupKey)) {
               log.info("yk push dedup: skip already pushed. [defect={}][line={}][face={}][key={}]",
                  record.getDefectName(), record.getLineNo(), record.getFaceNo(), dedupKey);
               return;
            }
         }

         ListParamsDTO<AlarmDTO> params = new ListParamsDTO<>();
         params.getValue().add(alarm);
         YKRequestDTO<ListParamsDTO<AlarmDTO>> request = new YKRequestDTO<>();
         request.setApiType("VisualInspectionController").setMethod("HandleVisualInspectionAlarm");
         request.getParameters().add(params);
         request.setContext(new ContextDTO(this.ticket, 1));
         try {
            HikWebClient<YKResponseDTO> client = HikWebClient.create(this.ykConfig.getBaseUrl());
            YKResponseDTO resp = client.uri(this.ykConfig.getUri()).body(request).blockPost(YKResponseDTO.class, YKResponseDTO.DEFAULT);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess()) || resp.getResult() == null || this.parseCode(resp.getResult()) != 200) {
               log.error("push alarm info to yk failed.[resp={}]", resp);
            } else {
               log.info("push alarm to yk success.[uuid={}][line={}][face={}][type={}]",
                  record.getUuid(), record.getLineNo(), record.getFaceNo(), record.getType());
            }
         } catch (Exception ex) {
            log.error("push alarm to yk exception.", ex);
         }
      }
   }

   /**
    * 同步入口（DataupLoad 任务要求），发布 {@link PushAlarmEvent} 后立即返回；
    * 实际推送由 {@link #pushAlarm2YK(PushAlarmEvent)} 异步消费。
    */
   @Override
   public void pushAlarm(AlarmRecord record) {
      if (record == null) {
         return;
      }
      EventUtil.publish(new PushAlarmEvent(this, record));
   }

   public Integer parseCode(Object data) {
      BaseResult result = (BaseResult) BeanUtil.toBean(data, BaseResult.class);
      return result.getCode();
   }


}
