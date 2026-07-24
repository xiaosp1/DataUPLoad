package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.line.entity.LineDefectType;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 生产线缺陷类型表 Mapper
 * 对应 PG 表 line_defect_type
 */
@Mapper
public interface LineDefectTypeMapper extends BaseMapper<LineDefectType> {

   /**
    * W-DFT-01b：按 lineNo 查询缺陷类型列表（PSM LineDefectTypeDAO 等价能力）。
    *
    * <p>SQL 1:1 检索 PG 表 {@code line_defect_type}，按 {@code line_no} 列等值过滤。
    * 实体字段对齐说明：工单 W-DFT-01b 最初定义为
    * {@code listByLineId(Integer lineId)}，但 DataupLoad {@link LineDefectType} 实体与 PSM
    * {@code LineDefectTypePO} 字段一致，主键是 {@code id}（Integer），业务线体关联是
    * {@code lineNo}（String）+ {@code faceNo}（String），<b>不存在 {@code lineId} 字段</b>。
    * 本方法按"1:1 对齐 PSM 反编译产物"的原则，参数与列对齐为 {@code lineNo}（String）。</p>
    *
    * <p>Service impl 已有同名的 lambdaQuery 版（{@code LineDefectTypeServiceImpl.listByLineNo}），
    * 本 Mapper 方法为显式 SQL 版本（PG 可走 {@code line_no} B-Tree 索引），二者并存：</p>
    * <ul>
    *   <li>Service impl 默认调用 lambdaQuery 版（动态 SQL）</li>
    *   <li>外部模块如需直查 DB，可调本方法</li>
    * </ul>
    *
    * @param lineNo 线体编号（{@code line_defect_type.line_no} 列）
    * @return 该线体下全部缺陷类型（无记录返回空列表，非 null）
    */
   @Select("SELECT * FROM line_defect_type WHERE line_no = #{lineNo}")
   List<LineDefectType> listByLineNo(String lineNo);
}
