/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.DetectDataUploadDTO
 *  com.hikrobotics.solution.module.line.dto.RealTimeDetectData
 *  com.hikrobotics.solution.module.line.dto.TodayDetectDataDTO
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 */
package com.hikrobotics.solution.module.line.dto;

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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DetectDataUploadDTO)) {
            return false;
        }
        DetectDataUploadDTO other = (DetectDataUploadDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$faceNo = this.getFaceNo();
        String other$faceNo = other.getFaceNo();
        if (this$faceNo == null ? other$faceNo != null : !this$faceNo.equals(other$faceNo)) {
            return false;
        }
        String this$lineNo = this.getLineNo();
        String other$lineNo = other.getLineNo();
        if (this$lineNo == null ? other$lineNo != null : !this$lineNo.equals(other$lineNo)) {
            return false;
        }
        TodayDetectDataDTO this$todayData = this.getTodayData();
        TodayDetectDataDTO other$todayData = other.getTodayData();
        if (this$todayData == null ? other$todayData != null : !this$todayData.equals(other$todayData)) {
            return false;
        }
        RealTimeDetectData this$realTimeData = this.getRealTimeData();
        RealTimeDetectData other$realTimeData = other.getRealTimeData();
        return !(this$realTimeData == null ? other$realTimeData != null : !this$realTimeData.equals(other$realTimeData));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DetectDataUploadDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        String $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        TodayDetectDataDTO $todayData = this.getTodayData();
        result = result * 59 + ($todayData == null ? 43 : $todayData.hashCode());
        RealTimeDetectData $realTimeData = this.getRealTimeData();
        result = result * 59 + ($realTimeData == null ? 43 : $realTimeData.hashCode());
        return result;
    }

    public String toString() {
        return "DetectDataUploadDTO(faceNo=" + this.getFaceNo() + ", lineNo=" + this.getLineNo() + ", todayData=" + this.getTodayData() + ", realTimeData=" + this.getRealTimeData() + ")";
    }
}

