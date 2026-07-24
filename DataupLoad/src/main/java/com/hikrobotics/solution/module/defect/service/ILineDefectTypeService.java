package com.hikrobotics.solution.module.defect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.entity.LineDefectType;
import java.util.List;

/**
 * 1:1 抄自反编译 ILineDefectTypeService；
 * 用项目已有的 line/entity/LineDefectType（DataupLoad 里 PSM LineDefectTypePO 已并入 line.entity 包）。
 *
 * <p>本工单（W-B03）只需 addDefectTypeIfNotExist，由 DefectRecordServiceImpl.handleDetectData 调用；
 * listIfShowEnable 用于后续 detect 详情查询，本工单暂不展开。</p>
 */
public interface ILineDefectTypeService extends IService<LineDefectType> {
   Boolean addDefectTypeIfNotExist(Line line, List<DefectCountDTO> defects);

   List<LineDefectType> listIfShowEnable(String lineNo, String faceNo);

   // ============================================================
   // W-DFT-01b — 5 个 CRUD 方法（Controller 暴露用）
   // ============================================================

   /**
    * W-DFT-01b：新增生产线缺陷类型（PSM LineDefectTypeController.add 1:1）。
    *
    * <p>DataupLoad 沿用 {@link LineDefectType} 实体（PSM 是 {@code LineDefectTypePO}，
    * 字段一致：id / name / showFlag / lineNo / faceNo / updateTime / createTime）。</p>
    *
    * <p>对应 endpoint：{@code POST /web/defect/line-type}。</p>
    *
    * @param entity 待新增的缺陷类型（含 name / showFlag / lineNo / faceNo）
    */
   void add(LineDefectType entity);

   /**
    * W-DFT-01b：修改生产线缺陷类型（PSM LineDefectTypeController.modify 1:1）。
    *
    * <p>按 {@code entity.id} 主键更新。</p>
    *
    * <p>对应 endpoint：{@code PUT /web/defect/line-type}。</p>
    *
    * @param entity 含 id 的缺陷类型更新实体
    */
   void modify(LineDefectType entity);

   /**
    * W-DFT-01b：删除生产线缺陷类型（PSM LineDefectTypeController.delete 1:1）。
    *
    * <p>MyBatis-Plus {@code removeById(Integer)} 删除行数（PSM 沿用 MyBatis-Plus
    * {@code IService.removeById} 行为，返回 boolean；DataupLoad 接口签名按工单
    * 约定为 {@code int}，实现里 {@code boolean ? 1 : 0} 转换）。</p>
    *
    * <p>对应 endpoint：{@code DELETE /web/defect/line-type/{id}}。</p>
    *
    * @param id 缺陷类型主键
    * @return 受影响行数（1=删除成功，0=记录不存在）
    */
   int delete(Integer id);

   /**
    * W-DFT-01b：查询全部缺陷类型（PSM LineDefectTypeController.list 1:1）。
    *
    * <p>无分页、无过滤，返回全表所有行（PSM 等价 {@code IService.list()}）。</p>
    *
    * <p>对应 endpoint：{@code GET /web/defect/line-type/list}。</p>
    *
    * @return 全部缺陷类型列表
    */
   List<LineDefectType> listAll();

   /**
    * W-DFT-01b：按 lineNo 查询缺陷类型列表（PSM LineDefectTypeController.byLine 1:1）。
    *
    * <p><b>关于签名适配的说明：</b>工单 W-DFT-01b 最初定义为
    * {@code listByLineId(Integer lineId)}（{@code lambdaQuery().eq(LineDefectType::getLineId, lineId).list()}），
    * 但 DataupLoad {@link LineDefectType} 实体与 PSM {@code LineDefectTypePO} 字段一致 —
    * 主键是 {@code id}（Integer），业务线体关联是 {@code lineNo}（String）+ {@code faceNo}（String），
    * <b>不存在 {@code lineId} 字段</b>。PSM 反编译产物中也无 {@code lineId} 字段。</p>
    *
    * <p>本方法按"1:1 对齐 PSM 反编译产物"的原则调整：</p>
    * <ul>
    *   <li>参数改为 {@code String lineNo}（对齐 PSM 实体实际字段）</li>
    *   <li>实现改为 {@code lambdaQuery().eq(LineDefectType::getLineNo, lineNo).list()}</li>
    *   <li>Controller 端点保留工单约定的 {@code GET /web/defect/line-type/by-line/{lineNo}}
    *       （路径变量由 controller 层从 URL 取出后传给本方法）</li>
    * </ul>
    *
    * <p>对应 endpoint：{@code GET /web/defect/line-type/by-line/{lineNo}}。</p>
    *
    * @param lineNo 线体编号（{@code line_defect_type.line_no} 列）
    * @return 该线体下全部缺陷类型
    */
   List<LineDefectType> listByLineNo(String lineNo);
}
