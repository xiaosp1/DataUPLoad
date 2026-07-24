package com.hikrobotics.solution.module.line.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import com.hikrobotics.solution.module.line.mapper.LineDayRecordMapper;
import com.hikrobotics.solution.module.line.service.ILineDayRecordService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 工单 W-D 临时桩：实现 DetectDataTaskManager 调度所需的
 * {@link ILineDayRecordService#removeRecordByTime(LocalDateTime)}。
 *
 * <p>反编译 PSM 中 LineDayRecordPO.time 是 String（HikDateUtil.formatLocalDate 的
 * "yyyy-MM-dd"），而本项目 entity.LineDayRecord.time 同样是 String；本实现把
 * LocalDateTime 截到天再按字符串字典序比较，符合"删除当天及之前"的语义。</p>
 *
 * <p>工单 SCRN-1 扩展：补齐 {@link ILineDayRecordService#listByTime(String)}，
 * 1:1 抄自反编译 PSM LineDayRecordServiceImpl.listByTime。</p>
 */
@Service
public class LineDayRecordServiceImpl
       extends ServiceImpl<LineDayRecordMapper, LineDayRecord>
       implements ILineDayRecordService {

    @Override
    public void removeRecordByTime(LocalDateTime time) {
        // 反编译原版没用到返回值，这里同样走 lambda delete；按字符串比较保持字典序语义
        this.remove(Wrappers.<LineDayRecord>lambdaQuery()
            .le(LineDayRecord::getTime, time.toLocalDate().toString()));
    }

    @Override
    public List<LineDayRecord> listByTime(String time) {
        return this.list(Wrappers.<LineDayRecord>lambdaQuery()
            .eq(LineDayRecord::getTime, time));
    }

    @Override
    public List<LineDayRecord> searchLineDayRecord(
            com.hikrobotics.solution.module.yingke.dto.SearchDefectRecordDTO form) {
        return this.list(Wrappers.<LineDayRecord>lambdaQuery()
            .ge(LineDayRecord::getTime, form.getStartTime().toLocalDate().toString())
            .le(LineDayRecord::getTime, form.getEndTime().toLocalDate().toString()));
    }
}
