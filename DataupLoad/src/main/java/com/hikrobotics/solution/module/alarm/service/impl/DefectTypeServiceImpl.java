package com.hikrobotics.solution.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.dto.DefectTypeDTO;
import com.hikrobotics.solution.module.alarm.dto.SearchDefectDTO;
import com.hikrobotics.solution.module.alarm.mapper.DefectTypeMapper;
import com.hikrobotics.solution.module.alarm.model.DefectType;
import com.hikrobotics.solution.module.alarm.service.IDefectTypeService;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * DataupLoad 缺陷类型 Service 实现。仅实现 DataupLoad alarm 链路真正调用的方法
 * （getByNameAndType / listByAttribute），其余 CRUD 接口留空返回 OK，
 * 后续迁移 PSM DefectTypeController 时再补齐。
 */
@Service
public class DefectTypeServiceImpl extends ServiceImpl<DefectTypeMapper, DefectType> implements IDefectTypeService {

   @Override
   public BaseResult handleDefectTypeAdd(DefectTypeDTO form) {
      return BaseResult.build().ok();
   }

   @Override
   public BaseResult handleDefectTypeDel(Integer id) {
      return BaseResult.build().ok();
   }

   @Override
   public BaseResult listDefect(SearchDefectDTO form) {
      return BaseResult.build().ok();
   }

   @Override
   public BaseResult editDefect(DefectTypeDTO form) {
      return BaseResult.build().ok();
   }

   @Override
   public DefectType getByNameAndType(String name, Integer type) {
      if (name == null || type == null) {
         return null;
      }
      LambdaQueryWrapper<DefectType> qw = Wrappers.<DefectType>lambdaQuery()
         .eq(DefectType::getName, name)
         .eq(DefectType::getCategory, type);
      return this.getOne(qw, false);
   }

   @Override
   public <T> List<DefectType> listByAttribute(T value, com.baomidou.mybatisplus.core.toolkit.support.SFunction<DefectType, T> column) {
      if (value == null || column == null) {
         return Collections.emptyList();
      }
      LambdaQueryWrapper<DefectType> qw = Wrappers.<DefectType>lambdaQuery().eq(column, value);
      return this.list(qw);
   }
}
