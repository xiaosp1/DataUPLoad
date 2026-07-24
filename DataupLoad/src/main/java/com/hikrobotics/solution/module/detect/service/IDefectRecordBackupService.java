package com.hikrobotics.solution.module.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.detect.entity.DefectRecord;
import com.hikrobotics.solution.module.detect.model.DefectRecordBackupPO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单 W-D：DefectRecordBackup 服务接口。
 *
 * <p>1:1 抄自反编译 IDefectRecordBackupService，唯一调整是入参类型
 * {@code DefectRecordPO} 换成 {@link DefectRecord}（PSM→DataupLoad 类名映射）。</p>
 */
public interface IDefectRecordBackupService extends IService<DefectRecordBackupPO> {
    /**
     * 删除 time 字段小于等于入参的全部 backup 记录，返回受影响行数。
     */
    Integer removeRecordByTime(LocalDateTime time);

    /**
     * 把一组 {@link DefectRecord} 转 PO 后批量入库。返回 true 表示写入行数与输入一致。
     */
    boolean backup(List<DefectRecord> records);
}
