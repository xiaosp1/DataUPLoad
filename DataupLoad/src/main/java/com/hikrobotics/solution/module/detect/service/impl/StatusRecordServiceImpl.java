package com.hikrobotics.solution.module.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.util.EventUtil;
import com.hikrobotics.solution.module.alarm.constant.AlarmReasonEnum;
import com.hikrobotics.solution.module.alarm.event.DealAlarmEvent;
import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import com.hikrobotics.solution.module.detect.enums.DeviceStatus;
import com.hikrobotics.solution.module.detect.mapper.StatusRecordMapper;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备状态记录服务实现（W-B03 + W-X30b DealAlarmEvent 接入）。
 *
 * <p>W-B03：实现 {@link #receiveStatus}，1:1 抄自反编译 PSM 同名方法；</p>
 * <p>W-X30b：客户端上线检测（旧状态 OUTLINE → 新状态 ONLINE）时发布 {@link DealAlarmEvent}，
 * 触发 {@code AlarmRecordServiceImpl.dealClientAlarmListener} 清理旧的 UNSOLVED 掉线告警，
 * 与 PSM 行为一致。</p>
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
            r.setCreateTime(now);
            r.setUpdateTime(now);
            this.baseMapper.insert(r);
         } else {
            r.setId(old.getId());
            r.setUpdateTime(now);
            this.baseMapper.updateById(r);
         }
      }
      return BaseResult.build();
   }

   @Override
   public Object searchOffLineClient(String lineNo, String faceNo, Integer type) {
      log.debug("searchOffLineClient W-B03 stub: lineNo={}, faceNo={}, type={}", lineNo, faceNo, type);
      return java.util.Collections.emptyList();
   }

   @Override
   public List<StatusRecord> listClientStatus(Set<Integer> lineIds) {
      LambdaQueryWrapper<StatusRecord> qw = Wrappers.<StatusRecord>lambdaQuery()
          .in(StatusRecord::getLineId, lineIds);
      return this.list(qw);
   }
}
