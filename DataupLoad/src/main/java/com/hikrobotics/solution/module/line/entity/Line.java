package com.hikrobotics.solution.module.line.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DataupLoad line 表映射实体（沿用 PSM LinePO 字段）。
 */
@TableName("line")
public class Line implements Serializable {
   private static final long serialVersionUID = 1L;
   @TableId(value = "id", type = IdType.AUTO)
   private Integer id;
   private String name;
   private String lineNo;
   private String faceNo;
   @TableField(updateStrategy = FieldStrategy.IGNORED)
   private String color;
   private String clientNo;
   @TableField(exist = false)
   private Integer order;
   private String realtimeData;
   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   private LocalDateTime updateTime;
   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   private LocalDateTime createTime;

   public String getKey() {
      return this.getLineNo() + ":" + this.getFaceNo();
   }

   public String getPos() {
      return this.lineNo + ":" + this.faceNo;
   }

   public Integer getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public String getLineNo() {
      return this.lineNo;
   }

   public String getFaceNo() {
      return this.faceNo;
   }

   public String getColor() {
      return this.color;
   }

   public String getClientNo() {
      return this.clientNo;
   }

   public Integer getOrder() {
      return this.order;
   }

   public String getRealtimeData() {
      return this.realtimeData;
   }

   public LocalDateTime getUpdateTime() {
      return this.updateTime;
   }

   public LocalDateTime getCreateTime() {
      return this.createTime;
   }

   public Line setId(Integer id) {
      this.id = id;
      return this;
   }

   public Line setName(String name) {
      this.name = name;
      return this;
   }

   public Line setLineNo(String lineNo) {
      this.lineNo = lineNo;
      return this;
   }

   public Line setFaceNo(String faceNo) {
      this.faceNo = faceNo;
      return this;
   }

   public Line setColor(String color) {
      this.color = color;
      return this;
   }

   public Line setClientNo(String clientNo) {
      this.clientNo = clientNo;
      return this;
   }

   public Line setOrder(Integer order) {
      this.order = order;
      return this;
   }

   public Line setRealtimeData(String realtimeData) {
      this.realtimeData = realtimeData;
      return this;
   }

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   public Line setUpdateTime(LocalDateTime updateTime) {
      this.updateTime = updateTime;
      return this;
   }

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   public Line setCreateTime(LocalDateTime createTime) {
      this.createTime = createTime;
      return this;
   }
}
