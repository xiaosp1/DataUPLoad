package com.hikrobotics.solution.module.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.dto.AlarmDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmInfoQueryDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmQueryDTO;
import com.hikrobotics.solution.module.alarm.dto.IgnoreAlarmDTO;
import com.hikrobotics.solution.module.alarm.dto.SearchAlarmDTO;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import java.util.List;

/**
 * DataupLoad alarm 记录服务接口（沿用 PSM IAlarmRecordService 语义；PO→实体）。
 * <p>
 * DataupLoad 当前只实现 add / sendAlarmMessage / sendAlarmTextMessage / listNotResolveDefectAlarmRecord；
 * 其余 web 后台查询接口（listAll / deal / handleAlarmNumGet / handleAlarmSearch / handleAlarmIgnore /
 * getAlarmListInfo）保留签名，由 AlarmRecordServiceImpl 返回 BaseResult.build().ok() 占位。
 */
public interface IAlarmRecordService extends IService<AlarmRecord> {
   BaseResult listAll(AlarmQueryDTO var1);

   BaseResult add(AlarmDTO var1);

   BaseResult deal(String var1);

   BaseResult getAlarmListInfo(AlarmInfoQueryDTO var1);

   BaseResult handleAlarmNumGet();

   BaseResult handleAlarmSearch(SearchAlarmDTO var1);

   void sendAlarmTextMessage();

   List<AlarmRecord> listNotResolveDefectAlarmRecord();

   BaseResult handleAlarmIgnore(IgnoreAlarmDTO var1);

   void sendAlarmMessage(AlarmRecord var1);
}
