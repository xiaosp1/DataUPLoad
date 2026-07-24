package com.hikrobotics.solution.module.alarm.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

public class AlarmDTO {
   @NotEmpty
   private String uuid;
   @NotEmpty
   private String time;
   @Range(min = 1L, max = 3L)
   private Integer type;
   @NotEmpty
   private String lineNo;
   @NotEmpty
   private String faceNo;
   @NotNull
   private Integer level;
   @NotEmpty
   private String message;

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

   public AlarmDTO setUuid(String uuid) {
      this.uuid = uuid;
      return this;
   }

   public AlarmDTO setTime(String time) {
      this.time = time;
      return this;
   }

   public AlarmDTO setType(Integer type) {
      this.type = type;
      return this;
   }

   public AlarmDTO setLineNo(String lineNo) {
      this.lineNo = lineNo;
      return this;
   }

   public AlarmDTO setFaceNo(String faceNo) {
      this.faceNo = faceNo;
      return this;
   }

   public AlarmDTO setLevel(Integer level) {
      this.level = level;
      return this;
   }

   public AlarmDTO setMessage(String message) {
      this.message = message;
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof AlarmDTO other)) {
         return false;
      } else {
         if (!other.canEqual(this)) {
            return false;
         }

         Object this$type = this.getType();
         Object other$type = other.getType();
         if (this$type == null ? other$type == null : this$type.equals(other$type)) {
            Object this$level = this.getLevel();
            Object other$level = other.getLevel();
            if (this$level == null ? other$level == null : this$level.equals(other$level)) {
               Object this$uuid = this.getUuid();
               Object other$uuid = other.getUuid();
               if (this$uuid == null ? other$uuid == null : this$uuid.equals(other$uuid)) {
                  Object this$time = this.getTime();
                  Object other$time = other.getTime();
                  if (this$time == null ? other$time == null : this$time.equals(other$time)) {
                     Object this$lineNo = this.getLineNo();
                     Object other$lineNo = other.getLineNo();
                     if (this$lineNo == null ? other$lineNo == null : this$lineNo.equals(other$lineNo)) {
                        Object this$faceNo = this.getFaceNo();
                        Object other$faceNo = other.getFaceNo();
                        if (this$faceNo == null ? other$faceNo == null : this$faceNo.equals(other$faceNo)) {
                           Object this$message = this.getMessage();
                           Object other$message = other.getMessage();
                           return this$message == null ? other$message == null : this$message.equals(other$message);
                        } else {
                           return false;
                        }
                     } else {
                        return false;
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof AlarmDTO;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $type = this.getType();
      result = result * 59 + ($type == null ? 43 : $type.hashCode());
      Object $level = this.getLevel();
      result = result * 59 + ($level == null ? 43 : $level.hashCode());
      Object $uuid = this.getUuid();
      result = result * 59 + ($uuid == null ? 43 : $uuid.hashCode());
      Object $time = this.getTime();
      result = result * 59 + ($time == null ? 43 : $time.hashCode());
      Object $lineNo = this.getLineNo();
      result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
      Object $faceNo = this.getFaceNo();
      result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
      Object $message = this.getMessage();
      return result * 59 + ($message == null ? 43 : $message.hashCode());
   }

   @Override
   public String toString() {
      return "AlarmDTO(uuid="
         + this.getUuid()
         + ", time="
         + this.getTime()
         + ", type="
         + this.getType()
         + ", lineNo="
         + this.getLineNo()
         + ", faceNo="
         + this.getFaceNo()
         + ", level="
         + this.getLevel()
         + ", message="
         + this.getMessage()
         + ")";
   }
}
