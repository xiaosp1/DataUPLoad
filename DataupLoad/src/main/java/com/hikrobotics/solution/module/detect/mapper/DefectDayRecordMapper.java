package com.hikrobotics.solution.module.detect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * DataupLoad defect_day_record 表 MyBatis-Plus Mapper（W-LIN-01 / W-DET-04）。
 *
 * <p>在 BaseMapper 基础上补齐 PSM DefectDayRecordDAO 的方法：
 * <ul>
 *   <li>{@link #updateCount} — 按 (time, type, lineNo, faceNo) 累加 count（W-DET-04，PSM {@code updateCount(records)}）</li>
 *   <li>{@link #selectDefectCountDay} — 按时间范围 + line/face + defectName 列表查询每日缺陷计数（W-LIN-01，PSM {@code selectDefectCountDay}）</li>
 * </ul>
 *
 * <p>SQL 1:1 抄自 PSM 反编译 DefectDayRecordXml.xml（见 audit 2026-07-24 Top 1）。
 * 原 PSM XML 中 {@code <foreach collection="records" ...>} 走 MyBatis 注解
 * {@code <script>} + 分号 {@code separator=";"} 内联（MySQL 多语句更新）；{@code <if test="defects.size()!=0">}
 * 分支同样通过 {@code <script>} 内联。</p>
 */
@Mapper
public interface DefectDayRecordMapper extends BaseMapper<DefectDayRecord> {

    /**
     * 工单 W-DET-04：PSM {@code updateCount}，按 (time, type, lineNo, faceNo) 累加 defect_day_record.count。
     *
     * <p>SQL 1:1 抄自 PSM XML（DefectDayRecordXml.xml#updateCount）：
     * <pre>{@code
     * <update id="updateCount" parameterType="java.util.List">
     *     <foreach collection="records" item="record" index="index" separator=";">
     *         update defect_day_record set count = count + #{record.count}
     *         where time = #{record.time}
     *         and type = #{record.type}
     *         and line_no = #{record.lineNo}
     *         and face_no = #{record.faceNo}
     *     </foreach>
     * </update>
     * }</pre>
     * </p>
     *
     * <p>PSM DAO 签名：{@code boolean updateCount(@Param("records") List<DefectDayRecordPO> records)}，
     * 返回 boolean 表示是否成功（MyBatis 在没有抛异常时即视为成功）。注解实现下返回类型用 {@code int} 表示
     * 受影响行数（Spring 习惯），与 DAO 的 boolean 等价（成功条件：受影响行数 &ge; 0）。</p>
     *
     * <p><b>注意</b>：separator 用 {@code ";"} 是 PSM 写法，依赖 MySQL JDBC 连接参数
     * {@code allowMultiQueries=true}。当前 DataupLoad 默认 datasource 配置未显式开启该参数，
     * 实际部署需在 application.yml 的 PG/MySQL URL 中追加 {@code ?allowMultiQueries=true}（或
     * 在 multi-statement 模式下默认允许）。如未开启，{@code updateCount} 单条可执行，多条会报
     * "multi-statement not allowed" 错误。</p>
     *
     * @param records 待累加的缺陷日记录（每条包含 time/type/lineNo/faceNo/count）
     * @return 受影响行数（PSM DAO 是 boolean，注解下退化为 int；0/正数均视作"成功"）
     */
    @Update("<script>" +
            "<foreach collection='records' item='record' index='index' separator=';'>" +
            "  UPDATE defect_day_record SET count = count + #{record.count} " +
            "  WHERE time = #{record.time} " +
            "    AND type = #{record.type} " +
            "    AND line_no = #{record.lineNo} " +
            "    AND face_no = #{record.faceNo}" +
            "</foreach>" +
            "</script>")
    int updateCount(@Param("records") List<DefectDayRecord> records);

    /**
     * 按 line/face + 时间范围 + 缺陷名集合查询每日缺陷计数（PSM selectDefectCountDay）。
     *
     * <p>SQL 1:1 抄自 PSM XML：
     * <pre>{@code
     * select count, type, time from defect_day_record
     * where time >= #{startTime}
     *   and time <= #{endTime}
     *   and line_no = #{lineNo}
     *   and face_no = #{faceNo}
     *   <if test="defects.size()!=0">
     *       and type in (
     *           <foreach collection="defects" item="defect" open="(" close=")" separator=",">
     *               #{defect}
     *           </foreach>
     *       )
     *   </if>
     * order by time
     * }</pre>
     * </p>
     *
     * @param startTime 时间下界（PSM 用法：{@code HikDateUtil.formatLocalDate(date, simplePattern)} 返回的 {@code yyyy-MM-dd} 串）
     * @param endTime   时间上界（同上）
     * @param lineNo    产线号
     * @param faceNo    工位号
     * @param defects   缺陷名集合；为空时不过滤 type
     */
    @Select("<script>" +
            "SELECT count, type, time FROM defect_day_record " +
            "WHERE time &gt;= #{startTime} " +
            "  AND time &lt;= #{endTime} " +
            "  AND line_no = #{lineNo} " +
            "  AND face_no = #{faceNo} " +
            "<if test='defects != null and defects.size() != 0'>" +
            "  AND type IN " +
            "  <foreach collection='defects' item='defect' open='(' close=')' separator=','>" +
            "    #{defect}" +
            "  </foreach>" +
            "</if>" +
            "ORDER BY time" +
            "</script>")
    List<DefectCountDTO> selectDefectCountDay(@Param("startTime") String startTime,
                                              @Param("endTime") String endTime,
                                              @Param("lineNo") String lineNo,
                                              @Param("faceNo") String faceNo,
                                              @Param("defects") List<String> defects);
}
