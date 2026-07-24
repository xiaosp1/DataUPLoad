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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
         List<String> uploadDefectNames = new ArrayList<>();
         defects.forEach(defect -> {
            uploadDefectNames.add(defect.getType());
            LineDefectType value = existDefectsOfLine.getOrDefault(defect.getType(), new LineDefectType());
            value.setShowFlag(defect.getShowFlag()).setLineNo(line.getLineNo()).setFaceNo(line.getFaceNo()).setName(defect.getType());
            existDefectsOfLine.put(value.getName(), value);
         });
         result = this.saveOrUpdateBatch(existDefectsOfLine.values());
         if (result) {
            List<Integer> needDelDefectId = new ArrayList<>();
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

   // ============================================================
   // W-DFT-01b — 5 个 CRUD 方法（Controller 暴露用，PSM LineDefectTypeServiceImpl 1:1）
   // ============================================================

   /**
    * W-DFT-01b：新增缺陷类型（PSM 1:1）。
    *
    * <p>用 MyBatis-Plus {@code IService.save(LineDefectType)} — 实体无 id 时走 INSERT
    * 自增主键（{@code @TableId(type = IdType.AUTO)}），有 id 时走 INSERT_OR_UPDATE。</p>
    *
    * <p>调用方应在 controller 层做参数校验（{@code @RequestBody} 反序列化后由 Spring 校验
    * 或者 service 入口校验）。本工单沿用 PSM 简单 save，不做业务校验（PSM 也未做）。</p>
    */
   @Override
   public void add(LineDefectType entity) {
      this.save(entity);
   }

   /**
    * W-DFT-01b：按 id 更新缺陷类型（PSM 1:1）。
    *
    * <p>用 MyBatis-Plus {@code IService.updateById(LineDefectType)} — 按 {@code entity.id}
    * 主键做 UPDATE（非空字段）。</p>
    */
   @Override
   public void modify(LineDefectType entity) {
      this.updateById(entity);
   }

   /**
    * W-DFT-01b：按 id 删除缺陷类型（PSM 1:1）。
    *
    * <p>用 MyBatis-Plus {@code IService.removeById(Integer)} — 返回 boolean，
    * 转 int 给接口（true → 1, false → 0）。</p>
    */
   @Override
   public int delete(Integer id) {
      return this.removeById(id) ? 1 : 0;
   }

   /**
    * W-DFT-01b：查询全部缺陷类型（PSM 1:1）。
    *
    * <p>用 MyBatis-Plus {@code IService.list()} — 无过滤全表 SELECT。</p>
    */
   @Override
   public List<LineDefectType> listAll() {
      return this.list();
   }

   /**
    * W-DFT-01b：按 lineNo 查询缺陷类型（PSM 1:1）。
    *
    * <p>用 MyBatis-Plus {@code lambdaQuery().eq(getLineNo, lineNo).list()}。</p>
    *
    * <p>实体字段对齐说明：见 {@link ILineDefectTypeService#listByLineNo(String)} 注释 —
    * PSM/DataupLoad 实体均无 {@code lineId} 字段，业务关联是 {@code lineNo}（String）。</p>
    */
   @Override
   public List<LineDefectType> listByLineNo(String lineNo) {
      LambdaQueryWrapper<LineDefectType> qw = Wrappers.<LineDefectType>lambdaQuery()
            .eq(LineDefectType::getLineNo, lineNo);
      return this.list(qw);
   }
}
