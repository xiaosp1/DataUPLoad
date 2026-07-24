package com.hikrobotics.solution.module.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ignore_alarm 表映射实体（对齐 PSM IgnoreAlarmPO 字段）。
 *
 * <p>PSM 表定义 V1.14：
 * <pre>
 * CREATE TABLE ignore_alarm (
 *   id serial primary key,
 *   defect_name varchar(128) not null,
 *   type int not null,
 *   line_no varchar(20) not null,
 *   face_no varchar(20) not null,
 *   ignore_time timestamp not null,
 *   update_time timestamp not null default current_timestamp,
 *   create_time timestamp not null default current_timestamp
 * );
 * </pre>
 */
@TableName("ignore_alarm")
public class IgnoreAlarm implements Serializable {
   private static final long serialVersionUID = 1L;

   @TableId(value = "id", type = IdType.AUTO)
   private Integer id;

   @TableField("defect_name")
   private String defectName;

   private Integer type;

   @TableField("line_no")
   private String lineNo;

   @TableField("face_no")
   private String faceNo;

   @TableField("ignore_time")
   private LocalDateTime ignoreTime;

   @TableField("update_time")
   private LocalDateTime updateTime;

   @TableField("create_time")
   private LocalDateTime createTime;

   // ===== helpers =====

   public String getKey() {
      return this.lineNo + ":" + this.faceNo + ":" + this.defectName;
   }

   // ===== getters =====

   public Integer getId() { return id; }
   public String getDefectName() { return defectName; }
   public Integer getType() { return type; }
   public String getLineNo() { return lineNo; }
   public String getFaceNo() { return faceNo; }
   public LocalDateTime getIgnoreTime() { return ignoreTime; }
   public LocalDateTime getUpdateTime() { return updateTime; }
   public LocalDateTime getCreateTime() { return createTime; }

   // ===== fluent setters =====

   public IgnoreAlarm setId(Integer id) { this.id = id; return this; }
   public IgnoreAlarm setDefectName(String defectName) { this.defectName = defectName; return this; }
   public IgnoreAlarm setType(Integer type) { this.type = type; return this; }
   public IgnoreAlarm setLineNo(String lineNo) { this.lineNo = lineNo; return this; }
   public IgnoreAlarm setFaceNo(String faceNo) { this.faceNo = faceNo; return this; }
   public IgnoreAlarm setIgnoreTime(LocalDateTime ignoreTime) { this.ignoreTime = ignoreTime; return this; }
   public IgnoreAlarm setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; return this; }
   public IgnoreAlarm setCreateTime(LocalDateTime createTime) { this.createTime = createTime; return this; }

   // ===== string setter for JSON input =====

   /** 按 "yyyy-MM-dd HH:mm:ss" 解析字符串为 ignoreTime */
   public IgnoreAlarm setIgnoreTimeByString(String s) {
      if (s != null && !s.isEmpty()) {
         this.ignoreTime = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      }
      return this;
   }
}
