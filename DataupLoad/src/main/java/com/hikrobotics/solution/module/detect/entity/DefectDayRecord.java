package com.hikrobotics.solution.module.detect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PG 表 defect_day_record 映射。
 * 字段顺序/命名 1:1 抄自反编译 DefectDayRecordPO；去掉了 HikDateUtil 工具方法依赖，
 * 只保留数据库映射和基础 setter/getter。
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
