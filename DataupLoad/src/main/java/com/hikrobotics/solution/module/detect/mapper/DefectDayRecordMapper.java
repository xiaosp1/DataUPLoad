package com.hikrobotics.solution.module.detect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * DataupLoad defect_day_record 表 MyBatis-Plus Mapper（W-LIN-01）。
 *
 * <p>在 W-B03 BaseMapper 基础上补齐 PSM DefectDayRecordDAO 的聚合查询方法：
 * <ul>
 *   <li>{@link #selectDefectCountDay} — 按时间范围 + line/face + defectName 列表查询每日缺陷计数</li>
 * </ul>
 *
 * <p>SQL 1:1 抄自 PSM 反编译 DefectDayRecordDAO.xml（见 audit 2026-07-24 Top 1）。
 * 原 PSM XML 中 {@code <if test="defects.size()!=0">} 分支通过 MyBatis 注解
 * {@code <script>} 内联。</p>
 */
@Mapper
public interface DefectDayRecordMapper extends BaseMapper<DefectDayRecord> {

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
