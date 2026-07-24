package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.dto.LineBodyDTO;
import com.hikrobotics.solution.module.line.dto.LinePanelQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO;
import com.hikrobotics.solution.module.line.dto.LineUpdateDTO;
import com.hikrobotics.solution.module.line.entity.Line;
import java.util.List;

/**
 * 线体服务接口（W-LIN-01 1:1 对齐 PSM 反编译 ILineService 的核心业务方法子集）。
 *
 * <p>本工单（W-LIN-01）补齐 7 个接口方法 + {@link LineServiceImpl#init()} Impl 内 @PostConstruct：
 * <ul>
 *   <li>{@link #add(LineBodyDTO)} 新增产线</li>
 *   <li>{@link #modify(LineUpdateDTO)} 修改产线</li>
 *   <li>{@link #delete(Integer)} 删除</li>
 *   <li>{@link #bindPlan(LinePlanBindDTO)} 配方分发</li>
 *   <li>{@link #switchPlan(LinePlanSwitchDTO)} 配方切换</li>
 *   <li>{@link #planPanel(LinePanelQueryDTO)} 大屏面板聚合</li>
 *   <li>{@link #planStatus(LinePanelQueryDTO)} 大屏实时状态</li>
 * </ul>
 *
 * <p>DataupLoad 沿用 {@link Line} 作为 entity（PSM 是 LinePO，字段一致；W-CLEAN-03 起 LinePO 已删除）。
 * `init()` 方法不进入接口（@PostConstruct 生命周期回调，由 Spring 容器调用，
 * 详见 PSM LineServiceImpl 类级别声明）。</p>
 */
public interface ILineService extends IService<Line> {

    /**
     * 查询全部线体（分页可选；W-B03 既有方法）。
     */
    BaseResult listAll(PageQuery pageQuery);

    /**
     * W-LIN-01：新增产线（PSM 1:1）。
     *
     * @param dto 线体基本信息
     */
    BaseResult add(LineBodyDTO dto);

    /**
     * W-LIN-01：修改产线（PSM 1:1）。
     *
     * @param dto 含 id 的线体更新 DTO
     */
    BaseResult modify(LineUpdateDTO dto);

    /**
     * W-LIN-01：删除产线（PSM 1:1；含掉线客户端告警联动）。
     *
     * @param id 线体主键
     */
    BaseResult delete(Integer id);

    /**
     * W-LIN-01：配方分发（PSM 1:1；WebSocket 广播通知 client）。
     *
     * @param dto 配方绑定 DTO
     */
    BaseResult bindPlan(LinePlanBindDTO dto);

    /**
     * W-LIN-01：配方切换（PSM 1:1；WebSocket 广播通知 client）。
     *
     * @param dto 配方切换 DTO
     */
    BaseResult switchPlan(LinePlanSwitchDTO dto);

    /**
     * W-LIN-01：大屏面板聚合（PSM 1:1）。
     *
     * @param query 查询条件（含 faceId + 时间范围）
     */
    BaseResult planPanel(LinePanelQueryDTO query);

    /**
     * W-LIN-01：大屏实时状态（PSM 1:1）。
     *
     * @param query 查询条件（含 faceId）
     */
    BaseResult planStatus(LinePanelQueryDTO query);

    /**
     * W-B03：根据 lineNo 查询线体列表（一个 lineNo 可对应多 face）。
     */
    List<Line> listByLineNo(String lineNo);

    /**
     * W-B03：按 lineNo 取第一条匹配记录，便于 controller 返回单个对象。
     */
    Line getByLineNo(String lineNo);

    /**
     * W-B03：按 (lineNo, faceNo) 取单条记录（W-B03 detect 模块 handleDetectData 唯一需要的扩展点）。
     */
    Line getByLineNoAndFaceNo(String lineNo, String faceNo);

    /**
     * W-B05 工单 SCRN-1 大屏模块所需：返回全部线体（PSM 等价签名 {@code listLine()}）。
     */
    List<Line> listLine();

    // ============================================================
    // W-LIN-05 — PSM 1:1 对齐 ILineService 剩余 4 个方法
    // ============================================================

    /**
     * W-LIN-05：产线分组查询（PSM 1:1，对应 PSM LineController.lineGroup）。
     *
     * <p>PSM 实现：{@code lineDAO.selectList(new QueryWrapper().select("distinct NAME,line_no"))}，
     * 返回 {@code BaseResult.data(List<LinePO> distinct name+line_no>)}（PSM 反编译引用 PSM LinePO，字段一致）。
     * DataupLoad 沿用 PSM 语义，{@code Line} 实体在 DataupLoad 中承担 PSM LinePO 角色。</p>
     */
    BaseResult lineGroup();

    /**
     * W-LIN-05：调整线体顺序（PSM 1:1，对应 PSM LineController.chgLineOrder）。
     *
     * <p>校验入参 size 与 line 表总记录数一致，否则返回错误 20209；
     * 调用 {@code lineOrderService.modLineOrder} 返回 false 则错误 20210；
     * 否则成功。</p>
     */
    BaseResult chgLineOrder(List<ChgLineOrderDTO> lineOrders);

    /**
     * W-LIN-05：产线树查询（PSM 1:1，对应 PSM LineController.searchLineTree）。
     *
     * <p>PSM 实现：按 lineNo 分组构建 {@code LineTreeItemDTO} 树结构
     * （父节点=lineNo，子节点=faceNo），返回 {@code BaseResult.data(List<LineTreeItemDTO>)}。</p>
     */
    BaseResult handleLineTreeSearch();

    /**
     * W-LIN-05：按 lineNo 列表批量查询线体（PSM 1:1 重载）。
     *
     * <p>PSM 实现：{@code list(Wrappers.lambdaQuery().in(LinePO::getLineNo, lineNos))}（PSM 反编译引用 PSM LinePO，字段一致），
     * 入参为空时返回空列表。</p>
     *
     * <p>DataupLoad 沿用 PSM 1:1，重载已有的 {@link #listByLineNo(String)}（W-B03 单参版本）
     * — Java 按参数类型分派，无歧义。</p>
     */
    List<Line> listByLineNo(List<String> lineNos);

    // ============================================================
    // W-LIN-06 — plan/manage endpoint 真实业务实装
    // ============================================================

    /**
     * W-LIN-06：产线配方大屏管理（PSM 反编译中无完全同名的 service 方法，
     * 名称沿用 PSM 反编译 {@code PlanServiceImpl.clientPlan(ClientPlanQueryDTO)}
     * 的业务语义与 DTO 形态，返回 {@code List<ClientPlanResultDTO>}）。
     *
     * <p>语义（PSM {@code PlanServiceImpl.clientPlan} 1:1）：
     * 按 {@code (lineNo, faceNo)} 联查 {@code plan} × {@code plan_to_line} × {@code line}，
     * 返回该线下面向客户端展示的配方信息（{@code name/uri/description/status/updateTime/createTime}）。</p>
     *
     * <p>DataupLoad 改造：原 stub {@code /plan/manage} 由 W-LIN-05 引入（{@code code=90003}），
     * 本工单将 service 入口从 {@code PlanServiceImpl.clientPlan(ClientPlanQueryDTO)}
     * 迁移到 {@code LineServiceImpl.planOrderDtos(String, String, Integer, Integer)}，
     * 并在前端契约上补齐 {@code page / size} 两个分页参数（PSM 无分页；DataupLoad 沿用
     * MyBatis Plus {@code IPage<ClientPlanResultDTO>} 包装以匹配项目其它 listPage 端点）。</p>
     *
     * @param lineNo 产线编号（必填）
     * @param faceNo 面编号（必填）
     * @param page   页码（从 1 开始；{@code null} 或 {@code <= 0} 视为第 1 页）
     * @param size   每页条数（{@code null} 或 {@code <= 0} 时退化为不分页，返回全量列表）
     * @return {@code BaseResult.data}：
     *         <ul>
     *           <li>分页模式 → {@code BaseResult.data(IPage<ClientPlanResultDTO>)}</li>
     *           <li>不分页模式 → {@code BaseResult.data(List<ClientPlanResultDTO>)}</li>
     *         </ul>
     */
    BaseResult planOrderDtos(String lineNo, String faceNo, Integer page, Integer size);
}
