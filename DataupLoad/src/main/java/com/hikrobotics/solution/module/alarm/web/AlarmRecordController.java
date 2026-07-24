package com.hikrobotics.solution.module.alarm.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.dto.AlarmDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmDealDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmInfoQueryDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmQueryDTO;
import com.hikrobotics.solution.module.alarm.dto.IgnoreAlarmDTO;
import com.hikrobotics.solution.module.alarm.dto.SearchAlarmDTO;
import com.hikrobotics.solution.module.alarm.service.IAlarmRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * DataupLoad alarm 控制器（沿用 PSM AlarmRecordController 1:1 对齐）。
 *
 * <h2>W-ALM-03 — 6 个 HTTP endpoint 1:1 迁移</h2>
 * <p>
 * 此前 DataupLoad 只保留了 {@code POST /client/data/alarm}（客户端上报）一个端点，
 * 其余 6 个 PSM 同款管理后台端点（{@code /web/alarm/*}）缺失。本工单按 PSM
 * {@code AlarmRecordController} 反编译产物逐字迁回，调用 W-ALM-02 已实现的
 * 6 个 Service 方法（{@code listAll / deal / getAlarmListInfo / handleAlarmNumGet /
 * handleAlarmSearch / handleAlarmIgnore}）。
 *
 * <h3>端点清单（与 PSM 1:1 对齐）</h3>
 * <ol>
 *   <li>{@code GET  /web/alarm/list}         → {@link IAlarmRecordService#listAll(AlarmQueryDTO)}</li>
 *   <li>{@code GET  /web/alarm/num}          → {@link IAlarmRecordService#handleAlarmNumGet()}</li>
 *   <li>{@code POST /client/data/alarm}      → {@link IAlarmRecordService#add(AlarmDTO)}（沿用 W-B04 + W-X30）</li>
 *   <li>{@code POST /client/data/deal-alarm} → {@link IAlarmRecordService#deal(String)}（PSM 路径）</li>
 *   <li>{@code GET  /web/alarm/list-info}    → {@link IAlarmRecordService#getAlarmListInfo(AlarmInfoQueryDTO)}</li>
 *   <li>{@code GET  /web/alarm}              → {@link IAlarmRecordService#handleAlarmSearch(SearchAlarmDTO)}</li>
 *   <li>{@code PUT  /web/alarm/ignore}       → {@link IAlarmRecordService#handleAlarmIgnore(IgnoreAlarmDTO)}</li>
 * </ol>
 *
 * <h3>路径差异说明（相对 PSM 任务简报初稿）</h3>
 * <ul>
 *   <li>{@code deal} —— PSM 实际是 {@code POST /client/data/deal-alarm}（与 {@code addAlarmData} 同属客户端链路），
 *       不是任务简报初稿写的 {@code /web/alarm/deal}。本工单以 PSM 反编译为权威源。</li>
 *   <li>{@code search} —— PSM 实际是 {@code GET /web/alarm}（裸路径），不是 {@code /web/alarm/search}。</li>
 *   <li>{@code ignore} —— PSM 用 {@code @PutMapping}，不是 {@code @PostMapping}。</li>
 * </ul>
 *
 * <h3>DataupLoad 与 PSM 的差异点（保留记录）</h3>
 * <ol>
 *   <li>{@code addAlarmData} —— PSM 用 {@code ValidateUtils.validateEntity("alarm.addAlarmData", alarmDTO)}，
 *       DataupLoad 用 {@code @Validated}（项目里没有 {@code ValidateUtils} 工具类，沿用 Spring Validation 注解）。</li>
 *   <li>{@code addAlarmData} —— PSM 还会调 {@code ISystemConfigService}；DataupLoad 未启用该组件，
 *       仅依赖 Service 层已有的 {@code sendAlarmMessage()} → {@code PushAlarmEvent} 推送链路。</li>
 * </ol>
 *
 * @see com.hikrobotics.solution.module.alarm.service.impl.AlarmRecordServiceImpl
 */
@RestController
public class AlarmRecordController {
   private static final Logger log = LoggerFactory.getLogger(AlarmRecordController.class);

   @Autowired
   private IAlarmRecordService alarmRecordService;

   /**
    * W-B04 + W-X30 —— 客户端上报报警入口（沿用 PSM {@code addAlarmData} 语义）。
    * <p>
    * 入库 + 推送 yk 全链路已在 {@code AlarmRecordServiceImpl.add()} 内的
    * {@code sendAlarmMessage()} → {@code PushAlarmEvent} 完成；本 Controller 只负责
    * 入参校验与透传给 Service。
    */
   @PostMapping("/client/data/alarm")
   public BaseResult addAlarmData(@Validated @RequestBody AlarmDTO alarmDTO) {
      log.info("receive alarm: {}", alarmDTO);
      return this.alarmRecordService.add(alarmDTO);
   }

   /**
    * 处理报警（PSM {@code dealAlaram} 1:1）。
    * <p>
    * PSM 入参是 {@link AlarmDealDTO}（含 {@code @NotEmpty uuid} 字段），
    * 本工单沿用 PSM 路径 {@code POST /client/data/deal-alarm}（不是任务简报初稿的
    * {@code /web/alarm/deal} —— PSM 反编译为准）。
    */
   @PostMapping("/client/data/deal-alarm")
   public BaseResult dealAlaram(@RequestBody AlarmDealDTO alarmDealDTO) {
      return this.alarmRecordService.deal(alarmDealDTO.getUuid());
   }

   /**
    * Web 后台报警列表查询（PSM {@code getAlarmList} 1:1）。
    * <p>
    * 入参 {@link AlarmQueryDTO} 由 Spring 通过 {@code @ModelAttribute}（默认）从
    * query string 自动绑定，无需 {@code @RequestParam} 显式声明；PSM 同款也是裸
    * 方法参数形式。
    */
   @GetMapping("/web/alarm/list")
   public BaseResult getAlarmList(AlarmQueryDTO form) {
      return this.alarmRecordService.listAll(form);
   }

   /**
    * 大屏告警计数（PSM {@code getAlarmNum} 1:1）。
    * <p>
    * 无入参，返回 {@code AlarmNumDTO}（totalNum / highNum）。
    */
   @GetMapping("/web/alarm/num")
   public BaseResult getAlarmNum() {
      return this.alarmRecordService.handleAlarmNumGet();
   }

   /**
    * 报警详情+关联设备分页查询（PSM {@code getAlarmListInfo} 1:1）。
    * <p>
    * 入参 {@link AlarmInfoQueryDTO} 由 Spring 自动绑定。
    */
   @GetMapping("/web/alarm/list-info")
   public BaseResult getAlarmListInfo(AlarmInfoQueryDTO alarmInfoQueryDTO) {
      return this.alarmRecordService.getAlarmListInfo(alarmInfoQueryDTO);
   }

   /**
    * 多条件报警搜索（PSM {@code searchAlarmByType} 1:1）。
    * <p>
    * PSM 路径是裸 {@code GET /web/alarm}（不是任务简报初稿写的
    * {@code /web/alarm/search}），入参 {@link SearchAlarmDTO} 由 Spring 自动绑定
    * 并经 {@code @Validated} 触发 jakarta.validation 校验。
    */
   @GetMapping("/web/alarm")
   public BaseResult searchAlarmByType(@Validated SearchAlarmDTO form) {
      return this.alarmRecordService.handleAlarmSearch(form);
   }

   /**
    * 忽略报警（PSM {@code ignoreAlarm} 1:1）。
    * <p>
    * PSM 用 {@code @PutMapping}（不是任务简报初稿写的 {@code @PostMapping}），
    * 入参 {@link IgnoreAlarmDTO} 用 {@code @RequestBody}（批量忽略时 body 较大，
    * 不适合 query string）。
    */
   @PutMapping("/web/alarm/ignore")
   public BaseResult ignoreAlarm(@RequestBody IgnoreAlarmDTO form) {
      return this.alarmRecordService.handleAlarmIgnore(form);
   }
}
