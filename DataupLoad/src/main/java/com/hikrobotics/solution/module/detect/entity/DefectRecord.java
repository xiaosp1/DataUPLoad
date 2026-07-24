package com.hikrobotics.solution.module.detect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * PG 表 defect_record 映射。
 * 1:1 抄自反编译 DefectRecordPO。
 * img_list 使用 jsonb 列，本类用 String 简化（不引入 hutool JSONArray）。
 * except_flag 通过 V1.3 迁移加上。
 */
@TableName("defect_record")
public class DefectRecord implements Serializable {
   private static final long serialVersionUID = 1L;

   @TableId(value = "id", type = IdType.AUTO)
   private Integer id;

   @TableField("line_no")
   private String lineNo;

   @TableField("face_no")
   private String faceNo;

   @TableField("glove_no")
   private String gloveNo;

   @TableField("result")
   private Integer result;

   @TableField("defect_type")
   private String defectType;

   @TableField("except_flag")
   private Integer exceptFlag;

   @TableField("img_list")
   private String imgList;

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
   @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   @TableField("time")
   private LocalDateTime time;

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
   @TableField("update_time")
   private LocalDateTime updateTime;

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
   @TableField("create_time")
   private LocalDateTime createTime;

   public String getPos() {
      return this.lineNo + ":" + this.faceNo;
   }

   public Integer getId() {
      return this.id;
   }

   public String getLineNo() {
      return this.lineNo;
   }

   public String getFaceNo() {
      return this.faceNo;
   }

   public String getGloveNo() {
      return this.gloveNo;
   }

   public Integer getResult() {
      return this.result;
   }

   public String getDefectType() {
      return this.defectType;
   }

   public Integer getExceptFlag() {
      return this.exceptFlag;
   }

   public String getImgList() {
      return this.imgList;
   }

   public LocalDateTime getTime() {
      return this.time;
   }

   public LocalDateTime getUpdateTime() {
      return this.updateTime;
   }

   public LocalDateTime getCreateTime() {
      return this.createTime;
   }

   public DefectRecord setId(Integer id) {
      this.id = id;
      return this;
   }

   public DefectRecord setLineNo(String lineNo) {
      this.lineNo = lineNo;
      return this;
   }

   public DefectRecord setFaceNo(String faceNo) {
      this.faceNo = faceNo;
      return this;
   }

   public DefectRecord setGloveNo(String gloveNo) {
      this.gloveNo = gloveNo;
      return this;
   }

   public DefectRecord setResult(Integer result) {
      this.result = result;
      return this;
   }

   public DefectRecord setDefectType(String defectType) {
      this.defectType = defectType;
      return this;
   }

   public DefectRecord setExceptFlag(Integer exceptFlag) {
      this.exceptFlag = exceptFlag;
      return this;
   }

   public DefectRecord setImgList(String imgList) {
      this.imgList = imgList;
      return this;
   }

   public DefectRecord setTime(LocalDateTime time) {
      this.time = time;
      return this;
   }

   public DefectRecord setUpdateTime(LocalDateTime updateTime) {
      this.updateTime = updateTime;
      return this;
   }

   public DefectRecord setCreateTime(LocalDateTime createTime) {
      this.createTime = createTime;
      return this;
   }
}
