package com.hikrobotics.solution.module.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import java.util.List;
import java.util.Set;

/**
 * DataupLoad 设备状态记录服务接口（沿用 PSM IStatusRecordService 语义）。
 *
 * <p>W-B03 扩展：新增 {@link #receiveStatus(List)} 用于
 * DetectDataController#receiveStatus，1:1 抄自反编译 PSM 同名方法；</p>
 *
 * <p>W-LIN-01 扩展：补齐 PSM {@code searchClientStatus(String, String)}，
 * LineServiceImpl.delete 依赖此接口查询指定 (lineNo, faceNo) 的客户端状态（type=CLIENT）；
 * 若 ONLINE 拒绝删除。</p>
 *
 * <p>DataupLoad 当前 alarm 链路只用到 {@link #searchOffLineClient}（被
 * AlarmRecordServiceImpl.handleAlarmSearch 调用），其余状态相关接口后续按需补齐。</p>
 */
public interface IStatusRecordService extends IService<StatusRecord> {

   /**
    * W-B03 新增：客户端批量上报设备状态，1:1 抄自反编译 PSM receiveStatus。
    */
   BaseResult receiveStatus(List<StatusRecord> records);

   /**
    * W-LIN-01 新增：按 (lineNo, faceNo) 查询 type=CLIENT 的状态记录。
    *
    * <p>1:1 抄自 PSM {@code searchClientStatus(String, String)}，
    * 用于 LineServiceImpl.delete 的"客户端在线校验"分支。</p>
    *
    * @param lineNo 产线号
    * @param faceNo 工位号
    * @return type=CLIENT 的状态记录；不存在则返回 {@code null}
    */
   StatusRecord searchClientStatus(String lineNo, String faceNo);

   /**
    * 离线客户端查询。DataupLoad 当前返回空集即可（handleAlarmSearch 的 type!=4 分支不会
    * 触发，因为 alarm 入口走 type=4 走 alarm_record 分支）。
    */
   Object searchOffLineClient(String lineNo, String faceNo, Integer type);

   /**
    * PSM 1:1 — 批量查询客户端状态（StateChangeServiceImpl.getStateStatistics 依赖）。
    */
   java.util.List<StatusRecord> listClientStatus(java.util.Set<Integer> lineIds);
}
