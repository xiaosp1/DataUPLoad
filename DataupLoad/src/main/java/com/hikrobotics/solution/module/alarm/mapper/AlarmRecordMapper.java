package com.hikrobotics.solution.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.alarm.dto.AlarmCountDTO;
import com.hikrobotics.solution.module.alarm.dto.AlarmCountOfLineDTO;
import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * DataupLoad alarm_record 表 MyBatis-Plus Mapper。
 * <p>
 * 沿用 PSM AlarmRecordDAO 接口语义；本类在 PSM BaseMapper 基础上补齐 5 个聚合查询方法：
 * <ul>
 *   <li>{@link #selectAlarmCountDay} — 按日聚合</li>
 *   <li>{@link #countAlarmCount} — 按 level 分组统计</li>
 *   <li>{@link #selectAlarmCountByType} — 未处理报警按 type 分组统计</li>
 *   <li>{@link #selectRecord} — 行号+面+缺陷名去重后取最新一条</li>
 *   <li>{@link #selectAlarmCount} — 按 line/face/defect 分组计数</li>
 * </ul>
 * 方法签名严格对齐 PSM {@code AlarmRecordDAO}（参见 audit 2026-07-24 Top 1）。
 * 原 PSM XML 中的 foreach / row_number 语义在本类中通过 MyBatis 注解
 * {@code <script>} 形式内联，避免引入额外 XML 资源。
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {

    /**
     * 按天聚合报警数（PSM selectAlarmCountDay）。
     * <p>
     * SQL: {@code SELECT TO_CHAR(time::date,'yyyy-MM-dd') AS count_time ,COUNT(1) AS count
     *        FROM alarm_record
     *        WHERE time >= #{startTime} AND time <= #{endTime}
     *          AND line_no = #{lineNo} AND face_no = #{faceNo}
     *        GROUP BY count_time ORDER BY count_time}
     */
    @Select("SELECT TO_CHAR(time::date,'yyyy-MM-dd') AS count_time, " +
            "COUNT(1) AS count " +
            "FROM alarm_record " +
            "WHERE time >= #{startTime} AND time <= #{endTime} " +
            "  AND line_no = #{lineNo} AND face_no = #{faceNo} " +
            "GROUP BY count_time ORDER BY count_time")
    List<AlarmCountDTO> selectAlarmCountDay(@Param("startTime") String startTime,
                                            @Param("endTime") String endTime,
                                            @Param("lineNo") String lineNo,
                                            @Param("faceNo") String faceNo);

    /**
     * 按 level 分组聚合报警数（PSM countAlarmCount，无参）。
     * <p>
     * SQL: {@code SELECT level, COUNT(*) FROM alarm_record GROUP BY level}
     * <p>
     * 注：PSM XML 中使用 {@code SELECT level, count(*) ...} 未指定列别名，
     * MyBatis 通过下划线 → 驼峰无法自动映射到 {@code AlarmCountDTO.level}，
     * 故此处显式起别名 {@code level AS level, count AS count}，保证 DTO 字段填充。
     */
    @Select("SELECT level AS level, COUNT(*) AS count " +
            "FROM alarm_record GROUP BY level")
    List<AlarmCountDTO> countAlarmCount();

    /**
     * 按 type 分组聚合未处理报警数（PSM selectAlarmCountByType，无参）。
     * <p>
     * SQL: {@code SELECT type, COUNT(*) FROM alarm_record WHERE solve=2 GROUP BY type}
     * <p>
     * 注：{@code solve=2} 对应 {@link com.hikrobotics.solution.module.alarm.constant.AlarmSolvedEnum#UNSOLVED}。
     */
    @Select("SELECT type AS type, COUNT(*) AS count " +
            "FROM alarm_record WHERE solve=2 GROUP BY type")
    List<AlarmCountDTO> selectAlarmCountByType();

    /**
     * 按 line_no + face_no + defect_name 去重，每个组合取 id 最大的最新一条
     * （PSM selectRecord，参数为缺陷名列表）。
     * <p>
     * SQL: {@code SELECT tmp.* FROM (
     *          SELECT row_number() OVER (PARTITION BY ar.line_no, ar.face_no, ar.defect_name
     *                                    ORDER BY id DESC) AS group_id, *
     *          FROM alarm_record ar
     *          WHERE ar.defect_name IN (...)
     *            AND ar.solve = 2
     *        ) tmp WHERE tmp.group_id = 1}
     */
    @Select("<script>" +
            "SELECT tmp.* FROM ( " +
            "  SELECT row_number() OVER (PARTITION BY ar.line_no, ar.face_no, ar.defect_name " +
            "                            ORDER BY id DESC) AS group_id, * " +
            "  FROM alarm_record ar " +
            "  WHERE ar.defect_name IN " +
            "    <foreach collection='names' item='name' separator=',' open='(' close=')'>" +
            "      #{name}" +
            "    </foreach> " +
            "    AND ar.solve = 2 " +
            ") tmp WHERE tmp.group_id = 1" +
            "</script>")
    List<AlarmRecord> selectRecord(@Param("names") List<String> names);

    /**
     * 按 line_no + face_no + defect_name 分组计数未处理报警数
     * （PSM selectAlarmCount，参数为缺陷名列表）。
     * <p>
     * SQL: {@code SELECT ar.line_no, ar.face_no, ar.defect_name, COUNT(*) AS count
     *        FROM alarm_record ar
     *        WHERE ar.defect_name IN (...)
     *          AND ar.solve = 2
     *        GROUP BY ar.line_no, ar.face_no, ar.defect_name}
     */
    @Select("<script>" +
            "SELECT ar.line_no AS line_no, ar.face_no AS face_no, ar.defect_name AS defect_name, " +
            "       COUNT(*) AS count " +
            "FROM alarm_record ar " +
            "WHERE ar.defect_name IN " +
            "  <foreach collection='names' item='name' separator=',' open='(' close=')'>" +
            "    #{name}" +
            "  </foreach> " +
            "  AND ar.solve = 2 " +
            "GROUP BY ar.line_no, ar.face_no, ar.defect_name" +
            "</script>")
    List<AlarmCountOfLineDTO> selectAlarmCount(@Param("names") List<String> names);
}
