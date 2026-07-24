package com.hikrobotics.solution.module.alarm.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.dto.AlarmDTO;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import com.hikrobotics.solution.module.alarm.service.IAlarmRecordService;
import com.hikrobotics.solution.module.yingke.service.IYKService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * DataupLoad alarm 控制器（沿用 PSM AlarmRecordController 1:1，关键方法 addAlarmData 不变）。
 * <p>
 * W-B04 关注点：
 * <ol>
 *   <li>{@code POST /client/data/alarm} —— 客户端上报报警入口</li>
 *   <li>PSM 原版：仅写 alarm_record → ISystemConfigService → WS 推送；DataupLoad 在此基础上同时
 *       触发 {@link IYKService#pushAlarm(AlarmRecord)}，由 yk 推 MES（沿用老板口径链路）。</li>
 * </ol>
 */
@RestController
public class AlarmRecordController {
   private static final Logger log = LoggerFactory.getLogger(AlarmRecordController.class);
   @Autowired
   private IAlarmRecordService alarmRecordService;
   @Autowired
   private IYKService ykService;

   /**
    * 接收工控机报警数据，入库 + 推送 yk。
    * <p>
    * <b>W-X30 修复</b>：Controller 只负责入库；推送链路已由 {@link AlarmRecordServiceImpl#add(AlarmDTO)}
    * 内部的 {@code sendAlarmMessage()} → {@code PushAlarmEvent} 完整覆盖，
    * 删除此处重复的 {@code ykService.pushAlarm()} 调用（原代码导致每条报警被推 yk 两次）。
    */
   @PostMapping("/client/data/alarm")
   public BaseResult addAlarmData(@Validated @RequestBody AlarmDTO alarmDTO) {
      log.info("receive alarm: {}", alarmDTO);
      return this.alarmRecordService.add(alarmDTO);
   }
}
