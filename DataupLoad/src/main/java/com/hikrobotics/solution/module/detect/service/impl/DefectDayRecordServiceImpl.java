package com.hikrobotics.solution.module.detect.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.detect.mapper.DefectDayRecordMapper;
import com.hikrobotics.solution.module.detect.service.IDefectDayRecordService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 工单 W-D 临时桩：实现 DetectDataTaskManager 调度所需的
 * {@link IDefectDayRecordService#removeRecordByTime(LocalDateTime)}。
 *
 * <p>反编译 PSM 中 defect_day_record.time 是 String，而本项目 entity.DefectDayRecord.time
 * 也是 String；按反编译原版逻辑，{@code time} 先经 HikDateUtil.formatLocalDate 转为
 * "yyyy-MM-dd"，本实现直接用日期段比对，保留"删除当天及之前"的语义。</p>
 *
 * <p>工单 SCRN-1 扩展：实现 {@link IDefectDayRecordService#listByStartTimeAndDefect(Set, String)}，
 * 1:1 抄自反编译 PSM DefectDayRecordServiceImpl.listByStartTimeAndDefect，用于大屏模块聚合缺陷数。</p>
 */
@Service
public class DefectDayRecordServiceImpl
       extends ServiceImpl<DefectDayRecordMapper, DefectDayRecord>
       implements IDefectDayRecordService {

    private static final Logger log = LoggerFactory.getLogger(DefectDayRecordServiceImpl.class);

    @Override
    public void removeRecordByTime(LocalDateTime time) {
        log.info("begin to delete expire defect day record.[time={}]", time);
        int count = this.baseMapper.delete(
            Wrappers.<DefectDayRecord>lambdaQuery()
                .le(DefectDayRecord::getTime, time.toLocalDate().toString()));
        log.info("end delete expire defect day record.[count={}]", count);
    }

    @Override
    public List<DefectDayRecord> listByStartTimeAndDefect(Set<String> defects, String time) {
        List<DefectDayRecord> result = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(defects)) {
            result.addAll(this.list(
                Wrappers.<DefectDayRecord>lambdaQuery()
                    .ge(DefectDayRecord::getTime, time)
                    .in(DefectDayRecord::getType, defects)));
        }
        return result;
    }
}
