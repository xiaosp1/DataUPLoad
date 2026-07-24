package com.hikrobotics.solution.module.detect.dto;

import com.hikrobotics.solution.framework.util.HikDateUtil;
import jakarta.validation.constraints.NotBlank;

public class ExportDefectStatisticForm {
   @NotBlank
   private String startTime;
   @NotBlank
   private String endTime;
   private String lineNo;

   public String getStartTime() {
      return this.startTime.substring(0, 10) + " 08:00:00";
   }

   public String getEndTime() {
      String day = HikDateUtil.transformTime(this.endTime).plusDays(1L).toLocalDate().toString();
      return day + " 07:00:00";
   }

   public String getLineNo() {
      return this.lineNo;
   }

   public void setStartTime(String startTime) {
      this.startTime = startTime;
   }

   public void setEndTime(String endTime) {
      this.endTime = endTime;
   }

   public void setLineNo(String lineNo) {
      this.lineNo = lineNo;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ExportDefectStatisticForm other)) {
         return false;
      } else {
         if (!other.canEqual(this)) {
            return false;
         }

         Object this$startTime = this.getStartTime();
         Object other$startTime = other.getStartTime();
         if (this$startTime == null ? other$startTime == null : this$startTime.equals(other$startTime)) {
            Object this$endTime = this.getEndTime();
            Object other$endTime = other.getEndTime();
            if (this$endTime == null ? other$endTime == null : this$endTime.equals(other$endTime)) {
               Object this$lineNo = this.getLineNo();
               Object other$lineNo = other.getLineNo();
               return this$lineNo == null ? other$lineNo == null : this$lineNo.equals(other$lineNo);
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ExportDefectStatisticForm;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $startTime = this.getStartTime();
      result = result * 59 + ($startTime == null ? 43 : $startTime.hashCode());
      Object $endTime = this.getEndTime();
      result = result * 59 + ($endTime == null ? 43 : $endTime.hashCode());
      Object $lineNo = this.getLineNo();
      return result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
   }

   @Override
   public String toString() {
      return "ExportDefectStatisticForm(startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", lineNo=" + this.getLineNo() + ")";
   }
}
