package com.hikrobotics.solution.module.alarm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.common.constants.StateEnum;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.alarm.dto.DefectTypeDTO;
import com.hikrobotics.solution.module.alarm.dto.SearchDefectDTO;
import com.hikrobotics.solution.module.alarm.mapper.DefectTypeMapper;
import com.hikrobotics.solution.module.alarm.model.DefectType;
import com.hikrobotics.solution.module.alarm.service.IDefectTypeService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * DataupLoad 缺陷类型 Service 实现（W-DEFECT-CFG 子单 A：补齐 4 个 CRUD 空壳）。
 *
 * <h2>W-DEFECT-CFG 子单 A — CRUD 1:1 对齐 PSM</h2>
 * <p>
 * 此前 4 个 web 后台管理方法（{@code handleDefectTypeAdd / handleDefectTypeDel / listDefect / editDefect}）
 * 全部为 {@code BaseResult.build().ok()} 空壳，本工单按 PSM
 * {@code DefectTypeServiceImpl} 反编译产物逐字迁回，关键行为：
 * <ul>
 *   <li>{@link #handleDefectTypeAdd}：name+category 查重 → 错误 20502；DTO → Entity 拷贝；count/rate/showImg 默认 false。</li>
 *   <li>{@link #handleDefectTypeDel}：按 id 删；不存在 → 错误 20505；删除失败 → 错误 20001。</li>
 *   <li>{@link #listDefect}：name 模糊 + category 精确过滤，按 category/createTime DESC，分页返回。</li>
 *   <li>{@link #editDefect}：按 id 改；不存在 → 20505；name+category 重名（排除自己） → 20502；
 *       soundEnable=1 + alarmEnable=0 → 20503；全字段覆盖更新。</li>
 * </ul>
 *
 * <h3>保留方法（DataupLoad 沿用，PSM 也有）</h3>
 * <ul>
 *   <li>{@link #getByNameAndType} —— 报警推送链路查 defect_type；本工单 B 子单钩入推送逻辑时调用。</li>
 *   <li>{@link #listByAttribute} —— {@code AlarmRecordServiceImpl} 多处复用（按 alarmEnable/category 拉缺陷名清单）。</li>
 * </ul>
 *
 * <h3>DataupLoad 与 PSM 的差异</h3>
 * <ol>
 *   <li>PSM 用 {@code DefectTypeDAO}，DataupLoad 用 MyBatis-Plus {@code BaseMapper<DefectType>}，调用方式一致。</li>
 *   <li>PSM 实体 {@code DefectTypePO}，DataupLoad 改名为 {@code DefectType}（去除 PO 后缀，详见 entity 文件）。</li>
 *   <li>PSM {@code editDefect} 直接用 {@code BeanUtil.copyProperties} 全覆盖；DataupLoad 沿用同款语义。</li>
 * </ol>
 */
@Service
public class DefectTypeServiceImpl extends ServiceImpl<DefectTypeMapper, DefectType> implements IDefectTypeService {
   private static final Logger log = LoggerFactory.getLogger(DefectTypeServiceImpl.class);

   @Override
   public BaseResult handleDefectTypeAdd(DefectTypeDTO form) {
      // name+category 查重（PSM 同款：用 getByNameAndType 而非普通 query，保持一致性）
      DefectType exist = this.getByNameAndType(form.getName(), form.getCategory());
      if (exist != null) {
         log.warn("add defect type failed, name+category already exists.[name={}, category={}]",
            form.getName(), form.getCategory());
         return BaseResult.build().error("20502").log("defect name+category duplicate", form.getName() + ":" + form.getCategory());
      }

      DefectType defectType = BeanUtil.copyProperties(form, DefectType.class);
      // PSM 默认字段
      defectType
         .setCountEnable(false)
         .setCountThreshold(0)
         .setRateEnable(false)
         .setShowImgEnable(false);
      defectType.setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now());
      this.save(defectType);
      log.info("add defect type success.[id={}, name={}, category={}]",
         defectType.getId(), defectType.getName(), defectType.getCategory());
      return BaseResult.build().ok();
   }

   @Override
   public BaseResult handleDefectTypeDel(Integer id) {
      DefectType defectType = this.getById(id);
      if (defectType == null) {
         return BaseResult.build().error("20505").log("defect type not found", id);
      }
      boolean ok = this.removeById(id);
      if (!ok) {
         return BaseResult.build().error("20001").log("defect type delete failed", id);
      }
      log.info("delete defect type success.[id={}, name={}]", id, defectType.getName());
      return BaseResult.build().ok();
   }

   @Override
   public BaseResult editDefect(DefectTypeDTO form) {
      DefectType defectType = this.getById(form.getId());
      if (defectType == null) {
         return BaseResult.build().error("20505").log("defect type not found", form.getId());
      }

      // name+category 查重（排除自己）
      DefectType commonName = this.getByNameAndType(form.getName(), form.getCategory());
      if (commonName != null && !Objects.equals(commonName.getId(), defectType.getId())) {
         log.warn("edit defect type failed, name+category duplicate.[name={}, category={}]",
            form.getName(), form.getCategory());
         return BaseResult.build().error("20502").log("defect name+category duplicate",
            form.getName() + ":" + form.getCategory());
      }

      // soundEnable=1 + alarmEnable=0 不合理（声音依赖推送大屏）→ PSM 错误 20503
      if (Objects.equals(form.getSoundEnable(), StateEnum.YES.getValue())
         && Objects.equals(form.getAlarmEnable(), StateEnum.NO.getValue())) {
         log.warn("edit defect type failed, soundEnable=1 but alarmEnable=0.[id={}]", form.getId());
         return BaseResult.build().error("20503").log("soundEnable requires alarmEnable", form.getId());
      }

      BeanUtil.copyProperties(form, defectType);
      defectType.setUpdateTime(LocalDateTime.now());
      boolean ok = this.updateById(defectType);
      if (!ok) {
         return BaseResult.build().error("20001").log("defect type update failed", form.getId());
      }
      log.info("edit defect type success.[id={}, name={}, category={}, alarmEnable={}, soundEnable={}, sendYkEnable={}]",
         defectType.getId(), defectType.getName(), defectType.getCategory(),
         defectType.getAlarmEnable(), defectType.getSoundEnable(), defectType.getSendYkEnable());
      return BaseResult.build().ok();
   }

   @Override
   public BaseResult listDefect(SearchDefectDTO form) {
      // PSM 1:1：name 模糊 + category 精确过滤；orderByDesc(category, createTime)
      LambdaQueryWrapper<DefectType> qw = Wrappers.<DefectType>lambdaQuery()
         .eq(form.getCategory() != null, DefectType::getCategory, form.getCategory())
         .like(StringUtils.isNotBlank(form.getName()), DefectType::getName, form.getName())
         .orderByDesc(DefectType::getCategory)
         .orderByDesc(DefectType::getCreateTime);

      IPage<DefectType> page = this.page(form.getPage(), qw);
      log.debug("list defect type.[name={}, category={}, total={}]",
         form.getName(), form.getCategory(), page.getTotal());
      return BaseResult.build().data(page);
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
   public <T> List<DefectType> listByAttribute(T value, SFunction<DefectType, T> column) {
      if (value == null || column == null) {
         return Collections.emptyList();
      }
      LambdaQueryWrapper<DefectType> qw = Wrappers.<DefectType>lambdaQuery().eq(column, value);
      return this.list(qw);
   }
}
