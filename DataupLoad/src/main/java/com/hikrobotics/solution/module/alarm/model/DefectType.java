package com.hikrobotics.solution.module.alarm.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DataupLoad defect_type 表映射实体（沿用 PSM DefectTypePO 字段）。
 * <p>
 * 实体名改为 DefectType（PO→裸实体）以避免与 PSM 历史包袱混淆。
 */
@TableName("defect_type")
public class DefectType implements Serializable {
   private static final long serialVersionUID = 1L;
   @TableId(value = "id", type = IdType.AUTO)
   private Integer id;
   private String name;
   private Integer category;
   private Boolean countEnable;
   private Integer countThreshold;
   private Boolean rateEnable;
   private Boolean showImgEnable;
   private Integer alarmEnable;
   private Integer sendYkEnable;
   private Integer soundEnable;
   private LocalDateTime updateTime;
   private LocalDateTime createTime;

   public Integer getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public Integer getCategory() {
      return this.category;
   }

   public Boolean getCountEnable() {
      return this.countEnable;
   }

   public Integer getCountThreshold() {
      return this.countThreshold;
   }

   public Boolean getRateEnable() {
      return this.rateEnable;
   }

   public Boolean getShowImgEnable() {
      return this.showImgEnable;
   }

   public Integer getAlarmEnable() {
      return this.alarmEnable;
   }

   public Integer getSendYkEnable() {
      return this.sendYkEnable;
   }

   public Integer getSoundEnable() {
      return this.soundEnable;
   }

   public LocalDateTime getUpdateTime() {
      return this.updateTime;
   }

   public LocalDateTime getCreateTime() {
      return this.createTime;
   }

   public DefectType setId(Integer id) {
      this.id = id;
      return this;
   }

   public DefectType setName(String name) {
      this.name = name;
      return this;
   }

   public DefectType setCategory(Integer category) {
      this.category = category;
      return this;
   }

   public DefectType setCountEnable(Boolean countEnable) {
      this.countEnable = countEnable;
      return this;
   }

   public DefectType setCountThreshold(Integer countThreshold) {
      this.countThreshold = countThreshold;
      return this;
   }

   public DefectType setRateEnable(Boolean rateEnable) {
      this.rateEnable = rateEnable;
      return this;
   }

   public DefectType setShowImgEnable(Boolean showImgEnable) {
      this.showImgEnable = showImgEnable;
      return this;
   }

   public DefectType setAlarmEnable(Integer alarmEnable) {
      this.alarmEnable = alarmEnable;
      return this;
   }

   public DefectType setSendYkEnable(Integer sendYkEnable) {
      this.sendYkEnable = sendYkEnable;
      return this;
   }

   public DefectType setSoundEnable(Integer soundEnable) {
      this.soundEnable = soundEnable;
      return this;
   }

   public DefectType setUpdateTime(LocalDateTime updateTime) {
      this.updateTime = updateTime;
      return this;
   }

   public DefectType setCreateTime(LocalDateTime createTime) {
      this.createTime = createTime;
      return this;
   }
}
