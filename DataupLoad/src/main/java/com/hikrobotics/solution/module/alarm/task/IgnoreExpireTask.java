package com.hikrobotics.solution.module.alarm.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hikrobotics.solution.module.alarm.entity.IgnoreAlarm;
import com.hikrobotics.solution.module.alarm.service.IIgnoreAlarmService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DataupLoad 忽略报警过期清理定时任务（沿用 PSM {@code AlarmTaskManager.delExpireIgnoreDefect} 语义）。
 *
 * <p>职责：每小时整点扫描 {@code ignore_alarm} 表，删除 {@code ignore_time} 已过期（小于当前时间）
 * 的记录，避免表无限增长。</p>
 *
 * <p>与 PSM 的差异：
 * <ul>
 *   <li>PSM 原版 cron 为 {@code "0 0 1 * * ?"}（每天凌晨 1 点），DataupLoad 按 W-F01-D 改为
 *       {@code "0 0 * * * ?"}（每小时整点），以更快回收已过期的忽略配置；</li>
 *   <li>PSM 原版方法体内仅调用 {@code ignoreAlarmService.removeExpire()}，DataupLoad 额外
 *       统计受影响行数用于运维日志，统计与删除通过 {@link IIgnoreAlarmService} 完成，<b>不</b>
 *       强依赖 {@code removeExpire()} 的返回类型，便于后续 W-F02-A 重构签名。</li>
 * </ul>
 * </p>
 *
 * <p>W-F01-D 工单：仅新建本文件，不修改 {@code application-prod.yml} 与 Service 实现。</p>
 */
@Component
public class IgnoreExpireTask {

    private static final Logger log = LoggerFactory.getLogger(IgnoreExpireTask.class);

    @Autowired
    private IIgnoreAlarmService ignoreAlarmService;

    /**
     * 每小时整点清理过期的 ignore_alarm 记录。
     *
     * <p>实现策略：先按 {@code ignore_time < now()} 统计即将清理的条数，再调用
     * {@link IIgnoreAlarmService#removeExpire()} 执行删除，最后按规范输出
     * {@code "ignore expire alarm removed. count={}"} 日志。</p>
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void delExpireIgnoreDefect() {
        try {
            // W-X17a 修复：PG ignore_alarm.end_time 是 varchar(19) "yyyy-MM-dd HH:mm:ss"，
            // 不能用 LocalDateTime<->timestamp 比较（operator does not exist: character varying < timestamp with time zone）；
            // 同时 PG 没有 ignore_time 列，原先 .lt(IgnoreAlarm::getIgnoreTime, ...) 必报错。
            // 改用 apply 注入字符串比较，now 也序列化为同格式字符串，PG 可按字典序比较。
            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
                .apply("end_time < {0}", nowStr);
            int count = (int) ignoreAlarmService.count(qw);
            ignoreAlarmService.removeExpire();
            log.info("ignore expire alarm removed. count={}", count);
        } catch (Exception e) {
            // W-X17a：绝不吞错。ERROR 日志带完整堆栈，并 rethrow 让监控/告警能捕获。
            log.error("ignore expire alarm remove failed, exception: {}", e.getMessage(), e);
            throw e;
        }
    }
}