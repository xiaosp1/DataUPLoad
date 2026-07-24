package com.hikrobotics.solution.module.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.alarm.constant.AlarmLevelEnum;
import com.hikrobotics.solution.module.alarm.constant.AlarmReasonEnum;
import com.hikrobotics.solution.module.alarm.constant.AlarmSolvedEnum;
import com.hikrobotics.solution.module.detect.enums.DeviceType;
import com.hikrobotics.solution.module.alarm.model.DefectType;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DataupLoad alarm_record 表映射实体。
 * <p>
 * 字段命名沿用反编译产物（PSM AlarmRecordPO）；实体名改为 AlarmRecord 以避免和 MyBatis-Plus
 * 的 PO 类名约定冲突（详见 mapper 子包）。
 * <p>
 * 注：{@code defectType} 与 {@code count} 字段在表中并不存在，使用 {@link TableField#exist()}
 * 显式标记为非持久化字段。
 */
@TableName("alarm_record")
public class AlarmRecord implements Serializable {
   private static final long serialVersionUID = 1L;
   @TableId(value = "id", type = IdType.AUTO)
   private Integer id;
   private String uuid;
   private String time;
   private Integer type;
   private String lineNo;
   private String faceNo;
   private Integer level;
   private String message;
   private Integer solve;
   private Integer reason;
   private String defectName;
   @TableField(exist = false)
   private DefectType defectType;
   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   private LocalDateTime updateTime;
   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   private LocalDateTime createTime;
   @TableField(exist = false)
   private int count;

   public AlarmRecord buildClientAlarm(String lineNo, String faceNo) {
      this.uuid = String.valueOf(System.currentTimeMillis());
      this.time = HikDateUtil.getCurrentTime();
      this.type = DeviceType.CLIENT.getValue();
      this.lineNo = lineNo;
      this.faceNo = faceNo;
      this.defectName = "客户端";
      this.level = AlarmLevelEnum.HIGH.getValue();
      this.message = lineNo + "-" + faceNo + "客户端掉线";
      this.reason = AlarmReasonEnum.DISCONNECT.getValue();
      this.solve = AlarmSolvedEnum.UNSOLVED.getValue();
      return this;
   }

   public String getLine() {
      return this.getLineNo() + ":" + this.getFaceNo();
   }

   public String getKey() {
      return this.lineNo + ":" + this.faceNo + ":" + this.defectName;
   }

   public Integer getId() {
      return this.id;
   }

   public String getUuid() {
      return this.uuid;
   }

   public String getTime() {
      return this.time;
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

   public Integer getLevel() {
      return this.level;
   }

   public String getMessage() {
      return this.message;
   }

   public Integer getSolve() {
      return this.solve;
   }

   public Integer getReason() {
      return this.reason;
   }

   public String getDefectName() {
      return this.defectName;
   }

   public DefectType getDefectType() {
      return this.defectType;
   }

   public LocalDateTime getUpdateTime() {
      return this.updateTime;
   }

   public LocalDateTime getCreateTime() {
      return this.createTime;
   }

   public int getCount() {
      return this.count;
   }

   public AlarmRecord setId(Integer id) {
      this.id = id;
      return this;
   }

   public AlarmRecord setUuid(String uuid) {
      this.uuid = uuid;
      return this;
   }

   public AlarmRecord setTime(String time) {
      this.time = time;
      return this;
   }

   public AlarmRecord setType(Integer type) {
      this.type = type;
      return this;
   }

   public AlarmRecord setLineNo(String lineNo) {
      this.lineNo = lineNo;
      return this;
   }

   public AlarmRecord setFaceNo(String faceNo) {
      this.faceNo = faceNo;
      return this;
   }

   public AlarmRecord setLevel(Integer level) {
      this.level = level;
      return this;
   }

   public AlarmRecord setMessage(String message) {
      this.message = message;
      return this;
   }

   public AlarmRecord setSolve(Integer solve) {
      this.solve = solve;
      return this;
   }

   public AlarmRecord setReason(Integer reason) {
      this.reason = reason;
      return this;
   }

   public AlarmRecord setDefectName(String defectName) {
      this.defectName = defectName;
      return this;
   }

   public AlarmRecord setDefectType(DefectType defectType) {
      this.defectType = defectType;
      return this;
   }

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   public AlarmRecord setUpdateTime(LocalDateTime updateTime) {
      this.updateTime = updateTime;
      return this;
   }

   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   public AlarmRecord setCreateTime(LocalDateTime createTime) {
      this.createTime = createTime;
      return this;
   }

   public AlarmRecord setCount(int count) {
      this.count = count;
      return this;
   }
}
