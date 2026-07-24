package com.hikrobotics.solution.module.alarm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Maps;
import com.hikrobotics.solution.common.constants.StateEnum;
import com.hikrobotics.solution.common.constants.WsTypeEnum;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.framework.component.ws.model.WsMessage;
import com.hikrobotics.solution.framework.util.EventUtil;
import com.hikrobotics.solution.module.alarm.config.DefectAlarmConfig;
import com.hikrobotics.solution.module.alarm.constant.AlarmSolvedEnum;
import com.hikrobotics.solution.module.alarm.constant.AlarmTypeEnum;
import com.hikrobotics.solution.module.alarm.dto.AlarmDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmInfoQueryDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmQueryDTO;
import com.hikrobotics.solution.module.alarm.dto.IgnoreAlarmDTO;
import com.hikrobotics.solution.module.alarm.dto.SearchAlarmDTO;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import com.hikrobotics.solution.module.alarm.mapper.AlarmRecordMapper;
import com.hikrobotics.solution.module.alarm.model.DefectType;
import com.hikrobotics.solution.module.alarm.service.IAlarmRecordService;
import com.hikrobotics.solution.module.alarm.service.IDefectTypeService;
import com.hikrobotics.solution.module.alarm.service.IIgnoreAlarmService;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.service.ILineService;
import com.hikrobotics.solution.module.alarm.event.DealAlarmEvent;
import com.hikrobotics.solution.module.yingke.event.PushAlarmEvent;
import com.hikrobotics.solution.module.yingke.service.IYKService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * DataupLoad alarm 记录 Service 实现（沿用 PSM AlarmRecordServiceImpl 1:1，关键逻辑 add / sendAlarmMessage 不变）。
 *
 * <h2>W-B04 BUG 修复</h2>
 * <p>
 * PSM 反编译产物中 {@link #sendAlarmMessage(AlarmRecord)} 有以下 BUG：
 * <pre>
 * boolean isIgnore = false;        // ← 硬编码 false，从不查库
 * if (... &amp;&amp; !isIgnore) { ... } // 白名单永远失效
 * </pre>
 * DataupLoad 修复：调用 {@link IIgnoreAlarmService#isIgnore(Integer, String, String, String)}
 * 实际查询 ignore_alarm 表，过滤 ignore_time &gt; now() 的记录。
 *
 * <h2>老板口径（W-B04）</h2>
 * <p>
 * "上传飞出报警这个其实原先我们已经做过了，只不过是被麦斯转了一手，其实原先的功能是我们将报警信息推送到麦斯之后，麦斯自己去推送飞书的这块"
 * <br>→ 沿用 PSM yk 推送链路：{@code alarm → yk 推 MES → MES 转飞书}。不新做飞书 webhook。
 *
 * <h2>DataupLoad 简化</h2>
 * <p>
 * DataupLoad 当前只实装了 alarm 入口必需的方法；listAll / deal / handleAlarmNumGet / handleAlarmSearch /
 * handleAlarmIgnore / getAlarmListInfo / dealClientAlarm / dealClientAlarmListener / sendAlarmSoundWsMessage
 * 暂以 BaseResult.build().ok() 占位，待后续迁移 PSM 后台时再补齐。
 */
@Service
public class AlarmRecordServiceImpl extends ServiceImpl<AlarmRecordMapper, AlarmRecord> implements IAlarmRecordService {
   private static final Logger log = LoggerFactory.getLogger(AlarmRecordServiceImpl.class);
   @Autowired
   private DefectAlarmConfig alarmConfig;
   @Lazy
   @Autowired
   private IStatusRecordService statusRecordService;
   @Autowired
   private WebSocketHandler webSocketHandler;
   @Autowired
   private AlarmRecordMapper alarmRecordMapper;
   @Value("${alarm.interval:60}")
   private Integer alarmInterval;
   @Value("${alarm.high-type:3}")
   private String highTypes;
   @Autowired
   @Lazy
   private ILineService lineService;
   @Autowired
   private IIgnoreAlarmService ignoreAlarmService;
   @Autowired
   private IDefectTypeService defectTypeService;
   @Autowired
   @Lazy
   private IYKService ykService;
   private static final String DEFECT_ALARM_MSG_TEMP = "[{}] 缺陷报警";

   @Override
   public BaseResult listAll(AlarmQueryDTO query) {
      // DataupLoad 当前 web 后台查询接口未启用；PSM 原版逻辑已省略，避免过度复刻。
      return BaseResult.build().ok().data(Collections.emptyList());
   }

   /**
    * 报警发送入口（核心方法，沿用 PSM add 逻辑 1:1）。
    * <p>
    * PSM 原版依赖 {@code ISystemConfigService}，DataupLoad 当前未启用该组件，所以 WebSocket 声音推送分支被去除。
    *
    * <h2>W-X21 全局开关（W-X21）</h2>
    * <p>
    * PM 锋卫 2026-07-23 14:25 派工：入口先判 {@code alarm.global-enabled}（默认 true）。
    * 当 {@code false} 时报警直接 return {@code BaseResult.build().ok()}，既不落 PG 也不推 yk / WebSocket；
    * 用于老板紧急关停（老板指令时 PM 派工单改 {@code alarm.global-enabled: false}）。
    */
   @Override
   public BaseResult add(AlarmDTO form) {
      if (!this.alarmConfig.isGlobalEnabled()) {
         log.warn("alarm global disabled, skip.[form={}]", form);
         return BaseResult.build().ok();
      }
      AlarmTypeEnum alarmType = AlarmTypeEnum.getByCode(form.getType());
      if (alarmType == null) {
         log.error("alarm type not support.[form={}]", form);
         return BaseResult.build().error("20101");
      }

      Map<String, DefectType> sortDefectTypeByName = Maps.newHashMap();
      this.defectTypeService.listByAttribute(form.getType(), DefectType::getCategory).forEach(type -> sortDefectTypeByName.put(type.getName(), type));
      boolean isInterestingDefect = false;
      if (CollectionUtils.isNotEmpty(sortDefectTypeByName)) {
         String message = form.getMessage();
         String defectName = null;

         for (DefectAlarmConfig.DefectTypeConfig config : this.alarmConfig.getConfig()) {
            if (config.getType().toUpperCase().equals(alarmType.name())) {
               message = ReUtil.get(config.getTemplate(), form.getMessage(), 0);

               for (String name : sortDefectTypeByName.keySet()) {
                  if (message.contains(name)) {
                     defectName = name;
                     if (alarmType == AlarmTypeEnum.DEFECT) {
                        message = StrUtil.format(DEFECT_ALARM_MSG_TEMP, defectName);
                     }
                     isInterestingDefect = true;
                     break;
                  }
               }
            }
         }

         if (isInterestingDefect) {
            // 把同一 (defectName + lineNo + faceNo + type) 下未处理的旧报警置为已忽略
            LambdaUpdateWrapper<AlarmRecord> uw = Wrappers.<AlarmRecord>lambdaUpdate()
               .eq(AlarmRecord::getDefectName, defectName)
               .eq(AlarmRecord::getLineNo, form.getLineNo())
               .eq(AlarmRecord::getType, form.getType())
               .eq(AlarmRecord::getFaceNo, form.getFaceNo())
               .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue())
               .set(AlarmRecord::getSolve, AlarmSolvedEnum.IGNORE.getValue());
            this.update(uw);

            AlarmRecord alarm = BeanUtil.copyProperties(form, AlarmRecord.class);
            alarm.setSolve(AlarmSolvedEnum.UNSOLVED.getValue())
               .setMessage(message)
               .setDefectName(defectName);
            alarm.setDefectType(sortDefectTypeByName.get(defectName));
            this.save(alarm);
            this.sendAlarmMessage(alarm);
         }
      }

      if (!isInterestingDefect) {
         log.warn("current alarm is not interesting defect.[form={}]", form);
      }
      return BaseResult.build();
   }

   /**
    * 报警发送 / 推送 yk（PSM sendAlarmMessage + isIgnore BUG 修复）。
    * <p>
    * DataupLoad 修复点：调用 {@link IIgnoreAlarmService#isIgnore(Integer, String, String, String)}
    * 实际查询 ignore_alarm 表，而非 PSM 原版硬编码 {@code boolean isIgnore = false;}。
    */
   @Override
   public void sendAlarmMessage(AlarmRecord alarm) {
      DefectType defectType = alarm.getDefectType();
      // ====== W-B04 BUG FIX START ======
      // PSM 反编译产物：boolean isIgnore = false; ← 白名单永远失效
      // DataupLoad 修复：实际查询 ignore_alarm 表
      boolean isIgnore = this.ignoreAlarmService.isIgnore(
         alarm.getType(), alarm.getDefectName(), alarm.getLineNo(), alarm.getFaceNo());
      // ====== W-B04 BUG FIX END ======
      if (defectType != null
         && Objects.equals(defectType.getAlarmEnable(), StateEnum.YES.getValue())
         && !isIgnore) {
         this.sendAlarmTextMessage();
         if (Objects.equals(defectType.getSoundEnable(), StateEnum.YES.getValue())
            && Objects.equals(alarm.getSolve(), AlarmSolvedEnum.UNSOLVED.getValue())) {
            // PSM 原版：sendAlarmSoundWsMessage —— DataupLoad 当前未启用 system_config，
            // 跳过 WS 声音推送（不影响报警记录入库和 yk 推送）。
            log.debug("defect alarm sound ws push skipped (system_config not wired).[alarm={}]", alarm);
         }
      }

      if (!isIgnore
         && defectType != null
         && Objects.equals(defectType.getSendYkEnable(), StateEnum.YES.getValue())) {
         EventUtil.publish(new PushAlarmEvent(this, alarm));
      }
   }

   @Override
   public BaseResult deal(String uuid) {
      // PSM 原版逻辑省略；DataupLoad 当前未启用。
      return BaseResult.build().ok();
   }
   /**
    * 处理客户端掉线告警（W-X30b）：PSM 同款 dealClientAlarm，按 (lineNo, faceNo, reason) 去重。
    * <p>
    * 查询指定产线工位下所有 reason 匹配且 solve=UNSOLVED 的报警，
    * 除首条外全部置为 SOLVED 批量更新，对首条调用 {@link #deal(String)}。
    * <p>
    * W-X30b 修复：原 DataupLoad 版本去重 key 为 (lineNo, faceNo, type)，
    * 现改为 PSM 同款的 (lineNo, faceNo, reason)。
    *
    * @param lineNo 产线号
    * @param faceNo 工位号
    * @param alarmReason 报警原因（1=客户端掉线）
    */
   public void dealClientAlarm(String lineNo, String faceNo, Integer alarmReason) {
      LambdaQueryWrapper<AlarmRecord> query = Wrappers.<AlarmRecord>lambdaQuery()
         .eq(AlarmRecord::getLineNo, lineNo)
         .eq(AlarmRecord::getFaceNo, faceNo)
         .eq(AlarmRecord::getReason, alarmReason)
         .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue());
      List<AlarmRecord> list = this.list(query);
      if (CollectionUtils.isEmpty(list)) {
         return;
      }
      List<AlarmRecord> toUpdate = Lists.newArrayList();
      for (int i = 1; i < list.size(); i++) {
         list.get(i).setSolve(AlarmSolvedEnum.SOLVED.getValue());
         toUpdate.add(list.get(i));
      }
      if (CollectionUtils.isNotEmpty(toUpdate)) {
         this.updateBatchById(toUpdate);
      }
      this.deal(list.get(0).getUuid());
   }

   /**
    * W-X30b：PSM 同款 DealAlarmEvent 监听器。
    * 客户端重连时 StatusRecordServiceImpl.receiveStatus 发布此事件，
    * 触发清理该产线工位下旧的 UNSOLVED 掉线告警（只保留第一条）。
    */
   @Async
   @EventListener(DealAlarmEvent.class)
   public void dealClientAlarmListener(DealAlarmEvent event) {
      this.dealClientAlarm(event.getLineNo(), event.getFaceNo(), event.getReason());
   }

   @Override
   public BaseResult getAlarmListInfo(AlarmInfoQueryDTO alarmInfoQueryDTO) {
      // PSM 原版逻辑省略；DataupLoad 当前未启用。
      return BaseResult.build().ok().data(Collections.emptyList());
   }

   @Override
   public BaseResult handleAlarmNumGet() {
      // PSM 原版按 alarmRecordDAO.selectAlarmCountByType() 聚合；DataupLoad 当前直接返回 0/0。
      return BaseResult.build().ok().data(new com.hikrobotics.solution.module.alarm.dto.AlarmNumDTO());
   }

   @Override
   public BaseResult handleAlarmSearch(SearchAlarmDTO form) {
      // PSM 原版逻辑省略；DataupLoad 当前未启用。
      return BaseResult.build().ok().data(Collections.emptyList());
   }

   @Override
   public void sendAlarmTextMessage() {
      List<AlarmRecord> alarms = this.listNotResolveDefectAlarmRecord();
      try {
         WsMessage wsData = WsMessage.build().type(WsTypeEnum.ALARM.getValue()).data(alarms);
         this.webSocketHandler.broadcastByUid(wsData.toJsonString(), "web");
      } catch (Exception ex) {
         log.warn("broadcastByUid failed (likely no ws clients). cause: {}", ex.toString());
      }
   }

   @Override
   public List<AlarmRecord> listNotResolveDefectAlarmRecord() {
      List<String> enableAlarmDefects = this.defectTypeService.listByAttribute(1, DefectType::getAlarmEnable)
         .stream().map(DefectType::getName).toList();
      if (CollectionUtils.isNotEmpty(enableAlarmDefects)) {
         LambdaQueryWrapper<AlarmRecord> qw = Wrappers.<AlarmRecord>lambdaQuery()
            .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue())
            .in(AlarmRecord::getDefectName, enableAlarmDefects);
         List<AlarmRecord> list = this.list(qw);
         return list == null ? Lists.newArrayList() : list;
      }
      return Lists.newArrayList();
   }

   @Override
   public BaseResult handleAlarmIgnore(IgnoreAlarmDTO form) {
      // PSM 原版 handleAlarmIgnore（含 listNotResolveDefectAlarmRecord + updateBatchById）；
      // DataupLoad 当前 IIgnoreAlarmService 已提供独立入口，alarm 模块此处不再重复实现。
      return BaseResult.build().ok();
   }
}
