package com.hikrobotics.solution.module.alarm.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.dto.IgnoreAlarmDTO;
import com.hikrobotics.solution.module.alarm.service.IIgnoreAlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DataupLoad 忽略报警 HTTP 接口（PSM AlarmRecordController.ignoreAlarm 拆出独立 Controller）。
 *
 * <p>W-F02-B 关注点：</p>
 * <ol>
 *   <li>{@code POST /web/alarm/ignore} —— 添加忽略（透传 {@link IgnoreAlarmDTO} 给
 *       {@link IIgnoreAlarmService#handleAlarmIgnore}，由 alarm 模块统一刷新 alarm_record）。</li>
 *   <li>{@code DELETE /web/alarm/ignore/{id}} —— 删除单条忽略记录。</li>
 *   <li>{@code GET /web/alarm/ignore} —— 列出当前生效中的忽略白名单。</li>
 *   <li>{@code GET /web/alarm/ignore/check} —— 查询 (type + lineNo + faceNo + defectName)
 *       是否在忽略白名单内（外部接口入参顺序沿用 PSM AlarmRecordController.ignoreAlarm 习惯）。</li>
 * </ol>
 *
 * <p>本工单任务声明：PSM 反编译未拿到 IgnoreAlarmController.class，故按 IIgnoreAlarmService
 * 公开方法 1:1 暴露 HTTP，参数顺序沿用 PSM 业务侧最常出现的 (type, lineNo, faceNo, defectName)。</p>
 */
@RestController
@RequestMapping("/web/alarm/ignore")
public class IgnoreAlarmController {
   private static final Logger log = LoggerFactory.getLogger(IgnoreAlarmController.class);

   @Autowired
   private IIgnoreAlarmService ignoreAlarmService;

   @PostMapping("/")
   public BaseResult add(@RequestBody IgnoreAlarmDTO form) {
      log.info("add ignore alarm: {}", form);
      return this.ignoreAlarmService.handleAlarmIgnore(form);
   }

   @DeleteMapping("/{id}")
   public BaseResult remove(@PathVariable("id") Integer id) {
      log.info("remove ignore alarm id={}", id);
      boolean ok = this.ignoreAlarmService.removeById(id);
      return ok ? BaseResult.build().ok() : BaseResult.build().error();
   }

   @GetMapping("/")
   public BaseResult list() {
      return BaseResult.build().ok().data(this.ignoreAlarmService.getIgnoreDefect());
   }

   @GetMapping("/check")
   public BaseResult check(@RequestParam("type") Integer type,
                           @RequestParam("lineNo") String lineNo,
                           @RequestParam("faceNo") String faceNo,
                           @RequestParam("defectName") String defectName) {
      // IIgnoreAlarmService.isIgnore 签名顺序：(type, defectName, lineNo, faceNo)
      boolean ignored = this.ignoreAlarmService.isIgnore(type, defectName, lineNo, faceNo);
      return BaseResult.build().ok().data(ignored);
   }
}
