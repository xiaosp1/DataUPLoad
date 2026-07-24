package com.hikrobotics.solution.module.detect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * PG 表 defect_day_record 映射。
 * 字段顺序/命名 1:1 抄自反编译 DefectDayRecordPO；只保留数据库映射和基础 setter/getter。
 *
 * <p>工单 W-DET-04 补齐 PSM DefectDayRecordPO#getLocalTime()：{@code time} 字段是 {@code String}，
 * 通过 {@code HikDateUtil.transformTime(String)} 解析为 {@code LocalDateTime} 后取 {@code LocalTime}，
 * 用于 {@code handleStatisticDataExport} 中按 {@code defect.getLocalTime().isBefore(Eight)} 区分
 * 早晚班（见审计报告 2026-07-24-detect-audit.md §文件级判定 / entity/DefectDayRecord）。</p>
 */
@TableName("defect_day_record")
public class DefectDayRecord implements Serializable {
   private static final long serialVersionUID = 1L;

   @TableId(value = "id", type = IdType.AUTO)
   private Integer id;

   @TableField("count")
   private Integer count;

   @TableField("time")
   private String time;

   @TableField("line_no")
   private String lineNo;

   @TableField("face_no")
   private String faceNo;

   @TableField("type")
   private String type;

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   @TableField("update_time")
   private LocalDateTime updateTime;

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   @TableField("create_time")
   private LocalDateTime createTime;

   public String getPos() {
      return this.lineNo + ":" + this.faceNo;
   }

   /**
    * 工单 W-DET-04：PSM {@code DefectDayRecordPO.getLocalTime()} 1:1。
    *
    * <p>将 {@link #time}（String，如 {@code "yyyy-MM-dd HH:mm:ss"}）通过
    * {@link HikDateUtil#transformTime(String)} 解析为 {@link LocalDateTime}，再取 {@link LocalTime} 部分。
    * 调用方（如 {@code DefectRecordServiceImpl.handleStatisticDataExport}）用
    * {@code defect.getLocalTime().isBefore(Eight)} 判断是否 8 点前的夜班。</p>
    *
    * <p><b>NPE 风险</b>：PSM 同款未做 null 检查，{@code time == null} 时
    * {@code HikDateUtil.transformTime(null)} 行为依赖 PSM 实现；当前 DataupLoad 沿用 PSM
    * 1:1 行为不另行保护（参见 PSM DefectDayRecordPO.java 反编译产物 line 41）。</p>
    *
    * @return 该记录 {@code time} 字段对应的 {@link LocalTime}；若 {@code time} 为 null 则 NPE
    *         （与 PSM 行为一致）
    */
   public LocalTime getLocalTime() {
      return HikDateUtil.transformTime((String) this.time).toLocalTime();
   }

   public Integer getId() {
      return this.id;
   }

   public Integer getCount() {
      return this.count;
   }

   public String getTime() {
      return this.time;
   }

   public String getLineNo() {
      return this.lineNo;
   }

   public String getFaceNo() {
      return this.faceNo;
   }

   public String getType() {
      return this.type;
   }

   public LocalDateTime getUpdateTime() {
      return this.updateTime;
   }

   public LocalDateTime getCreateTime() {
      return this.createTime;
   }

   public DefectDayRecord setId(Integer id) {
      this.id = id;
      return this;
   }

   public DefectDayRecord setCount(Integer count) {
      this.count = count;
      return this;
   }

   public DefectDayRecord setTime(String time) {
      this.time = time;
      return this;
   }

   public DefectDayRecord setLineNo(String lineNo) {
      this.lineNo = lineNo;
      return this;
   }

   public DefectDayRecord setFaceNo(String faceNo) {
      this.faceNo = faceNo;
      return this;
   }

   public DefectDayRecord setType(String type) {
      this.type = type;
      return this;
   }

   public DefectDayRecord setUpdateTime(LocalDateTime updateTime) {
      this.updateTime = updateTime;
      return this;
   }

   public DefectDayRecord setCreateTime(LocalDateTime createTime) {
      this.createTime = createTime;
      return this;
   }
}
