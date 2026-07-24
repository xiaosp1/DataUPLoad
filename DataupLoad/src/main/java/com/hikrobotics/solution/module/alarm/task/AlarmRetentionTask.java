package com.hikrobotics.solution.module.alarm.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hikrobotics.solution.module.alarm.constant.AlarmSolvedEnum;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import com.hikrobotics.solution.module.alarm.mapper.AlarmRecordMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DataupLoad 报警记录保留期清理定时任务（沿用 PSM {@code AlarmTaskManager.clearAlarmData} 语义）。
 *
 * <p>职责：每天凌晨 3 点扫描 {@code alarm_record} 表，物理删除 {@code create_time} 早于
 * {@code now-90 天} <b>且</b> {@code solve = SOLVED(1)} 的记录，防止 DB 单表无限增长。</p>
 *
 * <p>与 PSM 的差异：
 * <ul>
 *   <li>DataupLoad 沿用 {@code AlarmRecordMapper}（MP {@code BaseMapper<AlarmRecord>}）替代
 *       PSM 原版 {@code AlarmRecordDAO}，保留 {@code delete(Wrapper)} 调用语义；</li>
 *   <li>DataupLoad 实体名统一为 {@code AlarmRecord}（PSM 原版为 {@code AlarmRecordPO}），表名仍为
 *       {@code alarm_record}；</li>
 *   <li>cron 表达式按 W-F01-C 规范固定为 {@code "0 0 3 * * ?"}（PSM 原版为
 *       {@code "0 0 0 * * ?"} 凌晨 0 点）；</li>
 *   <li>保留天数硬编码 90 天（PSM 原版通过 {@code @Value("${data-retention-time.alarm:3}")} 注入，
 *       W-F01-C 范围内不修改 application-prod.yml，故此处写死 90 天，与工单口径一致）。</li>
 * </ul>
 * </p>
 *
 * <p>W-F01-C 工单：仅新建本文件，不修改 {@code application-prod.yml}、{@code AlarmRecordMapper}、
 * {@code AlarmRecord} 实体，亦不重启服务；测试期间可临时把 cron 改为 {@code "0/30 * * * * ?"}
 * 验证后还原。</p>
 */
@Component
public class AlarmRetentionTask {

    private static final Logger log = LoggerFactory.getLogger(AlarmRetentionTask.class);

    private static final long RETENTION_DAYS = 90L;

    @Autowired
    private AlarmRecordMapper alarmRecordMapper;

    /**
     * 每天凌晨 3 点清理 90 天前、且已处理（{@code solve = SOLVED}）的报警记录。
     *
     * <p>删除策略：{@code create_time < now - 90 days AND solve = SOLVED}；删除条数通过
     * MP {@code BaseMapper.delete(Wrapper)} 返回值获取，并按 PSM 字节码规范输出
     * {@code "delete alarm data 90 days ago success，delete count {}"} 日志。</p>
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void clearAlarmData() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
            LambdaQueryWrapper<AlarmRecord> wrapper = Wrappers.<AlarmRecord>lambdaQuery()
                .lt(AlarmRecord::getCreateTime, threshold)
                .eq(AlarmRecord::getSolve, AlarmSolvedEnum.SOLVED.getValue());
            Integer count = alarmRecordMapper.delete(wrapper);
            log.info("delete alarm data 90 days ago success，delete count {}", count);
        } catch (Exception e) {
            log.error("delete alarm data 90 days ago failed, get exception {}", e.getMessage(), e);
        }
    }
}
