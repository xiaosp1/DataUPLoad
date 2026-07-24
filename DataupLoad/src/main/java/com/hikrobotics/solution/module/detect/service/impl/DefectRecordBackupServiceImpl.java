package com.hikrobotics.solution.module.detect.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.module.detect.entity.DefectRecord;
import com.hikrobotics.solution.module.detect.mapper.DefectRecordBackupDAO;
import com.hikrobotics.solution.module.detect.model.DefectRecordBackupPO;
import com.hikrobotics.solution.module.detect.service.IDefectRecordBackupService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工单 W-D：DefectRecordBackup 服务实现。
 *
 * <p>1:1 抄自反编译 DefectRecordBackupServiceImpl，调整：</p>
 * <ul>
 *   <li>{@code insertBatchSomeColumn(...)} → 自实现的 {@code batchInsert(...)}；
 *       MyBatis-Plus 自带的 {@code insertBatchSomeColumn} 是 {@code AbstractMethod}，
 *       直接在 Mapper 上不可见，本项目在 {@link DefectRecordBackupDAO} 里补了等价的批量 SQL；</li>
 *   <li>{@code DefectRecordPO} → {@link DefectRecord}（PSM→DataupLoad 类名映射）；</li>
 *   <li>返回值对比由 {@code Long.intValue() == size} 改为更安全的 {@code count == size}
 *       （避免大 size 转回 int 时的语义混淆；同时本项目 batchInsert 直接返回 int）；</li>
 *   <li>{@code Wrapper} 显式 lambda 提取，编译期可读性更友好。</li>
 * </ul>
 */
@Service
public class DefectRecordBackupServiceImpl
       extends ServiceImpl<DefectRecordBackupDAO, DefectRecordBackupPO>
       implements IDefectRecordBackupService {

    @Autowired
    private DefectRecordBackupDAO defectRecordBackupDAO;

    @Override
    public Integer removeRecordByTime(LocalDateTime time) {
        return this.defectRecordBackupDAO.delete(
            Wrappers.<DefectRecordBackupPO>lambdaQuery()
                .le(DefectRecordBackupPO::getTime, time));
    }

    @Override
    public boolean backup(List<DefectRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return true;
        }
        List<DefectRecordBackupPO> backupRecords = records.stream()
            .map(DefectRecordBackupPO::new)
            .toList();
        int inserted = this.defectRecordBackupDAO.batchInsert(backupRecords);
        return inserted == backupRecords.size();
    }
}
