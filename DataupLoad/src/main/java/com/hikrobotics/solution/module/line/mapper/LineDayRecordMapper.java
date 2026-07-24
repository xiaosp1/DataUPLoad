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
 * 生产线单日检测数据汇总表 Mapper（W-LIN-01 + W-LIN-04）。
 *
 * <p>对应 PG 表 {@code line_day_record}。
 * <ul>
 *   <li>W-LIN-01：补齐 PSM {@code LineDayRecordDAO} 的 2 个聚合查询方法
 *       {@link #selectLineCountDay} / {@link #selectRightAndError}</li>
 *   <li>W-LIN-04：补齐多条件通用查询 {@link #listByAttribute(LineDayRecord)}，按 entity 非空字段动态拼接 WHERE</li>
 * </ul>
 *
 * <p>SQL 1:1 抄自 PSM 反编译 XML {@code LineDayRecordXml.xml}（{@code docs/domain/海康大屏逆向/psm-decompiled/BOOT-INF/classes/com/hikrobotics/solution/module/mapper/LineDayRecordXml.xml}）。</p>
 */
@Mapper
public interface LineDayRecordMapper extends BaseMapper<LineDayRecord> {

    /**
     * 按 line/face + 时间范围聚合每日产量/不良（PSM {@code selectLineCountDay}）。
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
     * 取指定产线工位当日（00:00:00 至 23:59:59）正/次品数（PSM {@code selectRightAndError}）。
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

    /**
     * 工单 W-LIN-04：按 {@link LineDayRecord} 任意非空字段做多条件组合查询。
     *
     * <p>支持按以下字段过滤（{@code null} 或空串时跳过对应条件）：
     * <ul>
     *   <li>{@code id} — 主键精确查询（{@code = #{query.id}}）</li>
     *   <li>{@code lineNo} — 产线号精确查询（{@code = #{query.lineNo}}）</li>
     *   <li>{@code faceNo} — 工位号精确查询（{@code = #{query.faceNo}}）</li>
     *   <li>{@code time} — {@code yyyy-MM-dd HH:mm:ss} 时间戳精确查询（{@code = #{query.time}}）</li>
     *   <li>{@code startTime}（String，非 entity 字段）— 时间下界（{@code >= #{query.startTime}}）</li>
     *   <li>{@code endTime}（String，非 entity 字段）— 时间上界（{@code <= #{query.endTime}}）</li>
     * </ul>
     * </p>
     *
     * <p>排序：按 {@code time ASC}（与 {@link #selectLineCountDay} 一致，便于大屏按时间序列消费）。</p>
     *
     * <p>动态 SQL 走 MyBatis {@code <script>} + {@code <if>}（与
     * {@code DefectDayRecordMapper.selectDefectCountDay} 同一风格）。</p>
     *
     * @param query 查询条件模板（{@code null} 字段忽略）
     * @return 命中的 {@link LineDayRecord} 行列表
     */
    @Select("<script>" +
            "SELECT id, right_count, error_count, line_no, face_no, " +
            "       remove_total, upload_remove_total, time, update_time, create_time " +
            "FROM line_day_record " +
            "<where>" +
            "  <if test='query.id != null'>AND id = #{query.id}</if>" +
            "  <if test='query.lineNo != null and query.lineNo != \"\"'>AND line_no = #{query.lineNo}</if>" +
            "  <if test='query.faceNo != null and query.faceNo != \"\"'>AND face_no = #{query.faceNo}</if>" +
            "  <if test='query.time != null and query.time != \"\"'>AND time = #{query.time}</if>" +
            "  <if test='query.startTime != null and query.startTime != \"\"'>AND time &gt;= #{query.startTime}</if>" +
            "  <if test='query.endTime != null and query.endTime != \"\"'>AND time &lt;= #{query.endTime}</if>" +
            "</where>" +
            "ORDER BY time ASC" +
            "</script>")
    List<LineDayRecord> listByAttribute(@Param("query") LineDayRecord query);
}
