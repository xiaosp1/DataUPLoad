package com.hikrobotics.solution.module.defect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Maps;
import com.hikrobotics.solution.module.defect.service.ILineDefectTypeService;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.entity.LineDefectType;
import com.hikrobotics.solution.module.line.mapper.LineDefectTypeMapper;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Service;

/**
 * 1:1 抄自反编译 LineDefectTypeServiceImpl；
 * 用项目已有的 line/mapper/LineDefectTypeMapper + line/entity/LineDefectType。
 *
 * <p>修 PSM 反编译产物的类型转换（Wrappers.lambdaQuery() 在 JDK17 + mybatis-plus 3.5.3 下需要显式
 * 传 Class 才能正确推断 SFunction 的方法引用类型）。</p>
 */
@Service
public class LineDefectTypeServiceImpl
       extends ServiceImpl<LineDefectTypeMapper, LineDefectType>
       implements ILineDefectTypeService {

   private List<LineDefectType> listByLine(String faceNo, String lineNo) {
      LambdaQueryWrapper<LineDefectType> qw = Wrappers.<LineDefectType>lambdaQuery()
            .eq(LineDefectType::getLineNo, lineNo)
            .eq(LineDefectType::getFaceNo, faceNo);
      return this.list(qw);
   }

   @Override
   public Boolean addDefectTypeIfNotExist(Line line, List<DefectCountDTO> defects) {
      boolean result = true;
      if (CollectionUtils.isNotEmpty(defects)) {
         Map<String, LineDefectType> existDefectsOfLine = Maps.newHashMap();
         LambdaQueryWrapper<LineDefectType> qw = Wrappers.<LineDefectType>lambdaQuery()
               .eq(LineDefectType::getLineNo, line.getLineNo())
               .eq(LineDefectType::getFaceNo, line.getFaceNo());
         this.list(qw).forEach(defect -> existDefectsOfLine.put(defect.getName(), defect));
         List<String> uploadDefectNames = Lists.newArrayList();
         defects.forEach(defect -> {
            uploadDefectNames.add(defect.getType());
            LineDefectType value = existDefectsOfLine.getOrDefault(defect.getType(), new LineDefectType());
            value.setShowFlag(defect.getShowFlag()).setLineNo(line.getLineNo()).setFaceNo(line.getFaceNo()).setName(defect.getType());
            existDefectsOfLine.put(value.getName(), value);
         });
         result = this.saveOrUpdateBatch(existDefectsOfLine.values());
         if (result) {
            List<Integer> needDelDefectId = Lists.newArrayList();
            existDefectsOfLine.forEach((name, defect) -> {
               if (!uploadDefectNames.contains(name)) {
                  needDelDefectId.add(defect.getId());
               }
            });
            if (CollectionUtils.isNotEmpty(needDelDefectId)) {
               result = this.removeBatchByIds(needDelDefectId);
            }
         }
      }
      return result;
   }

   @Override
   public List<LineDefectType> listIfShowEnable(String lineNo, String faceNo) {
      LambdaQueryWrapper<LineDefectType> qw = Wrappers.<LineDefectType>lambdaQuery()
            .eq(LineDefectType::getLineNo, lineNo)
            .eq(LineDefectType::getFaceNo, faceNo)
            .eq(LineDefectType::getShowFlag, 1);
      return this.list(qw);
   }
}
