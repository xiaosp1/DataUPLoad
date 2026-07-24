package com.hikrobotics.solution.module.detect.dto;

import com.hikrobotics.solution.module.line.dto.RealTimeDetectData;
import com.hikrobotics.solution.module.line.dto.TodayDetectDataDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DetectDataUploadDTO {
   @NotBlank
   private String faceNo;
   @NotBlank
   private String lineNo;
   @NotNull
   private TodayDetectDataDTO todayData;
   @NotNull
   private RealTimeDetectData realTimeData;

   public String getFaceNo() {
      return this.faceNo;
   }

   public String getLineNo() {
      return this.lineNo;
   }

   public TodayDetectDataDTO getTodayData() {
      return this.todayData;
   }

   public RealTimeDetectData getRealTimeData() {
      return this.realTimeData;
   }

   public void setFaceNo(String faceNo) {
      this.faceNo = faceNo;
   }

   public void setLineNo(String lineNo) {
      this.lineNo = lineNo;
   }

   public void setTodayData(TodayDetectDataDTO todayData) {
      this.todayData = todayData;
   }

   public void setRealTimeData(RealTimeDetectData realTimeData) {
      this.realTimeData = realTimeData;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof DetectDataUploadDTO other)) {
         return false;
      } else {
         if (!other.canEqual(this)) {
            return false;
         }

         Object this$faceNo = this.getFaceNo();
         Object other$faceNo = other.getFaceNo();
         if (this$faceNo == null ? other$faceNo == null : this$faceNo.equals(other$faceNo)) {
            Object this$lineNo = this.getLineNo();
            Object other$lineNo = other.getLineNo();
            if (this$lineNo == null ? other$lineNo == null : this$lineNo.equals(other$lineNo)) {
               Object this$todayData = this.getTodayData();
               Object other$todayData = other.getTodayData();
               if (this$todayData == null ? other$todayData == null : this$todayData.equals(other$todayData)) {
                  Object this$realTimeData = this.getRealTimeData();
                  Object other$realTimeData = other.getRealTimeData();
                  return this$realTimeData == null ? other$realTimeData == null : this$realTimeData.equals(other$realTimeData);
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
      return other instanceof DetectDataUploadDTO;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $faceNo = this.getFaceNo();
      result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
      Object $lineNo = this.getLineNo();
      result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
      Object $todayData = this.getTodayData();
      result = result * 59 + ($todayData == null ? 43 : $todayData.hashCode());
      Object $realTimeData = this.getRealTimeData();
      return result * 59 + ($realTimeData == null ? 43 : $realTimeData.hashCode());
   }

   @Override
   public String toString() {
      return "DetectDataUploadDTO(faceNo="
         + this.getFaceNo()
         + ", lineNo="
         + this.getLineNo()
         + ", todayData="
         + this.getTodayData()
         + ", realTimeData="
         + this.getRealTimeData()
         + ")";
   }
}
