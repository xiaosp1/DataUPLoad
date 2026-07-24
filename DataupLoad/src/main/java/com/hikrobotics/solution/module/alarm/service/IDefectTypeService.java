package com.hikrobotics.solution.module.alarm.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.dto.DefectTypeDTO;
import com.hikrobotics.solution.module.alarm.dto.SearchDefectDTO;
import com.hikrobotics.solution.module.alarm.model.DefectType;
import java.util.List;

/**
 * DataupLoad 缺陷类型服务接口（沿用 PSM IDefectTypeService 语义，去除 PO 后缀）。
 * <p>
 * DataupLoad 当前只用到 {@link #listByAttribute} 与 {@link #getByNameAndType}；其余 web 后台
 * CRUD 接口（DxfTypeController）留待后续迁移时按需补齐。
 */
public interface IDefectTypeService extends IService<DefectType> {
   BaseResult handleDefectTypeAdd(DefectTypeDTO var1);

   BaseResult handleDefectTypeDel(Integer var1);

   BaseResult listDefect(SearchDefectDTO var1);

   BaseResult editDefect(DefectTypeDTO var1);

   DefectType getByNameAndType(String var1, Integer var2);

   <T> List<DefectType> listByAttribute(T var1, SFunction<DefectType, T> var2);
}
