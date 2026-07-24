package com.hikrobotics.solution.module.detect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.module.detect.entity.WorkshopDayRecord;
import com.hikrobotics.solution.module.detect.mapper.WorkshopDayRecordMapper;
import com.hikrobotics.solution.module.detect.service.IWorkshopDayRecordService;
import org.springframework.stereotype.Service;

/**
 * PSM 1:1 WorkshopDayRecordServiceImpl — 车间日统计。
 */
@Service
public class WorkshopDayRecordServiceImpl
    extends ServiceImpl<WorkshopDayRecordMapper, WorkshopDayRecord>
    implements IWorkshopDayRecordService {
}
