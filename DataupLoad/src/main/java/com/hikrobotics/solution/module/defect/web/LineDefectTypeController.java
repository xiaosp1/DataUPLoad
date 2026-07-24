package com.hikrobotics.solution.module.defect.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.defect.service.ILineDefectTypeService;
import com.hikrobotics.solution.module.line.entity.LineDefectType;
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
 * 生产线缺陷类型 Controller（W-DFT-01b）。
 *
 * <p>PSM 反编译产物中 <b>不存在</b> {@code LineDefectTypeController}（已确认 — defect 模块
 * 仅含 {@code mapper/LineDefectTypeDAO} + {@code model/LineDefectTypePO} +
 * {@code service/ILineDefectTypeService} + {@code service/imp/LineDefectTypeServiceImpl}，
 * <b>无</b> {@code web/LineDefectTypeController}）。PSM 实际是把 line_defect_type 的 CRUD
 * 暴露在 {@code alarm/web/DefectTypeController}（路由 {@code /web/defect}），管理的是
 * defect_type 字典（全局缺陷类型字典表），与 line_defect_type（按线体绑定的缺陷类型）不是
 * 同一张表。</p>
 *
 * <p>本 Controller 按工单 W-DFT-01b 新建，路由
 * {@code /web/defect/line-type/**}，从 PSM {@code alarm/web/DefectTypeController} 借样式
 * （类级别 {@code @RequestMapping("/web/defect/line-type")} + 方法级别 {@code @PostMapping} /
 * {@code @PutMapping} / {@code @DeleteMapping} / {@code @GetMapping}），5 个 endpoint 全部
 * 对齐 PSM DefectTypeController 的方法语义（add / del / list / edit + 新增 byLine）。</p>
 *
 * <p>5 个 endpoint：</p>
 * <table>
 *   <caption>W-DFT-01b endpoint 列表</caption>
 *   <tr><th>#</th><th>Method</th><th>Path</th><th>Service 调用</th></tr>
 *   <tr><td>1</td><td>POST</td><td>{@code /}</td><td>{@code lineDefectTypeService.add(entity)}</td></tr>
 *   <tr><td>2</td><td>PUT</td><td>{@code /}</td><td>{@code lineDefectTypeService.modify(entity)}</td></tr>
 *   <tr><td>3</td><td>DELETE</td><td>{@code /{id}}</td><td>{@code lineDefectTypeService.delete(id)}</td></tr>
 *   <tr><td>4</td><td>GET</td><td>{@code /list}</td><td>{@code lineDefectTypeService.listAll()}</td></tr>
 *   <tr><td>5</td><td>GET</td><td>{@code /by-line/{lineNo}}</td><td>{@code lineDefectTypeService.listByLineNo(lineNo)}</td></tr>
 * </table>
 *
 * <p>W-DFT-01b 关键约束：</p>
 * <ul>
 *   <li>每个 {@code @RequestParam} / {@code @PathVariable} 显式声明 {@code name} 属性（避免
 *       依赖编译期参数名反射；与 DataupLoad 既有 {@code line/web/*} 规范一致）</li>
 *   <li>不修改其它模块（alarm / line / detect 等）</li>
 *   <li>{@code listByLineNo} 路径变量对齐工单约定的 {@code /by-line/{lineId}} 改为
 *       {@code /by-line/{lineNo}} — 因 {@link LineDefectType} 实体无 {@code lineId} 字段，
 *       业务关联键是 {@code lineNo}（String，{@code line_defect_type.line_no} 列），
 *       详见 {@link ILineDefectTypeService#listByLineNo(String)} 注释</li>
 * </ul>
 */
@RestController
@RequestMapping("/web/defect/line-type")
public class LineDefectTypeController {

    @Autowired
    private ILineDefectTypeService lineDefectTypeService;

    // ============================================================
    // 1) POST / — 新增缺陷类型
    // ============================================================
    /**
     * 新增缺陷类型（PSM DefectTypeController.addDefectType 样式 1:1）。
     *
     * <p>请求体 {@link LineDefectType}（id 可为空，由 PG 自增；
     * 含 name / showFlag / lineNo / faceNo）。</p>
     */
    @PostMapping
    public BaseResult add(@RequestBody LineDefectType entity) {
        this.lineDefectTypeService.add(entity);
        return BaseResult.build().data(entity);
    }

    // ============================================================
    // 2) PUT / — 修改缺陷类型（按 id 主键）
    // ============================================================
    /**
     * 修改缺陷类型（PSM DefectTypeController.editDefect 样式 1:1）。
     *
     * <p>请求体 {@link LineDefectType}（必须含 id；按 id 主键做 UPDATE）。</p>
     */
    @PutMapping
    public BaseResult modify(@RequestBody LineDefectType entity) {
        this.lineDefectTypeService.modify(entity);
        return BaseResult.build().data(entity);
    }

    // ============================================================
    // 3) DELETE /{id} — 删除缺陷类型（按 id 主键）
    // ============================================================
    /**
     * 删除缺陷类型（PSM DefectTypeController.delDefectType 样式 1:1）。
     *
     * <p>路径变量 {@code id}（缺陷类型主键）。DataupLoad 接口签名为
     * {@code int delete(Integer)}，本 controller 把受影响行数原样回传，调用方可据此判断
     * 是否实际命中记录（0 = id 不存在）。</p>
     */
    @DeleteMapping("/{id}")
    public BaseResult delete(@PathVariable(name = "id") Integer id) {
        int affected = this.lineDefectTypeService.delete(id);
        return BaseResult.build().data(affected);
    }

    // ============================================================
    // 4) GET /list — 查全部缺陷类型
    // ============================================================
    /**
     * 查询全部缺陷类型（PSM DefectTypeController.listDefect 样式 1:1，无分页）。
     *
     * <p>全表 SELECT，无过滤；返回 {@code BaseResult.data(List<LineDefectType>)}。</p>
     *
     * <p>DataupLoad 路径沿用工单约定的 {@code /list}（PSM DefectTypeController.listDefect
     * 使用根路径 {@code GET /web/defect}，本工单为与 line 模块 controller 风格一致，
     * 子路径 {@code /list}）。</p>
     */
    @GetMapping("/list")
    public BaseResult listAll() {
        List<LineDefectType> data = this.lineDefectTypeService.listAll();
        return BaseResult.build().data(data);
    }

    // ============================================================
    // 5) GET /by-line/{lineNo} — 按线体查询缺陷类型
    // ============================================================
    /**
     * 按线体编号查询缺陷类型列表（W-DFT-01b 新增 endpoint，PSM 无对应）。
     *
     * <p>路径变量对齐工单约定的 {@code /by-line/{lineId}} 调整为
     * {@code /by-line/{lineNo}} — 因 {@link LineDefectType} 实体无 {@code lineId} 字段，
     * 业务关联键是 {@code lineNo}（String，{@code line_defect_type.line_no} 列），
     * 详见 {@link ILineDefectTypeService#listByLineNo(String)} 注释。</p>
     *
     * <p>实现：{@code this.lineDefectTypeService.listByLineNo(lineNo)}（MyBatis-Plus
     * {@code lambdaQuery().eq(getLineNo, lineNo).list()}）。</p>
     *
     * @param lineNo 线体编号（{@code line_defect_type.line_no} 列）
     */
    @GetMapping("/by-line/{lineNo}")
    public BaseResult listByLine(@PathVariable(name = "lineNo") String lineNo) {
        List<LineDefectType> data = this.lineDefectTypeService.listByLineNo(lineNo);
        return BaseResult.build().data(data);
    }

    // ============================================================
    // 工单附录 — 关于 {@code @RequestParam} 命名的额外说明
    // ============================================================
    //
    // 当前 5 个 endpoint 用到 {@code @RequestParam} 的位置仅 DELETE 方法不存在
    // （路径变量直接走 {@code @PathVariable}）。{@code GET /list} 和 {@code GET /by-line/{lineNo}}
    // 均无 query/form 参数（全部走路径变量）。
    //
    // 如果后续业务需要在 POST/PUT 上加额外 query 参数（如 audit-by / dryRun），
    // 按 DataupLoad 规范统一写成：
    //
    //     @RequestParam(name = "auditBy", required = false) String auditBy
    //
    // 以避免 javac -parameters 警告 + 兼容反编译产物中可能缺失参数名的场景。
}
