package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hikrobotics.solution.module.line.entity.Line;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 线体表 Mapper（W-LIN-04）。
 *
 * <p>对应 PG 表 {@code line}。本工单补齐 PSM 反编译 {@code LineDAO} 的 2 个自定义查询方法
 * （见 audit 2026-07-24 Top 2）：
 * <ul>
 *   <li>{@link #listAll(IPage)} — 自定义分页查询，按 {@code create_time DESC} 排序，字段集与
 *       {@link Line} entity 1:1（id/name/lineNo/faceNo/color/clientNo/realtimeData/updateTime/createTime）</li>
 *   <li>{@link #selectLine()} — 绑定方案用，简单全查 + LEFT JOIN {@code line_order} 取 {@code orderValue}，
 *       写入 {@link Line#order}（@TableField(exist=false) 字段）</li>
 * </ul>
 *
 * <p>SQL 1:1 抄自 PSM 反编译 XML {@code LineXml.xml}（{@code docs/domain/海康大屏逆向/psm-decompiled/BOOT-INF/classes/com/hikrobotics/solution/module/mapper/LineXml.xml}）。
 * 注：PSM {@code listAll} XML 输出 {@code LineDTO}（含 planId/planName 两个 JOIN 字段）；本工单按任务规范
 * 把返回类型固定为 {@link Line} entity，因此投影列只保留 {@code line.*}，丢弃 plan_to_line / plan JOIN
 * 与 selectLine 的差异通过 entity {@code @TableField(exist=false)} 字段隔离。</p>
 */
@Mapper
public interface LineMapper extends BaseMapper<Line> {

    /**
     * 自定义分页查询（PSM LineDAO.listAll(IPage&lt;LineDTO&gt;) 等价能力）。
     *
     * <p>SQL 1:1 抄自 PSM XML {@code LineXml.xml#listAll} 的主表部分：
     * <pre>{@code
     * SELECT line.* FROM line ORDER BY create_time DESC
     * }</pre>
     * </p>
     *
     * <p>PSM 原版 SELECT 带 LEFT JOIN {@code plan_to_line} + LEFT JOIN {@code plan} 投影 {@code planId} /
     * {@code planName}（落在 {@code LineDTO}）；本工单按任务规范返回类型固定为 {@link Line}，无法承载这两个
     * JOIN 字段，因此剔除 JOIN，仅保留主表字段。MyBatis-Plus 在调用方传入 {@link IPage} 参数时会自动
     * 在主 SQL 外层包装分页查询（自动追加 COUNT 与 LIMIT/OFFSET），无需手写额外 SQL。</p>
     *
     * @param page MyBatis-Plus 分页对象（{@code PageQuery.getPage()} 产出）
     * @return 分页后的 {@link Line} 列表（{@code IPage.records}）；total 自动由 MyBatis-Plus 计算
     */
    @Select("SELECT * FROM line ORDER BY create_time DESC")
    IPage<Line> listAll(IPage<Line> page);

    /**
     * 绑定方案用查询（PSM LineDAO.selectLine() 1:1）。
     *
     * <p>SQL 1:1 抄自 PSM XML {@code LineXml.xml#selectLine}：
     * <pre>{@code
     * select line.*, lo.order_value as "order" from line left join line_order lo on line.id = lo.line_id
     * }</pre>
     * </p>
     *
     * <p>{@code Line} entity 已声明 {@code @TableField(exist = false) private Integer order;}，
     * 因此查询结果里的 {@code order_value as "order"} 列会自动映射到 {@link Line#getOrder()} 字段
     （mybatis 默认下划线→驼峰，{@code order_value} → {@code orderValue}；此处 SELECT 用别名
     * {@code "order"} 避开 mybatis-plus 默认驼峰规则，避免与实体 {@code order} 字段冲突）。</p>
     *
     * @return 全量线体列表（含 {@code order} 排序值；line_order 表无记录时为 {@code null}）
     */
    @Select("SELECT line.*, lo.order_value AS \"order\" FROM line LEFT JOIN line_order lo ON line.id = lo.line_id")
    List<Line> selectLine();
}
