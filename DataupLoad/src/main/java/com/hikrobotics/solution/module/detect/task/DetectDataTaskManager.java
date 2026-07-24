package com.hikrobotics.solution.module.detect.task;

import com.hikrobotics.solution.module.detect.service.IDefectRecordBackupService;
import com.hikrobotics.solution.module.line.service.ILineDayRecordService;
import com.hikrobotics.solution.module.detect.service.impl.DefectDayRecordServiceImpl;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 工单 W-D：每日定时清理 defect_record_backup / defect_day_record / line_day_record。
 *
 * <p>1:1 抄自反编译 DetectDataTaskManager，调整：</p>
 * <ul>
 *   <li>DataupLoad 把 defect_day_record / line_day_record 拆到了 detect.entity.DefectDayRecord
 *       和 line.entity.LineDayRecord，对应服务名为本项目的
 *       {@link DefectDayRecordServiceImpl} 和
 *       {@link ILineDayRecordService}（路径已在工单说明中确认 ✅）；</li>
 *   <li>{@code @Value} 默认值与反编译一致（detect=3 天、statistic=30 天）；</li>
 *   <li>{@code @Scheduled} 表达式与反编译一致（每日 0 点）。</li>
 * </ul>
 */
@Component
public class DetectDataTaskManager {

    private static final Logger log = LoggerFactory.getLogger(DetectDataTaskManager.class);

    @Autowired
    private DefectDayRecordServiceImpl defectDayRecordService;

    @Autowired
    private ILineDayRecordService lineDayRecordService;

    @Autowired
    private IDefectRecordBackupService defectRecordBackupService;

    @Value(value = "${data-retention-time.detect:3}")
    private Integer detectRetentionTime;

    @Value(value = "${data-retention-time.statistic:30}")
    private Integer statisticDataRetentionTime;

    @Scheduled(cron = "0 0 0 * * ?")
    public void clearDetectData() {
        log.info("begin to delete defect record");
        LocalDateTime time = LocalDateTime.now().minusDays(this.detectRetentionTime.intValue());
        int count = this.defectRecordBackupService.removeRecordByTime(time);
        log.info("end delete defect record.[time={}][count={}]", time, count);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void clearStatisticDetectData() {
        LocalDateTime time = LocalDateTime.now().minusDays(this.statisticDataRetentionTime.intValue());
        this.defectDayRecordService.removeRecordByTime(time);
        this.lineDayRecordService.removeRecordByTime(time);
    }
}
