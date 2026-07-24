package com.hikrobotics.solution.module.detect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.detect.model.DefectRecordBackupPO;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 工单 W-D：defect_record_backup 表 Mapper。
 *
 * <p>PSM 用自家 {@code HBaseMapper}（带 {@code insertBatchSomeColumn} 批量插入能力）；
 * 本项目使用通用 MyBatis-Plus {@link BaseMapper}，批量插入通过自定义 {@code batchInsert}
 * 方法实现，匹配反编译代码里的 {@code insertBatchSomeColumn(backupRecords)} 调用语义。</p>
 */
public interface DefectRecordBackupDAO extends BaseMapper<DefectRecordBackupPO> {

    /**
     * 批量插入 backup 记录。返回值等于实际写入行数，便于上层校验是否全部成功。
     *
     * <p>实现要点：</p>
     * <ul>
     *   <li>用 foreach 把列表拼成多 VALUES，绕开 MyBatis-Plus 自带 {@code insertBatchSomeColumn}
     *       只对 {@code IService} 内部可见的限制；</li>
     *   <li>显式列出字段，避开主键 id（DB 自增）；时间字段由 DB 默认或调用方填充；</li>
     *   <li>一次网络往返，PG/HSQL/Mysql 通用，代价小于单条循环 insert。</li>
     * </ul>
     */
    @Insert({
        "<script>",
        "INSERT INTO defect_record_backup(line_no, face_no, glove_no, result, defect_type, img_list, time, update_time, create_time, except_flag) VALUES ",
        "<foreach collection='records' item='r' separator=','>",
        "(#{r.lineNo}, #{r.faceNo}, #{r.gloveNo}, #{r.result}, #{r.defectType}, #{r.imgList}, #{r.time}, #{r.updateTime}, #{r.createTime}, #{r.exceptFlag})",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("records") List<DefectRecordBackupPO> records);
}
