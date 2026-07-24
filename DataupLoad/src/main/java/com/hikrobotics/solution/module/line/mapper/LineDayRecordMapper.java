package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.line.dto.LineCountDTO;
import com.hikrobotics.solution.module.line.dto.ToDayCountDTO;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 生产线单日检测数据汇总表 Mapper（W-LIN-01）
 *
 * <p>对应 PG 表 line_day_record。
 * 在 W-B03 BaseMapper 基础上补齐 PSM LineDayRecordDAO 的 2 个聚合查询方法（工单 W-LIN-01）：
 * <ul>
 *   <li>{@link #selectLineCountDay} 按 line/face + 时间范围聚合每日产量/不良</li>
 *   <li>{@link #selectRightAndError} 取指定产线工位当日正/次品数</li>
 * </ul>
 *
 * <p>SQL 1:1 抄自 PSM 反编译 LineDayRecordDAO.xml（见 audit 2026-07-24 Top 1）。</p>
 */
@Mapper
public interface LineDayRecordMapper extends BaseMapper<LineDayRecord> {

    /**
     * 按 line/face + 时间范围聚合每日产量/不良（PSM selectLineCountDay）。
     *
     * <p>SQL 1:1 抄自 PSM XML：{@code select right_count+error_count as count, time, error_count
     *        from line_day_record
     *        where time >= #{startTime} and time <= #{endTime}
     *          and line_no = #{lineNo} and face_no = #{faceNo}
     *        order by time}</p>
     *
     * <p>注意：参数 {@code startTime / endTime} 在 PSM 用法里是 {@code HikDateUtil.formatLocalDate(date)}
     * 返回的 {@code yyyy-MM-dd} 串（不带时间），但 SQL 比较的是 line_day_record.time 时间戳。
     * 这里沿用 PSM 字符串拼接行为，不做额外 cast。</p>
     */
    @Select("SELECT (right_count + error_count) AS count, time, error_count " +
            "FROM line_day_record " +
            "WHERE time >= #{startTime} AND time <= #{endTime} " +
            "  AND line_no = #{lineNo} AND face_no = #{faceNo} " +
            "ORDER BY time")
    List<LineCountDTO> selectLineCountDay(@Param("startTime") String startTime,
                                          @Param("endTime") String endTime,
                                          @Param("lineNo") String lineNo,
                                          @Param("faceNo") String faceNo);

    /**
     * 取指定产线工位当日（00:00:00 至 23:59:59）正/次品数（PSM selectRightAndError）。
     *
     * <p>SQL 1:1 抄自 PSM XML：{@code SELECT SUM(right_count) AS right_count,
     *        SUM(error_count) AS error_count FROM line_day_record
     *        WHERE TIME >= TO_CHAR(NOW(),'yyyy-MM-dd 00:00:00')
     *          AND TIME <= TO_CHAR(NOW(),'yyyy-MM-dd 23:59:59')
     *          AND line_no = #{lineNo} AND face_no = #{faceNo}
     *        GROUP BY line_no}</p>
     */
    @Select("SELECT COALESCE(SUM(right_count), 0) AS right_count, " +
            "       COALESCE(SUM(error_count), 0) AS error_count " +
            "FROM line_day_record " +
            "WHERE TIME >= TO_CHAR(NOW(), 'yyyy-MM-dd 00:00:00') " +
            "  AND TIME <= TO_CHAR(NOW(), 'yyyy-MM-dd 23:59:59') " +
            "  AND line_no = #{lineNo} AND face_no = #{faceNo} " +
            "GROUP BY line_no")
    ToDayCountDTO selectRightAndError(@Param("lineNo") String lineNo,
                                      @Param("faceNo") String faceNo);
}
