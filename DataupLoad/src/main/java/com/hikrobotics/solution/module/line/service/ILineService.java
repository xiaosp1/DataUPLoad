package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.module.line.entity.Line;
import java.util.List;

/**
 * 线体服务接口（W-B03 + W-B05 子集）
 *
 * <p>完全照抄 PSM 反编译产物 ILineService 的接口签名，
 * 但本工单只实现 listAll / getByLineNo / getByLineNoAndFaceNo 三个方法，
 * 其余方法（add/modify/delete/bindPlan/...）在后续工单补齐。</p>
 *
 * <p>W-B03 扩展：新增 {@link #getByLineNoAndFaceNo} 专供 DefectRecordServiceImpl.handleDetectData 使用。</p>
 */
public interface ILineService extends IService<Line> {

    /**
     * 查询全部线体（分页可选）。
     */
    BaseResult listAll(PageQuery pageQuery);

    /**
     * 根据 lineNo 查询线体列表（一个 lineNo 可对应多 face）。
     */
    List<Line> listByLineNo(String lineNo);

    /**
     * 按 lineNo 取第一条匹配记录，便于 controller 返回单个对象。
     */
    Line getByLineNo(String lineNo);

    /**
     * 按 (lineNo, faceNo) 取单条记录（W-B03 detect 模块 handleDetectData 唯一需要的扩展点）。
     */
    Line getByLineNoAndFaceNo(String lineNo, String faceNo);

    /**
     * 工单 SCRN-1 大屏模块所需：返回全部线体（PSM 等价签名 {@code listLine()}，不带分页，
     * 按 id 升序。ScreenServiceImpl 会再做 sorted by order+color）。
     */
    List<Line> listLine();
}
