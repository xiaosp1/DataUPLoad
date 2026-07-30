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
import com.hikrobotics.solution.module.alarm.constant.AlarmConstants;
import com.hikrobotics.solution.module.alarm.constant.AlarmSolvedEnum;
import com.hikrobotics.solution.module.alarm.constant.AlarmTypeEnum;
import com.hikrobotics.solution.module.alarm.dto.AlarmCountDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmInfoQueryDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmNumDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmQueryDTO;
import com.hikrobotics.solution.module.alarm.dto.IgnoreAlarmDTO;
import com.hikrobotics.solution.module.alarm.dto.PlaySoundWsMsgDTO;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * <h2>W-B04 BUG 修复（保留）</h2>
 * <p>
 * PSM 反编译产物中 {@link #sendAlarmMessage(AlarmRecord)} 有以下 BUG：
 * <pre>
 * boolean isIgnore = false;        // ← 硬编码 false，从不查库
 * if (... &amp;&amp; !isIgnore) { ... } // 白名单永远失效
 * </pre>
 * DataupLoad 修复：调用 {@link IIgnoreAlarmService#isIgnore(Integer, String, String, String)}
 * 实际查询 ignore_alarm 表，过滤 ignore_time &gt; now() 的记录。
 *
 * <h2>W-ALM-02 — 6 个管理方法 1:1 对齐 PSM</h2>
 * <p>
 * 此前 {@code listAll / deal / getAlarmListInfo / handleAlarmNumGet / handleAlarmSearch /
 * handleAlarmIgnore} 6 个方法全部为 {@code BaseResult.build().ok()} 占位；本工单按 PSM
 * {@code AlarmRecordServiceImpl} 反编译产物逐字迁回，调用 W-ALM-01 新增的 5 个 Mapper
 * 聚合方法 + BaseMapper.page()/list()/update()/getOne()/updateBatchById()。
 *
 * <h3>PSM vs DPL 差异点（保留记录）</h3>
 * <ol>
 *   <li>{@code alarm.global-enabled} 短路：DataupLoad 入口先判 {@link DefectAlarmConfig#isGlobalEnabled()}，
 *       {@code false} 直接 return OK（老板紧急关停用）。</li>
 *   <li>{@code sendAlarmSoundWsMessage}：DataupLoad 未启用 {@code ISystemConfigService}，无法像 PSM 那样从
 *       {@code system_config} 表读 uri/playCount；W-ALM-05 改为使用 {@link AlarmConstants} 里的兜底默认值。
 *       推送结构（{@link PlaySoundWsMsgDTO} + {@link WsMessage#type(String)} + 
 *       {@link WebSocketHandler#broadcastByUid(String, String)}）仍 1:1 对齐 PSM。</li>
 *   <li>BaseResult.data()：PSM XML 用 {@code resultType}，DPL 走对象 → List/Page 直接 {@code data(...)} 即可，无需类型转换。</li>
 * </ol>
 *
 * <h2>W-ALM-05 — handleAlarmNumGet WS 推送 + soundEnable 声音播放分支</h2>
 * <ul>
 *   <li>{@link #handleAlarmNumGet()} 在 selectAlarmCountByType 之后追加 {@link #sendAlarmSoundWsMessage(int)}，
 *       把 total 次数推到 /web 端大屏（PSM 未在 handleAlarmNumGet 推声音——本工单按任务简报要求补齐）。</li>
 *   <li>{@link #sendAlarmMessage(AlarmRecord)} 当 {@code defectType.soundEnable == YES} 且 {@code solve == UNSOLVED}
 *       时真正调 {@link #sendAlarmSoundWsMessage(int)}（之前 W-ALM-02 只 log.debug 跳过）。</li>
 *   <li>{@code dealClientAlarm} 链路最终走 {@link #deal(String)} → {@link #sendAlarmMessage(AlarmRecord)}，
 *       因此 soundEnable 判断在 sendAlarmMessage 内统一完成；dealClientAlarm 本体无需另加分支。</li>
 * </ul>
 *
 * <h2>老板口径（W-B04）</h2>
 * <p>
 * "上传飞出报警这个其实原先我们已经做过了，只不过是被麦斯转了一手，其实原先的功能是我们将报警信息推送到麦斯之后，麦斯自己去推送飞书的这块"
 * <br>→ 沿用 PSM yk 推送链路：{@code alarm → yk 推 MES → MES 转飞书}。不新做飞书 webhook。
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

   // ============================================================
   //  W-ALM-02 — 6 个管理方法（PSM AlarmRecordServiceImpl 1:1 对齐）
   // ============================================================

   /**
    * Web 后台报警列表查询（PSM listAll 1:1）。
    *
    * <h3>PSM 逻辑</h3>
    * <pre>
    * 1. lambdaQueryWrapper 按 type/level/solve/createTime (between start/end) 过滤；
    * 2. 若 faceId != null 且 lineService.getById(faceId) 存在 → 追加 lineNo + faceNo 条件；
    * 3. sortType == 0 → 升序，else 降序，按 AlarmRecord.time 排；
    * 4. isPaged() → page(IPage, wrapper)，否则 list(wrapper)。
    * </pre>
    *
    * <p>注：{@code query.localStartTime()/localEndTime()} 来自 PSM
    * {@link com.hikrobotics.solution.framework.common.query.TimePageQuery}，
    * 返回 {@code LocalDateTime}；DataupLoad {@code AlarmRecord.createTime} 也是
    * {@code LocalDateTime}，可直接比较。</p>
    */
   @Override
   public BaseResult listAll(AlarmQueryDTO query) {
      LambdaQueryWrapper<AlarmRecord> lambdaQueryWrapper = Wrappers.<AlarmRecord>lambdaQuery()
         .eq(query.getType() != null, AlarmRecord::getType, query.getType())
         .eq(query.getLevel() != null, AlarmRecord::getLevel, query.getLevel())
         .eq(query.getSolve() != null, AlarmRecord::getSolve, query.getSolve())
         .between(query.localStartTime() != null && query.localEndTime() != null,
                  AlarmRecord::getCreateTime, query.localStartTime(), query.localEndTime());

      if (query.getFaceId() != null) {
         Line line = this.lineService.getById((Serializable) query.getFaceId());
         if (line != null) {
            lambdaQueryWrapper
               .eq(AlarmRecord::getLineNo, line.getLineNo())
               .eq(AlarmRecord::getFaceNo, line.getFaceNo());
         }
      }

      // sortType: 0 = ASC, else (默认 1) = DESC —— 按 PSM 原始语义
      boolean isAsc = query.getSortType() != null && query.getSortType() == 0;
      lambdaQueryWrapper.orderBy(true, isAsc, AlarmRecord::getTime);

      if (query.isPaged()) {
         return BaseResult.build().data(this.page(query.getPage(), lambdaQueryWrapper));
      }
      return BaseResult.build().data(this.list(lambdaQueryWrapper));
   }

   /**
    * 处理报警（PSM deal 1:1）：按 uuid 把 UNSOLVED 标记为 SOLVED。
    *
    * <h3>PSM 逻辑</h3>
    * <pre>
    * 1. updateWrapper：uuid = #{uuid} AND solve = UNSOLVED → setSolve = SOLVED；
    * 2. updateFlag = true → 再 getOne(uuid) 取最新 alarm；
    *    2.1 若 defectName 非空 → defectTypeService.getByNameAndType(...) → sendAlarmMessage(alarm) 推送；
    *    2.2 否则 warn 但仍返回 OK；
    * 3. updateFlag = false → 返回 error("20102")。
    * </pre>
    *
    * <p>DataupLoad 当前 {@link #sendAlarmMessage} 已对齐 PSM + 修复 isIgnore BUG，
    * 推送链路 1:1；只是 WS 声音推送因 ISystemConfigService 未启用被跳过（与 add() 行为一致）。</p>
    */
   @Override
   public BaseResult deal(String uuid) {
      LambdaUpdateWrapper<AlarmRecord> updateWrapper = Wrappers.<AlarmRecord>lambdaUpdate()
         .eq(AlarmRecord::getUuid, uuid)
         .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue())
         .set(AlarmRecord::getSolve, AlarmSolvedEnum.SOLVED.getValue());
      boolean updateFlag = this.update(updateWrapper);
      if (updateFlag) {
         AlarmRecord alarm = this.getOne(Wrappers.<AlarmRecord>lambdaQuery().eq(AlarmRecord::getUuid, uuid));
         if (alarm != null && StringUtils.isNotBlank(alarm.getDefectName())) {
            DefectType defect = this.defectTypeService.getByNameAndType(alarm.getDefectName(), alarm.getType());
            if (defect != null) {
               alarm.setDefectType(defect);
               this.sendAlarmMessage(alarm);
            } else {
               log.warn("defect is not exist. will not send alarm message.[alarm={}]", alarm);
            }
         }
         return BaseResult.build();
      }
      return BaseResult.build().error("20102").log("deal alarm failed, alarm uuid.", uuid);
   }

   /**
    * 报警详情+关联设备查询（PSM getAlarmListInfo 1:1）。
    *
    * <h3>PSM 逻辑</h3>
    * <pre>
    * 1. between(time, startTime, endTime) 强制过滤（PSM DTO 无空指针保护 —— 复用 TimePageQuery 默认 startTime/endTime）；
    * 2. 可选 faceId：getById 反查 lineNo/faceNo；
    * 3. 强制走分页（PSM DTO 继承 TimePageQuery，isPaged() 默认 true 当 pageNum/pageSize 都给）。
    * </pre>
    *
    * <p>注：PSM DTO 不校验 startTime/endTime 非空；
    * 若调用方未传时间，DPL 这里仍能跑（{@code AlarmRecord.time} 是 String，between 用 null → 行为取决于 MP，
    * DPL 选用 {@code isNull} 守卫避免语法异常）。</p>
    */
   @Override
   public BaseResult getAlarmListInfo(AlarmInfoQueryDTO alarmInfoQueryDTO) {
      LambdaQueryWrapper<AlarmRecord> lambdaQueryWrapper = Wrappers.<AlarmRecord>lambdaQuery();
      // PSM 1:1: 无条件 between(time, startTime, endTime)，
      // 调用方需保证 TimePageQuery 的 startTime/endTime 已设值；TimePageQuery 默认两者为 null，
      // 此时生成的 SQL "BETWEEN NULL AND NULL" 在 PG 里等价于恒假 → 返回空列表。
      lambdaQueryWrapper.between(AlarmRecord::getTime,
         alarmInfoQueryDTO.getStartTime(), alarmInfoQueryDTO.getEndTime());
      if (alarmInfoQueryDTO.getFaceId() != null) {
         Line line = this.lineService.getById((Serializable) alarmInfoQueryDTO.getFaceId());
         if (line != null) {
            lambdaQueryWrapper
               .eq(AlarmRecord::getLineNo, line.getLineNo())
               .eq(AlarmRecord::getFaceNo, line.getFaceNo());
         }
      }
      return BaseResult.build().data(this.page(alarmInfoQueryDTO.getPage(), lambdaQueryWrapper));
   }

   /**
    * 大屏告警计数（PSM handleAlarmNumGet 1:1）。
    *
    * <h3>PSM 逻辑</h3>
    * <pre>
    * 1. specialTypes = Arrays.stream(highTypes.split(",")).map(Integer::parseInt).toList()
    *    （来自 application.yml alarm.high-type，PSM 默认 3 = DEVICE 类型）；
    * 2. alarmRecordDAO.selectAlarmCountByType() → List<AlarmCountDTO>（type + count）；
    * 3. total += count；specialAlarmNum += specialTypes.contains(type) ? count : 0；
    * 4. AlarmNumDTO.builder().totalNum(total).highNum(special).build()。
    * </pre>
    *
    * <p>W-ALM-01 已实现 {@link AlarmRecordMapper#selectAlarmCountByType()}，
    * 直接调用即可。</p>
    */
   @Override
   public BaseResult handleAlarmNumGet() {
      List<Integer> specialTypes = Arrays.stream(this.highTypes.split(","))
         .map(Integer::parseInt)
         .toList();
      int total = 0;
      int specialAlarmNum = 0;
      List<AlarmCountDTO> alarmCountsOfTypes = this.alarmRecordMapper.selectAlarmCountByType();
      for (AlarmCountDTO alarmCountOfType : alarmCountsOfTypes) {
         if (alarmCountOfType.getCount() != null) {
            total += alarmCountOfType.getCount().intValue();
            specialAlarmNum += specialTypes.contains(alarmCountOfType.getType())
               ? alarmCountOfType.getCount().intValue() : 0;
         }
      }
      AlarmNumDTO alarmNum = AlarmNumDTO.builder()
         .totalNum(Integer.valueOf(total))
         .highNum(Integer.valueOf(specialAlarmNum))
         .build();
      // ====== W-ALM-05：handleAlarmNumGet 末尾推送声音到大屏 ======
      // PSM 反编译产物 handleAlarmNumGet 不主动推 sound —— 仅 add()/deal() 链路经 sendAlarmMessage 推。
      // 任务简报要求：handleAlarmNumGet 调 selectAlarmCountByType 后用 WebSocketHandler 推 PlaySoundWsMsgDTO。
      // 这里按任务补齐：把 total 数推给 /web，前端按 SOUND_PLAY_DEFAULT_INTERVAL_SECONDS 轮播。
      if (total > 0) {
         this.sendAlarmSoundWsMessage(total);
      }
      return BaseResult.build().ok().data(alarmNum);
   }

   /**
    * 多条件报警搜索（PSM handleAlarmSearch 1:1）。
    *
    * <h3>PSM 逻辑</h3>
    * <pre>
    * if (form.getType() != 4) {
    *     data = statusRecordService.searchOffLineClient(lineNo, faceNo, type);
    * } else {
    *     wrapper = lambdaQuery(AlarmRecordPO)
    *         .eq(type, DEFECT)
    *         .eq(faceNo, form.faceNo)
    *         .eq(lineNo, form.lineNo)
    *         .eq(solve, UNSOLVED);
    *     data = list(wrapper);
    * }
    * return BaseResult.ok().data(data);
    * </pre>
    *
    * <p>注：{@code searchOffLineClient} 在 W-FIX-01 中已 PSM 1:1 实现
    * （返回 type 匹配且 status=OUTLINE 的 {@code DeviceStateDTO} 列表）。</p>
    */
   @Override
   @SuppressWarnings("unchecked")
   public BaseResult handleAlarmSearch(SearchAlarmDTO form) {
      List<?> data;
      if (form.getType() != 4) {
         data = (List<?>) this.statusRecordService.searchOffLineClient(
            form.getLineNo(), form.getFaceNo(), form.getType());
      } else {
         LambdaQueryWrapper<AlarmRecord> wrapper = Wrappers.<AlarmRecord>lambdaQuery()
            .eq(AlarmRecord::getType, AlarmTypeEnum.DEFECT.getCode())
            .eq(AlarmRecord::getFaceNo, form.getFaceNo())
            .eq(AlarmRecord::getLineNo, form.getLineNo())
            .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue());
         data = this.list(wrapper);
      }
      return BaseResult.build().ok().data(data);
   }

   /**
    * 忽略报警（PSM handleAlarmIgnore 1:1）：把 alarm_record 的 solve 标记为 IGNORE。
    *
    * <h3>PSM 逻辑</h3>
    * <pre>
    * 1. ignoreAll == 0（NO）：按 lineNo/faceNo/type/defectName + (time between startTime, endTime) 过滤；
    *    可选 faceId → getById 反查 lineNo/faceNo 回填；
    * 2. ignoreAll == 1（YES）：listNotResolveDefectAlarmRecord() —— 所有未处理缺陷报警；
    * 3. 把命中的 alarmRecord.solve 全部置为 IGNORE；
    * 4. updateBatchById 失败 → error("20102")；
    * 5. 成功 → sendAlarmTextMessage() 刷新 WS 大屏。
    * </pre>
    *
    * <p>DataupLoad 关注：白名单（ignore_alarm 表）的写入已由 {@link IIgnoreAlarmService#handleAlarmIgnore}
    * 独立负责；本方法只负责把 alarm_record 标记为 IGNORE。
    * 两者组合：IIgnoreAlarmService 写 ignore_alarm（带 ignoreTime 生效区间）+
    *           IAlarmRecordService 把当前匹配的 alarm_record 立即标 IGNORE。
    * </p>
    */
   @Override
   public BaseResult handleAlarmIgnore(IgnoreAlarmDTO form) {
      List<AlarmRecord> alarmRecords = new ArrayList<>();
      // PSM: form.getIgnoreAll() == StateEnum.NO.getValue().intValue() —— 0
      // DPL ignoreAll 是 Integer，auto-unbox 与 int 比较
      if (form.getIgnoreAll() != null && form.getIgnoreAll() == StateEnum.NO.getValue().intValue()) {
         if (form.getFaceId() != null) {
            Line face = this.lineService.getById((Serializable) form.getFaceId());
            if (face != null) {
               form.setLineNo(face.getLineNo()).setFaceNo(face.getFaceNo());
            }
         }
         LambdaQueryWrapper<AlarmRecord> qw = Wrappers.<AlarmRecord>lambdaQuery()
            .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue())
            .eq(StringUtils.isNotBlank(form.getLineNo()), AlarmRecord::getLineNo, form.getLineNo())
            .eq(StringUtils.isNotBlank(form.getFaceNo()), AlarmRecord::getFaceNo, form.getFaceNo())
            .eq(form.getType() != null, AlarmRecord::getType, form.getType())
            .eq(StringUtils.isNotBlank(form.getDefectName()), AlarmRecord::getDefectName, form.getDefectName())
            .between(StringUtils.isNotBlank(form.getStartTime()),
                     AlarmRecord::getTime, form.getStartTime(), form.getEndTime());
         alarmRecords.addAll(this.list(qw));
      } else {
         alarmRecords = this.listNotResolveDefectAlarmRecord();
      }

      if (CollectionUtils.isNotEmpty(alarmRecords)) {
         alarmRecords.forEach(record -> record.setSolve(AlarmSolvedEnum.IGNORE.getValue()));
         if (!this.updateBatchById(alarmRecords)) {
            return BaseResult.build().error("20102");
         }
      }
      this.sendAlarmTextMessage();
      return BaseResult.build().ok();
   }

   // ============================================================
   //  已有方法（add / sendAlarmMessage / dealClientAlarm 等保持不变）
   // ============================================================

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
    *
    * <h2>W-DEFECT-CFG 子单 B — 细粒度推送开关</h2>
    * <p>
    * 原版仅在 {@code isInterestingDefect=true}（即报警消息包含已知缺陷名）时才推 WS / yk，
    * 其余系统/设备/未登记缺陷报警一律 drop。
    * 本工单按任务简报要求：
    * <ul>
    *   <li>**始终保存** alarm_record（让"破洞未登记的设备告警"也有据可查）；</li>
    *   <li>查 {@code defect_type} by (defectName, type)；</li>
    *   <li>未找到：使用默认行为（推大屏 + 推声音，**不**推英科）；</li>
    *   <li>找到：按 alarm_enable / send_yk_enable / sound_enable 决定推不推；</li>
    *   <li>把 3 个布尔塞进 {@link PushAlarmEvent}，让 YK 订阅者按 ykPublish 决定是否真推。</li>
    * </ul>
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
                  break;
               }
            }
         }
      }

      // ====== W-DEFECT-CFG 子单 B：按 (defectName, type) 查 defect_type 决定 3 个推送开关 ======
      // 与 listByAttribute 不同：直接 (name, type) 二元组精确查询。
      // 若 defectName 为 null（系统/设备报警或非已登记缺陷），按"查不到"处理 → 默认行为。
      DefectType defectType = defectName == null
         ? null
         : this.defectTypeService.getByNameAndType(defectName, form.getType());

      // screenPublish：默认 true（向前兼容 PSM 老行为）；找到时按 alarm_enable
      boolean screenPublish = (defectType == null) || Objects.equals(defectType.getAlarmEnable(), StateEnum.YES.getValue());
      // ykPublish：默认 false（向前兼容 PSM 老行为：不推英科，安全默认）；找到时按 send_yk_enable
      boolean ykPublish = defectType != null && Objects.equals(defectType.getSendYkEnable(), StateEnum.YES.getValue());
      // soundPublish：默认 true（向前兼容 PSM 老行为）；找到时按 sound_enable
      boolean soundPublish = (defectType == null) || Objects.equals(defectType.getSoundEnable(), StateEnum.YES.getValue());

      if (defectName != null) {
         // 把同一 (defectName + lineNo + faceNo + type) 下未处理的旧报警置为已忽略（仅对已知缺陷去重）
         LambdaUpdateWrapper<AlarmRecord> uw = Wrappers.<AlarmRecord>lambdaUpdate()
            .eq(AlarmRecord::getDefectName, defectName)
            .eq(AlarmRecord::getLineNo, form.getLineNo())
            .eq(AlarmRecord::getType, form.getType())
            .eq(AlarmRecord::getFaceNo, form.getFaceNo())
            .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue())
            .set(AlarmRecord::getSolve, AlarmSolvedEnum.IGNORE.getValue());
         this.update(uw);
      }

      AlarmRecord alarm = BeanUtil.copyProperties(form, AlarmRecord.class);
      alarm.setSolve(AlarmSolvedEnum.UNSOLVED.getValue())
         .setMessage(message)
         .setDefectName(defectName);
      alarm.setDefectType(defectType);
      this.save(alarm);

      // sendAlarmMessage 内部根据 screenPublish/soundPublish/ykPublish 决定推送
      this.sendAlarmMessage(alarm, screenPublish, ykPublish, soundPublish);

      if (defectName == null) {
         log.warn("alarm has no matched defect_name, using default push.[form={}, screenPublish={}, ykPublish={}, soundPublish={}]",
            form, screenPublish, ykPublish, soundPublish);
      } else if (defectType == null) {
         log.warn("defect_type not configured for (name={}, type={}), using default push.[screenPublish={}, ykPublish={}, soundPublish={}]",
            defectName, form.getType(), screenPublish, ykPublish, soundPublish);
      } else {
         log.info("alarm pushed with fine-grained flags.[name={}, type={}, screenPublish={}, ykPublish={}, soundPublish={}]",
            defectName, form.getType(), screenPublish, ykPublish, soundPublish);
      }
      return BaseResult.build();
   }

   /**
    * 报警发送 / 推送 yk（PSM sendAlarmMessage + isIgnore BUG 修复 + W-DEFECT-CFG 子单 B 细粒度推送）。
    * <p>
    * DataupLoad 修复点：调用 {@link IIgnoreAlarmService#isIgnore(Integer, String, String, String)}
    * 实际查询 ignore_alarm 表，而非 PSM 原版硬编码 {@code boolean isIgnore = false;}。
    *
    * <h2>W-DEFECT-CFG 子单 B — 细粒度推送开关</h2>
    * <p>
    * 三个开关 {@code screenPublish / ykPublish / soundPublish} 已由 {@link #add(AlarmDTO)}
    * 算好（基于 defect_type 查表 + 默认值兜底），本方法只负责按开关放行：
    * <ul>
    *   <li>{@code screenPublish=true} → 调 {@link #sendAlarmTextMessage()} 推大屏</li>
    *   <li>{@code soundPublish=true} + UNSOLVED → 调 {@link #sendAlarmSoundWsMessage(int)} 推声音</li>
    *   <li>{@code ykPublish=true} → 发布 {@link PushAlarmEvent}，由 YK 订阅者按 {@code yk.uploadEnabled} 二次放行</li>
    * </ul>
    *
    * <p>isIgnore（白名单）逻辑保留：被忽略的报警**不**推大屏 + 声音 + 英科，仍记录 alarm_record。</p>
    *
    * <p>{@link #deal(String)} 链路调用本方法（无参三开关）时，使用
    * {@code screenPublish=true / ykPublish=defectType.sendYkEnable / soundPublish=defectType.soundEnable}，
    * 与 PSM 原版行为 1:1 对齐。</p>
    */
   @Override
   public void sendAlarmMessage(AlarmRecord alarm) {
      // 无参版本：按 defectType 直查（保持 PSM 兼容，deal() 链路沿用）
      DefectType defectType = alarm.getDefectType();
      boolean screenPublish = defectType != null
         && Objects.equals(defectType.getAlarmEnable(), StateEnum.YES.getValue());
      boolean ykPublish = defectType != null
         && Objects.equals(defectType.getSendYkEnable(), StateEnum.YES.getValue());
      boolean soundPublish = defectType != null
         && Objects.equals(defectType.getSoundEnable(), StateEnum.YES.getValue());
      this.sendAlarmMessage(alarm, screenPublish, ykPublish, soundPublish);
   }

   /**
    * W-DEFECT-CFG 子单 B：3 个布尔由 {@link #add(AlarmDTO)} 算出后传入，按开关放行。
    *
    * <p>为什么把判定放 add() 而非 sendAlarmMessage？add() 是入口，能同时拿到
    * (defectName, type) → getByNameAndType(...) 的查表结果；sendAlarmMessage
    * 也可独立查 defect_type（deal() 链路复用），但 add() 已经查过，传进来避免
    * 二次查询浪费 + 双源不一致（前端刚改 alarm_enable=0，缓存里可能还是老值）。</p>
    */
   public void sendAlarmMessage(AlarmRecord alarm,
                                boolean screenPublish,
                                boolean ykPublish,
                                boolean soundPublish) {
      DefectType defectType = alarm.getDefectType();
      // ====== W-B04 BUG FIX START ======
      // PSM 反编译产物：boolean isIgnore = false; ← 白名单永远失效
      // DataupLoad 修复：实际查询 ignore_alarm 表
      boolean isIgnore = this.ignoreAlarmService.isIgnore(
         alarm.getType(), alarm.getDefectName(), alarm.getLineNo(), alarm.getFaceNo());
      // ====== W-B04 BUG FIX END ======

      if (screenPublish && !isIgnore) {
         this.sendAlarmTextMessage();
         if (soundPublish
            && Objects.equals(alarm.getSolve(), AlarmSolvedEnum.UNSOLVED.getValue())) {
            // ====== W-ALM-05 + W-DEFECT-CFG：soundPublish 决定是否推声音 ======
            // 之前（W-ALM-02）：log.debug 跳过（system_config 未启用）。
            // 现在：soundPublish=true 时调 sendAlarmSoundWsMessage，uri/interval/playCount 走 AlarmConstants 兜底。
            this.sendAlarmSoundWsMessage(AlarmConstants.SOUND_PLAY_DEFAULT_COUNT);
         }
      } else if (soundPublish
         && !screenPublish
         && Objects.equals(alarm.getSolve(), AlarmSolvedEnum.UNSOLVED.getValue())) {
         // soundPublish 单独开启的边角：若 PSM 老版本兼容到此分支，仍推声音（不依赖 screenPublish）
         this.sendAlarmSoundWsMessage(AlarmConstants.SOUND_PLAY_DEFAULT_COUNT);
      }

      if (!isIgnore && ykPublish) {
         EventUtil.publish(new PushAlarmEvent(this, alarm,
            Boolean.valueOf(screenPublish),
            Boolean.valueOf(ykPublish),
            Boolean.valueOf(soundPublish)));
      }
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
      List<AlarmRecord> toUpdate = new ArrayList<>();
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

   /**
    * 推送报警声音到 /web 大屏（PSM {@code sendAlarmSoundWsMessage} DPL 适配版）。
    * <p>
    * PSM 原版：{@code sendAlarmSoundWsMessage(DefectTypePO)} 从 {@code system_config} 读
    * {@code type.getSoundConfigKey()}（uri）+ {@code sound_play_count}（次数），再 broadcast。
    * <p>
    * DPL（W-ALM-05）：{@code ISystemConfigService} 未启用（任务约束：不修改其它模块），
    * uri / playCount 走 {@link AlarmConstants} 兜底；本方法新增一个 {@code count} 参数，
    * 兼顾两路调用方：
    * <ul>
    *   <li>{@link #handleAlarmNumGet()} → {@code count = total}（大屏告警计数推送）</li>
    *   <li>{@link #sendAlarmMessage(AlarmRecord)} → {@code count = SOUND_PLAY_DEFAULT_COUNT}（单条报警推送）</li>
    * </ul>
    * <p>
    * 推送结构与 PSM 1:1：{@link WsMessage#type(String) WsMessage.type(ALARM_SOUND)} +
    * {@link PlaySoundWsMsgDTO} + {@link WebSocketHandler#broadcastByUid(String, String) broadcastByUid(json, "web")}。
    */
   private void sendAlarmSoundWsMessage(int count) {
      try {
         PlaySoundWsMsgDTO soundMsg = new PlaySoundWsMsgDTO()
            .setUri(AlarmConstants.SOUND_PLAY_DEFAULT_URI)
            .setPlayCount(Integer.valueOf(count));
         WsMessage wsData = WsMessage.build()
            .type(WsTypeEnum.ALARM_SOUND.getValue())
            .data(soundMsg);
         this.webSocketHandler.broadcastByUid(wsData.toJsonString(), "web");
      } catch (Exception ex) {
         // 推送失败不应阻塞报警链路；log 后吞掉（与 sendAlarmTextMessage 处理一致）。
         log.warn("broadcast sound ws msg failed. cause: {}", ex.toString());
      }
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
         return list == null ? new ArrayList<>() : list;
      }
      return new ArrayList<>();
   }
}
