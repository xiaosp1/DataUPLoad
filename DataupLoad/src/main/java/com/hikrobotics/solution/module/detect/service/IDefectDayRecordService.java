package com.hikrobotics.solution.module.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 工单 SCRN-1 补齐：DataupLoad 原本没有 defect_day_record 的服务接口（只有桩
 * {@link com.hikrobotics.solution.module.detect.service.impl.DefectDayRecordServiceImpl}），
 * 这里按反编译 PSM {@code IDefectDayRecordService} 接口形态建立接口，
 * 仅落地 {@link #removeRecordByTime(LocalDateTime)} + {@link #listByStartTimeAndDefect(Set, String)}，
 * 其余方法后续工单按需补齐。
 *
 * <p>实体类型用本项目已有的 {@link DefectDayRecord}（PSM 是 {@code DefectDayRecordPO}）。</p>
 */
public interface IDefectDayRecordService extends IService<DefectDayRecord> {

   /**
    * 工单 W-D / DetectDataTaskManager 调度所需：删除指定时间之前的 defect_day_record 行。
    */
   void removeRecordByTime(LocalDateTime time);

   /**
    * 工单 SCRN-1 大屏模块所需：
    * 按缺陷名集合 + 时间下界，查询 defect_day_record 行（用于大屏聚合缺陷数）。
    *
    * <p>PSM 实现语义：{@code time >= startTime}（PSM 注释里也写 {@code ge(time)}），
    * 若 defects 为空直接返回空集合。</p>
    */
   List<DefectDayRecord> listByStartTimeAndDefect(Set<String> defects, String time);
}
