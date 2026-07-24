package com.hikrobotics.solution.common.task;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hikrobotics.solution.framework.util.EventUtil;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.alarm.constant.AlarmReasonEnum;
import com.hikrobotics.solution.module.alarm.constant.AlarmSolvedEnum;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import com.hikrobotics.solution.module.alarm.model.DefectType;
import com.hikrobotics.solution.module.alarm.service.IAlarmRecordService;
import com.hikrobotics.solution.module.alarm.service.IDefectTypeService;
import com.hikrobotics.solution.module.detect.enums.DeviceStatus;
import com.hikrobotics.solution.module.detect.enums.DeviceType;
import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import com.hikrobotics.solution.module.detect.mapper.StatusRecordMapper;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import com.hikrobotics.solution.module.line.event.StateChangeEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
public class GlobalTaskManager {
    private static final Logger log = LoggerFactory.getLogger(GlobalTaskManager.class);
    @Autowired
    private StatusRecordMapper statusRecordMapper;
    @Autowired
    private IStatusRecordService statusRecordService;
    @Autowired
    private IDefectTypeService defectTypeService;
    // IScreenService 不存在（DataupLoad 无大屏推送链路，详见 ADR-0006）
    // @Autowired
    // private IScreenService screenService;
    @Autowired
    private IAlarmRecordService alarmRecordService;
    @Value(value = "${alarm.save-all:true}")
    private Boolean isSaveAllAlarm;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     *
     * Adapted from PSM GlobalTaskManager#checkClientStatus.
     * Mapping: StatusRecordDAO -> StatusRecordMapper, StatusRecordPO -> StatusRecord,
     * AlarmRecordPO -> AlarmRecord, DefectTypePO -> DefectType, IAlarmRecordService
     * still resolves to AlarmRecordServiceImpl (extends ServiceImpl so getOne/save are available).
     */
    @Scheduled(initialDelay = 60000L, fixedDelay = 5000L)
    public void checkClientStatus() {
        StopWatch watch = new StopWatch();
        watch.start();
        List<StatusRecord> statusRecords = this.statusRecordMapper.selectList(
            (Wrapper) Wrappers.<StatusRecord>lambdaQuery()
                .eq(StatusRecord::getType, DeviceType.CLIENT.getValue())
                .eq(StatusRecord::getStatus, DeviceStatus.ONLINE.getValue()));
        List<DefectType> deviceDefectType = this.defectTypeService.listByAttribute("客户端", DefectType::getName);
        ArrayList<StatusRecord> offLineDevices = new ArrayList<>();
        for (StatusRecord statusRecord : statusRecords) {
            String lineNo = statusRecord.getLineNo();
            String faceNo = statusRecord.getFaceNo();
            long timeDiff = HikDateUtil.getTimeDifference(
                HikDateUtil.transformTime(statusRecord.getTime()),
                LocalDateTime.now());
            if (timeDiff <= 60000L) {
                continue;
            }
            List<StatusRecord> devices = this.statusRecordService.list(
                (Wrapper) Wrappers.<StatusRecord>lambdaQuery()
                    .eq(StatusRecord::getFaceNo, faceNo)
                    .eq(StatusRecord::getLineNo, lineNo));
            for (StatusRecord device : devices) {
                device.setStatus(DeviceStatus.OUTLINE.getValue());
                offLineDevices.add(device);
            }
            StateChangeEvent event = new StateChangeEvent(this)
                .setTime(LocalDateTime.now())
                .setStatus(DeviceStatus.OUTLINE)
                .setLineNo(lineNo)
                .setFaceNo(faceNo);
            EventUtil.publish((ApplicationEvent) event);
            // PSM 原版用 synchronized(this) 包裹告警去重 + 入库逻辑。
            // DataupLoad 无并发抢锁场景（Scheduled 单线程串行执行），直接调用即可。
            AlarmRecord alarm = this.alarmRecordService.getOne(
                (Wrapper) Wrappers.<AlarmRecord>lambdaQuery()
                    .eq(AlarmRecord::getLineNo, lineNo)
                    .eq(AlarmRecord::getFaceNo, faceNo)
                    .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue())
                    .eq(AlarmRecord::getReason, AlarmReasonEnum.DISCONNECT.getValue())
                    .last("limit 1"));
            if (alarm == null) {
                boolean isMatch = false;
                alarm = new AlarmRecord().buildClientAlarm(lineNo, faceNo);
                if (CollectionUtils.isNotEmpty((Collection<?>) deviceDefectType)) {
                    isMatch = true;
                    alarm.setDefectType(deviceDefectType.get(0));
                }
                if (isMatch || this.isSaveAllAlarm.booleanValue()) {
                    this.alarmRecordService.save(alarm);
                    this.alarmRecordService.sendAlarmMessage(alarm);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(offLineDevices)) {
            this.statusRecordService.updateBatchById(offLineDevices);
        }
        watch.stop();
        log.warn("end check client status.[cst={}]", watch.getTotalTimeMillis());
    }

    /*
     * PSM 原版通过 IScreenService.sendScreenDataInfo() 推送大屏。
     * DataupLoad 无 screen 模块（ADR-0006），整个方法体留空，仅保留 @Scheduled 占位避免误删。
     * TODO(ADR-0006): 若 DataupLoad 后续接入大屏推送，恢复 IScreenService 注入并调用 sendScreenDataInfo。
     */
    @Scheduled(initialDelay = 10000L, fixedDelay = 5000L)
    public void sendScreen() {
        // TODO(ADR-0006): DataupLoad 无 IScreenService（PSM com.hikrobotics.solution.module.screen.service.IScreenService 未迁移）。
        //                  原 PSM 调用：this.screenService.sendScreenDataInfo();
    }

    /*
     * PSM 原版每分钟调用加密狗 SDK（HikDongleUtil.validateFourthDongle / DongleUtils.validateFifthDongle），
     * 失败时 System.exit(1)。DataupLoad 按 ADR-0005 不再接入加密狗，整个方法体留空。
     * TODO(ADR-0005): 若 DataupLoad 后续接入加密狗鉴权，恢复 HikDongleUtil / DongleUtils 调用及 dongleType/dongleGeneration @Value 注入。
     */
    @Scheduled(initialDelay = 5000L, fixedDelay = 60000L)
    public void checkDogOnlineStatus() {
        // TODO(ADR-0005): DataupLoad 不接入加密狗 SDK，原 PSM 调用已移除。
        //                  原 PSM 调用：
        //                  HikDongleUtil.validateFourthDongle(this.dongleType, null)
        //                  DongleUtils.validateFifthDongle(Integer.parseInt(this.dongleType))
        //                  失败时 System.exit(1)。
    }
}
