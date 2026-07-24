package com.hikrobotics.solution.module.line.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.dto.LineBodyDTO;
import com.hikrobotics.solution.module.line.dto.LinePanelQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO;
import com.hikrobotics.solution.module.line.dto.LineUpdateDTO;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.service.ILineService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 线体管理 Controller（W-LIN-03）。
 *
 * <p>路由 1:1 对齐 PSM 反编译产物
 * {@code com.hikrobotics.solution.module.line.web.LineController}（类级别
 * {@code @RequestMapping("/web/line")}）。共 11 个 endpoint：</p>
 *
 * <table>
 *   <caption>PSM 1:1 对照</caption>
 *   <tr><th>#</th><th>Method</th><th>Path</th><th>Service 调用</th></tr>
 *   <tr><td>1</td><td>GET</td><td>{@code /list}</td><td>{@code lineService.listAll(pageQuery)}（DataupLoad 沿用旧路径；PSM 用根 {@code /}）</td></tr>
 *   <tr><td>2</td><td>GET</td><td>{@code /{lineNo}}</td><td>{@code lineService.getByLineNo(lineNo)}（DataupLoad 扩展点）</td></tr>
 *   <tr><td>3</td><td>POST</td><td>{@code /}</td><td>{@code lineService.add(lineDTO)}（PSM add）</td></tr>
 *   <tr><td>4</td><td>PUT</td><td>{@code /}</td><td>{@code lineService.modify(lineUpdateDTO)}（PSM modify）</td></tr>
 *   <tr><td>5</td><td>DELETE</td><td>{@code /}</td><td>{@code lineService.delete(id)}（PSM delete）</td></tr>
 *   <tr><td>6</td><td>PUT</td><td>{@code /order}</td><td>PSM {@code lineService.chgLineOrder(lineOrders)}（<b>W-LIN-03 待补</b>）</td></tr>
 *   <tr><td>7</td><td>GET</td><td>{@code /tree}</td><td>PSM {@code lineService.handleLineTreeSearch()}（<b>W-LIN-03 待补</b>）</td></tr>
 *   <tr><td>8</td><td>POST</td><td>{@code /plan/bind}</td><td>{@code lineService.bindPlan(dto)}（PSM dispatchSolution）</td></tr>
 *   <tr><td>9</td><td>POST</td><td>{@code /plan/switch}</td><td>{@code lineService.switchPlan(dto)}（PSM switchSolution）</td></tr>
 *   <tr><td>10</td><td>GET</td><td>{@code /panel}</td><td>{@code lineService.planPanel(query)}（PSM planPanel）</td></tr>
 *   <tr><td>11</td><td>GET</td><td>{@code /status}</td><td>{@code lineService.planStatus(query)}（PSM planStatus）</td></tr>
 *   <tr><td>12</td><td>GET</td><td>{@code /tree-search}</td><td>{@code lineService.lineGroup()}（W-LIN-05 新增；PSM 等价于 {@code /group}）</td></tr>
 *   <tr><td>13</td><td>POST</td><td>{@code /chg-line-order}</td><td>{@code lineService.chgLineOrder(lineOrders)}（W-LIN-05 新增；PSM 等价于 {@code PUT /order}）</td></tr>
 *   <tr><td>14</td><td>GET</td><td>{@code /plan/manage}</td><td>stub — {@code lineService.planPanelListPage} 待补（W-LIN-05）</td></tr>
 * </table>
 *
 * <p>DataupLoad 与 PSM 路径差异说明：</p>
 * <ul>
 *   <li>PSM 类级别 {@code @RequestMapping("/web/line")} + 方法级别 {@code @GetMapping}
 *       → 实际路径 {@code GET /web/line}；DataupLoad 工单约定路径为
 *       {@code /web/line/list}（既已存在，保留以避免破坏现有调用方）。</li>
 *   <li>PSM {@code @PostMapping} / {@code @PutMapping} / {@code @DeleteMapping} 都没子路径
 *       → 实际路径 {@code POST/PUT/DELETE /web/line}；DataupLoad 同样保持类根。</li>
 *   <li>DataupLoad 额外保留 {@code GET /web/line/{lineNo}} 作为扩展（PSM 没有）。</li>
 *   <li>W-LIN-05 新增 {@code GET /tree-search}（PSM {@code /group}）、{@code POST /chg-line-order}
 *       （PSM {@code PUT /order}）、{@code GET /plan/manage}（PSM 无；stub）。</li>
 * </ul>
 *
 * <p>W-LIN-05 工单约束：</p>
 * <ul>
 *   <li>3 个 endpoint 与 PSM 行为对齐（{@code /tree-search} → lineGroup，
 *       {@code /chg-line-order} → chgLineOrder，{@code /plan/manage} → stub）</li>
 *   <li>每个 {@code @RequestParam} 加 {@code name="..."} 属性（避免 javac -parameters 警告）</li>
 *   <li>DTO 复用 DataupLoad 已有的 {@code line.dto} 包</li>
 *   <li>业务方法调用 W-LIN-05 已实现的 4 个新 service 方法（lineGroup / chgLineOrder /
 *       handleLineTreeSearch / listByLineNo(List)）</li>
 *   <li>{@code /tree} 与 {@code /tree-search} 共存（前者调用 {@code handleLineTreeSearch} 按 lineNo 构树，
 *       后者调用 {@code lineGroup} 返回 distinct name+lineNo 列表）</li>
 *   <li>{@code /order}（PSM 原路径）与 {@code /chg-line-order}（kebab-case 别名）共存</li>
 * </ul>
 *
 * <p>已知限制：{@code GET /plan/manage} 是 DataupLoad 自定义 endpoint（PSM 无对应），
 * 调用 {@code lineService.planPanelListPage} 但该 service 方法尚未实现（W-LIN-05 不新增 service 方法）。
 * 本 endpoint 以 stub 形式提供（{@code code=90003}），待后续工单在 service 层补齐后即可联通。</p>
 */
@RestController
@RequestMapping("/web/line")
public class LineController {

    @Autowired
    private ILineService lineService;

    // ============================================================
    // 1) GET /list — 列全部线体（DataupLoad 沿用既有路径；PSM 用根 /）
    // ============================================================
    /**
     * 列全部线体（分页可选，参数 pageNum / pageSize）。
     *
     * <p>DataupLoad 沿用工单 W-B05 既有路径 {@code /list}；PSM 反编译控制器使用根路径
     * {@code GET /web/line}。两者均可同时存在（不同路由），本实现只暴露 {@code /list}
     * 以避免破坏现有调用方。</p>
     */
    @GetMapping("/list")
    public BaseResult listAll(PageQuery pageQuery) {
        return this.lineService.listAll(pageQuery);
    }

    // ============================================================
    // 2) GET /{lineNo} — 按 lineNo 取一条（DataupLoad 扩展点，PSM 无此 endpoint）
    // ============================================================
    /**
     * 按 lineNo 取第一条匹配的线体记录。
     */
    @GetMapping("/{lineNo}")
    public BaseResult getByLineNo(@PathVariable(name = "lineNo") String lineNo) {
        Line line = this.lineService.getByLineNo(lineNo);
        if (line == null) {
            // 未找到：返回 200 + 空列表（避免框架把 msg 当 i18n key 解析导致 NoSuchMessageException）
            return BaseResult.build().data(List.of());
        }
        return BaseResult.build().data(line);
    }

    // ============================================================
    // 3) POST / — 新增产线（PSM add）
    // ============================================================
    /**
     * 新增产线（PSM LineController.add 1:1）。
     *
     * <p>请求体 {@link LineBodyDTO} 含 name/lineNo/faceNo/color/clientNo；
     * service 层做 (lineNo, faceNo) 唯一校验、clientNo 自动拼接、
     * 并联动 line_order 追加新行。</p>
     */
    @PostMapping
    public BaseResult add(@RequestBody LineBodyDTO lineDTO) {
        return this.lineService.add(lineDTO);
    }

    // ============================================================
    // 4) PUT / — 修改产线（PSM modify）
    // ============================================================
    /**
     * 修改产线（PSM LineController.modify 1:1）。
     *
     * <p>请求体 {@link LineUpdateDTO} 含 id/name/lineNo/faceNo/color/clientNo；
     * service 层按 id 排除自身做唯一校验、自动 clientNo 拼接。</p>
     */
    @PutMapping
    public BaseResult modify(@RequestBody LineUpdateDTO lineUpdateDTO) {
        return this.lineService.modify(lineUpdateDTO);
    }

    // ============================================================
    // 5) DELETE / — 删除产线（PSM delete）
    // ============================================================
    /**
     * 删除产线（PSM LineController.delete 1:1）。
     *
     * <p>查询参数 {@code id}（PSM {@code @RequestParam Integer id}）；
     * service 层校验客户端 ONLINE、联动 status_record 删除与掉线告警清理、
     * 同步从 line_order 表移除。</p>
     */
    @DeleteMapping
    public BaseResult delete(@RequestParam(name = "id") Integer id) {
        return this.lineService.delete(id);
    }

    // ============================================================
    // 6) PUT /order — 调整线体顺序（PSM chgLineOrder；W-LIN-05 兼容保留）
    // ============================================================
    /**
     * 调整线体顺序（PSM LineController.chgLineOrder 1:1）。
     *
     * <p>请求体 {@code List<ChgLineOrderDTO>}（lineId + order）。</p>
     *
     * <p>W-LIN-05：{@link ILineService#chgLineOrder(List)} 已实现，
     * 本 endpoint 联通调用该方法（size 校验 → modLineOrder）。</p>
     *
     * <p>保留原有 {@code PUT /order} 路由以兼容 PSM 客户端；新增 {@code POST /chg-line-order}（kebab-case）
     * 走相同 service 方法，二者并存。</p>
     */
    @PutMapping("/order")
    public BaseResult chgLineOrder(@RequestBody List<ChgLineOrderDTO> lineOrders) {
        return this.lineService.chgLineOrder(lineOrders);
    }

    // ============================================================
    // 6.5) POST /chg-line-order — 调整线体顺序（W-LIN-05 新增；别名 /order）
    // ============================================================
    /**
     * 调整线体顺序（W-LIN-05 新增 kebab-case 别名路由）。
     *
     * <p>DataupLoad 路径沿用工单约定的 {@code POST /chg-line-order}（PSM 原路径 {@code PUT /order}）；
     * 内部调用 {@link ILineService#chgLineOrder(List)}，与 {@code PUT /order} 等价。</p>
     */
    @PostMapping("/chg-line-order")
    public BaseResult chgLineOrderKebab(@RequestBody List<ChgLineOrderDTO> lineOrders) {
        return this.lineService.chgLineOrder(lineOrders);
    }

    // ============================================================
    // 7) GET /tree — 查询产线树（PSM searchLineTree；W-LIN-05 已联通）
    // ============================================================
    /**
     * 查询产线树（PSM LineController.searchLineTree 1:1）。
     *
     * <p>W-LIN-05：{@link ILineService#handleLineTreeSearch()} 已实现，
     * 本 endpoint 联通返回 {@code BaseResult.data(List<LineTreeItemDTO>)}（按 lineNo 分组，
     * 子节点为 faceNo）。</p>
     */
    @GetMapping("/tree")
    public BaseResult searchLineTree() {
        return this.lineService.handleLineTreeSearch();
    }

    // ============================================================
    // 7.5) GET /tree-search — 产线分组（PSM lineGroup；W-LIN-05 新增）
    // ============================================================
    /**
     * 产线分组查询（W-LIN-05 新增；PSM 对应 {@code LineController.lineGroup}）。
     *
     * <p>DataupLoad 路径沿用工单约定的 {@code /tree-search}（PSM 原路径 {@code GET /group}）；
     * 内部调用 {@link ILineService#lineGroup()}，返回 {@code BaseResult.data(List<Line>)}
     * （仅含 {@code name} + {@code lineNo} 字段的 distinct 行）。</p>
     *
     * <p>语义区别：{@code /tree}（按 lineNo 分组的父子树） vs  {@code /tree-search}（distinct name+lineNo 列表）。
     * 两者并存，各自对应 PSM 不同的 service 方法。</p>
     */
    @GetMapping("/tree-search")
    public BaseResult treeSearch() {
        return this.lineService.lineGroup();
    }

    // ============================================================
    // 8) POST /plan/bind — 配方分发（PSM dispatchSolution）
    // ============================================================
    /**
     * 配方分发（PSM LineController.dispatchSolution 1:1）。
     *
     * <p>请求体 {@link LinePlanBindDTO}（clientNo/lineId/planIds）；
     * service 层校验当前运行 plan 是否被取消（错误 20205）、
     * 清空旧 plan_to_line 并批量插入新行，最后通过 WebSocket 广播 {@code changePlan}。</p>
     */
    @PostMapping("/plan/bind")
    public BaseResult dispatchSolution(@RequestBody LinePlanBindDTO linePlanBindDTO) {
        return this.lineService.bindPlan(linePlanBindDTO);
    }

    // ============================================================
    // 9) POST /plan/switch — 配方切换（PSM switchSolution）
    // ============================================================
    /**
     * 配方切换（PSM LineController.switchSolution 1:1）。
     *
     * <p>请求体 {@link LinePlanSwitchDTO}（clientNo/lineId/planId）；
     * service 层校验目标 plan 是否在线（错误 20207）、
     * 旧 ENABLE → DISABLE、目标 → ENABLE，最后通过 WebSocket 广播。</p>
     */
    @PostMapping("/plan/switch")
    public BaseResult switchSolution(@RequestBody LinePlanSwitchDTO linePlanSwitchDTO) {
        return this.lineService.switchPlan(linePlanSwitchDTO);
    }

    // ============================================================
    // 10) GET /panel — 大屏面板聚合（PSM planPanel）
    // ============================================================
    /**
     * 大屏面板聚合（PSM LineController.planPanel 1:1）。
     *
     * <p>查询参数绑定到 {@link LinePanelQueryDTO}（继承 {@code TimePageQuery}，
     * 含 faceId + 时间范围字段）；service 层按 faceId 取 line 后聚合
     * lineCount / defectCount / alarmCount / statusRecord / toDayCount 五个数据集合。</p>
     */
    @GetMapping("/panel")
    public BaseResult planPanel(LinePanelQueryDTO linePanelQueryDTO) {
        return this.lineService.planPanel(linePanelQueryDTO);
    }

    // ============================================================
    // 11) GET /status — 大屏实时状态（PSM planStatus）
    // ============================================================
    /**
     * 大屏实时状态（PSM LineController.planStatus 1:1）。
     *
     * <p>查询参数绑定到 {@link LinePanelQueryDTO}（含 faceId）；
     * service 层按 faceId 取 line 后返回该 line/face 全部 status_record，
     * 不存在返回错误 20204。</p>
     */
    @GetMapping("/status")
    public BaseResult planStatus(LinePanelQueryDTO linePanelQueryDTO) {
        return this.lineService.planStatus(linePanelQueryDTO);
    }

    // ============================================================
    // 12) GET /plan/manage — 产线大屏管理（W-LIN-05 新增；PSM 无对应，暂以 stub 提供）
    // ============================================================
    /**
     * 产线大屏管理（W-LIN-05 新增）。
     *
     * <p>DataupLoad 自定义 endpoint（PSM 反编译中无对应路由）。
     * 工单说明：本 endpoint 应调用 {@code planPanel} 的 listPage 版本；
     * 但 DataupLoad 当前 {@link ILineService} 接口未声明分页版本的 planPanel 方法，
     * 且本工单约束不新增 service 类/方法，故本 endpoint 暂以 stub 形式路由。</p>
     *
     * <p>待后续工单：</p>
     * <ul>
     *   <li>在 {@code ILineService} 增加 {@code planPanelListPage(LinePanelQueryDTO) → BaseResult}</li>
     *   <li>把本方法体改为 {@code return this.lineService.planPanelListPage(linePanelQueryDTO);}</li>
     * </ul>
     *
     * <p>当前返回：{@code BaseResult.code(90003).error()}，语义与 W-LIN-03 stub 一致。</p>
     */
    @GetMapping("/plan/manage")
    public BaseResult planManage(LinePanelQueryDTO linePanelQueryDTO) {
        return BaseResult.build()
            .code(90003)
            .msgBody("W-LIN-05 pending: ILineService.planPanelListPage(LinePanelQueryDTO) not implemented yet")
            .error();
    }
}
