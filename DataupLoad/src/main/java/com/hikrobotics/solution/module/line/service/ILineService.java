package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.module.line.dto.LineBodyDTO;
import com.hikrobotics.solution.module.line.dto.LinePanelQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO;
import com.hikrobotics.solution.module.line.dto.LineUpdateDTO;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.model.LinePO;
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
 * <p>DataupLoad 沿用 {@link Line} 作为 entity（PSM 是 {@link LinePO}，字段一致）。
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
}
