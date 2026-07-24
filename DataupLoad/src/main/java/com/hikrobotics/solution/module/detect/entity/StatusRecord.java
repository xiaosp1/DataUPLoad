package com.hikrobotics.solution.module.detect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PG 表 status_record 映射（W-B03）。
 * 1:1 抄自反编译 StatusRecordPO；
 * device_name / line_id 通过 V1.10 / V1.19 迁移加上。
 */
@TableName("status_record")
public class StatusRecord implements Serializable {
   private static final long serialVersionUID = 1L;

   @TableId(value = "id", type = IdType.AUTO)
   private Integer id;

   @TableField("time")
   private String time;

   @TableField("line_id")
   private Integer lineId;

   @TableField("type")
   private Integer type;

   @TableField("line_no")
   private String lineNo;

   @TableField("face_no")
   private String faceNo;

   @TableField("status")
   private Integer status;

   @TableField("device_no")
   private String deviceNo;

   @TableField("device_name")
   private String deviceName;

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
   @TableField("update_time")
   private LocalDateTime updateTime;

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
   @TableField("create_time")
   private LocalDateTime createTime;

   public String getLine() {
      return this.getLineNo() + ":" + this.getFaceNo();
   }

   public Integer getId() { return this.id; }
   public String getTime() { return this.time; }
   public Integer getLineId() { return this.lineId; }
   public Integer getType() { return this.type; }
   public String getLineNo() { return this.lineNo; }
   public String getFaceNo() { return this.faceNo; }
   public Integer getStatus() { return this.status; }
   public String getDeviceNo() { return this.deviceNo; }
   public String getDeviceName() { return this.deviceName; }
   public LocalDateTime getUpdateTime() { return this.updateTime; }
   public LocalDateTime getCreateTime() { return this.createTime; }

   public StatusRecord setId(Integer id) { this.id = id; return this; }
   public StatusRecord setTime(String time) { this.time = time; return this; }
   public StatusRecord setLineId(Integer lineId) { this.lineId = lineId; return this; }
   public StatusRecord setType(Integer type) { this.type = type; return this; }
   public StatusRecord setLineNo(String lineNo) { this.lineNo = lineNo; return this; }
   public StatusRecord setFaceNo(String faceNo) { this.faceNo = faceNo; return this; }
   public StatusRecord setStatus(Integer status) { this.status = status; return this; }
   public StatusRecord setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; return this; }
   public StatusRecord setDeviceName(String deviceName) { this.deviceName = deviceName; return this; }
   public StatusRecord setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; return this; }
   public StatusRecord setCreateTime(LocalDateTime createTime) { this.createTime = createTime; return this; }
}
