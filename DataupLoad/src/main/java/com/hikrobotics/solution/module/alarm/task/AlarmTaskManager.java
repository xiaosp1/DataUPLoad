package com.hikrobotics.solution.module.alarm.task;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hikrobotics.solution.module.alarm.constant.AlarmSolvedEnum;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import com.hikrobotics.solution.module.alarm.mapper.AlarmRecordMapper;
import com.hikrobotics.solution.module.alarm.service.IIgnoreAlarmService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlarmTaskManager {
    private static final Logger log = LoggerFactory.getLogger(AlarmTaskManager.class);
    @Autowired
    private AlarmRecordMapper alarmRecordDAO;
    @Value(value="${data-retention-time.alarm:3}")
    private Integer alarmRetentionTime;
    @Autowired
    private IIgnoreAlarmService ignoreAlarmService;

    @Scheduled(cron="0 0 0 * * ?")
    public void clearAlarmData() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime time = now.minusDays(this.alarmRetentionTime.intValue());
            LambdaQueryWrapper<AlarmRecord> lambdaQuery = (LambdaQueryWrapper<AlarmRecord>)((LambdaQueryWrapper<AlarmRecord>)Wrappers.<AlarmRecord>lambdaQuery().lt(AlarmRecord::getCreateTime, (Object)time)).eq(AlarmRecord::getSolve, (Object)AlarmSolvedEnum.SOLVED.getValue());
            Integer count = this.alarmRecordDAO.delete((Wrapper)lambdaQuery);
            log.info("delete alarm data 90 days ago success\uff0cdelete count {}", (Object)count);
        }
        catch (Exception e) {
            log.error("delete alarm data 90 days ago failed,get excepiton {}\n{}", (Object)e.getMessage(), (Object)e);
        }
    }

    @Scheduled(cron="0 0 1 * * ?")
    public void delExpireIgnoreDefect() {
        this.ignoreAlarmService.removeExpire();
    }
}
