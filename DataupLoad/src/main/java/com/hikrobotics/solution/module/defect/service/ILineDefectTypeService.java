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
}
