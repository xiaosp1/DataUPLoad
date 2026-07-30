package com.hikrobotics.solution.module.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.util.EventUtil;
import com.hikrobotics.solution.module.alarm.constant.AlarmReasonEnum;
import com.hikrobotics.solution.module.alarm.event.DealAlarmEvent;
import com.hikrobotics.solution.module.detect.dto.DeviceStateDTO;
import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import com.hikrobotics.solution.module.detect.enums.DeviceStatus;
import com.hikrobotics.solution.module.detect.enums.DeviceType;
import com.hikrobotics.solution.module.detect.mapper.StatusRecordMapper;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备状态记录服务实现（W-B03 + W-X30b DealAlarmEvent + W-LIN-01）。
 *
 * <p>W-B03：实现 {@link #receiveStatus}，1:1 抄自反编译 PSM 同名方法；</p>
 * <p>W-X30b：客户端上线检测（旧状态 OUTLINE → 新状态 ONLINE）时发布 {@link DealAlarmEvent}，
 * 触发 {@code AlarmRecordServiceImpl.dealClientAlarmListener} 清理旧的 UNSOLVED 掉线告警，
 * 与 PSM 行为一致。</p>
 * <p>W-LIN-01：实现 {@link #searchClientStatus}（PSM 1:1），
 * 供 {@code LineServiceImpl.delete} 调用。</p>
 * <p>W-FIX-01：实现 {@link #searchOffLineClient}（PSM 1:1），
 * 返回 type 匹配 + status=OUTLINE 的 DeviceStateDTO 列表，供
 * {@code AlarmRecordServiceImpl.handleAlarmSearch} 的 type!=4 分支调用。</p>
 */
@Service
public class StatusRecordServiceImpl
       extends ServiceImpl<StatusRecordMapper, StatusRecord>
       implements IStatusRecordService {

   private static final Logger log = LoggerFactory.getLogger(StatusRecordServiceImpl.class);

   @Transactional(rollbackFor = Exception.class)
   @Override
   public BaseResult receiveStatus(List<StatusRecord> records) {
      if (CollectionUtils.isEmpty(records)) {
         return BaseResult.build().error("20402").log("status record error, data is empty");
      }
      String lineNo = records.get(0).getLineNo();
      String faceNo = records.get(0).getFaceNo();
      long count = records.stream().filter(r -> r.getLineNo().equals(lineNo)).filter(r -> r.getFaceNo().equals(faceNo)).count();
      if (count != records.size()) {
         return BaseResult.build().error("20401").log("status record error, not same line", records.toString());
      }

      LocalDateTime now = LocalDateTime.now();
      for (StatusRecord r : records) {
         LambdaQueryWrapper<StatusRecord> qw = (LambdaQueryWrapper<StatusRecord>)((LambdaQueryWrapper<StatusRecord>)((LambdaQueryWrapper<StatusRecord>)Wrappers
                  .lambdaQuery(StatusRecord.class).eq(StatusRecord::getLineNo, r.getLineNo()))
               .eq(StatusRecord::getFaceNo, r.getFaceNo()))
            .eq(StatusRecord::getDeviceNo, r.getDeviceNo());
         StatusRecord old = this.baseMapper.selectOne(qw);

         // W-X30b：检测客户端从掉线恢复 → 发布 DealAlarmEvent 清理旧告警
         if (old != null
            && old.getStatus() != null
            && old.getStatus().equals(DeviceStatus.OUTLINE.getValue())) {
            log.info("W-X30b: client recovered from outline, fire DealAlarmEvent.[line={}][face={}][device={}]",
               lineNo, faceNo, r.getDeviceNo());
            EventUtil.publish(new DealAlarmEvent(this)
               .setLineNo(lineNo)
               .setFaceNo(faceNo)
               .setReason(AlarmReasonEnum.DISCONNECT.getValue()));
         }

         if (old == null) {
            // W-LIVE-DATA-FIX Bug C：status_record.time NOT NULL，但 mapper INSERT 字段不含 time。
            // 在 entity 上显式塞 time，service 层补齐 NOT NULL 列，避免 PG 抛
            // "null value in column 'time' violates not-null constraint" → 500/10500。
            r.setTime(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            r.setCreateTime(now);
            r.setUpdateTime(now);
            this.baseMapper.insert(r);
         } else {
            // W-LIVE-DATA-FIX Bug C：update 路径也补 time（与 create_time 同源，保持 status_record.time 列语义"上报时间"）
            r.setTime(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            r.setId(old.getId());
            r.setUpdateTime(now);
            this.baseMapper.updateById(r);
         }
      }
      return BaseResult.build();
   }

   @Override
   public StatusRecord searchClientStatus(String lineNo, String faceNo) {
      // W-LIN-01：1:1 抄自 PSM StatusRecordServiceImpl.searchClientStatus
      // SELECT * FROM status_record
      // WHERE line_no = #{lineNo} AND face_no = #{faceNo} AND type = #{DeviceType.CLIENT}
      LambdaQueryWrapper<StatusRecord> qw = Wrappers.<StatusRecord>lambdaQuery()
          .eq(StatusRecord::getLineNo, lineNo)
          .eq(StatusRecord::getFaceNo, faceNo)
          .eq(StatusRecord::getType, DeviceType.CLIENT.getValue());
      return this.baseMapper.selectOne(qw);
   }

   @Override
   public List<DeviceStateDTO> searchOffLineClient(String lineNo, String faceNo, Integer type) {
      // W-FIX-01：1:1 抄自反编译 PSM StatusRecordServiceImpl.searchOffLineClient
      // SELECT * FROM status_record
      // WHERE line_no = #{lineNo}
      //   AND face_no = #{faceNo}
      //   AND type    = #{type}
      //   AND status  = DeviceStatus.OUTLINE (2)
      // → 转换为 DeviceStateDTO 列表
      LambdaQueryWrapper<StatusRecord> qw = Wrappers.<StatusRecord>lambdaQuery()
          .eq(StatusRecord::getLineNo, lineNo)
          .eq(StatusRecord::getFaceNo, faceNo)
          .eq(StatusRecord::getType, type)
          .eq(StatusRecord::getStatus, DeviceStatus.OUTLINE.getValue());
      return this.list(qw).stream().map(DeviceStateDTO::new).toList();
   }

   @Override
   public List<StatusRecord> listClientStatus(Set<Integer> lineIds) {
      LambdaQueryWrapper<StatusRecord> qw = Wrappers.<StatusRecord>lambdaQuery()
          .in(StatusRecord::getLineId, lineIds);
      return this.list(qw);
   }
}
