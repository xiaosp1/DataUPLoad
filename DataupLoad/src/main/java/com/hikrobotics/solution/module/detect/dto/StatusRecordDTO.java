package com.hikrobotics.solution.module.detect.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import com.hikrobotics.solution.module.line.model.LinePO;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单 W-B03：Detect 模块入参 DTO（/client/data/status）。
 * 反编译产物里 DetectDataController 实际用的是 List<StatusRecordPO>，但工单要求以 DTO 入参；
 * 字段与 StatusRecordPO 完全一致，便于在 service 里直接 copy 成 PO 写入数据库。
 */
public class StatusRecordDTO implements Serializable {
   private static final long serialVersionUID = 1L;

   private Integer id;

   @NotEmpty
   private String time;

   private Integer lineId;

   @NotNull
   private Integer type;

   @NotEmpty
   private String lineNo;

   @NotEmpty
   private String faceNo;

   @NotNull
   private Integer status;

   @NotEmpty
   private String deviceNo;

   private String deviceName;

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
   private LocalDateTime updateTime;

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
   private LocalDateTime createTime;

   public StatusRecordDTO buildClient(LinePO line, String deviceNo) {
      StatusRecordDTO record = new StatusRecordDTO();
      record.setTime(java.time.LocalDateTime.now().toString());
      record.setType(com.hikrobotics.solution.module.detect.enums.DeviceType.CLIENT.getValue());
      record.setLineNo(line.getLineNo());
      record.setDeviceNo(deviceNo);
      record.setStatus(com.hikrobotics.solution.module.detect.enums.DeviceStatus.ONLINE.getValue());
      record.setFaceNo(line.getFaceNo());
      record.setLineId(line.getId());
      return record;
   }

   public String getLine() {
      return this.getLineNo() + ":" + this.getFaceNo();
   }

   public Integer getId() {
      return this.id;
   }

   public String getTime() {
      return this.time;
   }

   public Integer getLineId() {
      return this.lineId;
   }

   public Integer getType() {
      return this.type;
   }

   public String getLineNo() {
      return this.lineNo;
   }

   public String getFaceNo() {
      return this.faceNo;
   }

   public Integer getStatus() {
      return this.status;
   }

   public String getDeviceNo() {
      return this.deviceNo;
   }

   public String getDeviceName() {
      return this.deviceName;
   }

   public LocalDateTime getUpdateTime() {
      return this.updateTime;
   }

   public LocalDateTime getCreateTime() {
      return this.createTime;
   }

   public StatusRecordDTO setId(Integer id) {
      this.id = id;
      return this;
   }

   public StatusRecordDTO setTime(String time) {
      this.time = time;
      return this;
   }

   public StatusRecordDTO setLineId(Integer lineId) {
      this.lineId = lineId;
      return this;
   }

   public StatusRecordDTO setType(Integer type) {
      this.type = type;
      return this;
   }

   public StatusRecordDTO setLineNo(String lineNo) {
      this.lineNo = lineNo;
      return this;
   }

   public StatusRecordDTO setFaceNo(String faceNo) {
      this.faceNo = faceNo;
      return this;
   }

   public StatusRecordDTO setStatus(Integer status) {
      this.status = status;
      return this;
   }

   public StatusRecordDTO setDeviceNo(String deviceNo) {
      this.deviceNo = deviceNo;
      return this;
   }

   public StatusRecordDTO setDeviceName(String deviceName) {
      this.deviceName = deviceName;
      return this;
   }

   public StatusRecordDTO setUpdateTime(LocalDateTime updateTime) {
      this.updateTime = updateTime;
      return this;
   }

   public StatusRecordDTO setCreateTime(LocalDateTime createTime) {
      this.createTime = createTime;
      return this;
   }

   /** 复制成本项目 StatusRecord 实体。 */
   public StatusRecord toEntity() {
      return new StatusRecord()
         .setId(this.id)
         .setTime(this.time)
         .setLineId(this.lineId)
         .setType(this.type)
         .setLineNo(this.lineNo)
         .setFaceNo(this.faceNo)
         .setStatus(this.status)
         .setDeviceNo(this.deviceNo)
         .setDeviceName(this.deviceName)
         .setUpdateTime(this.updateTime)
         .setCreateTime(this.createTime);
   }
}
