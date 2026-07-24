/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.screen.dto.ClientStatusDTO
 */
package com.hikrobotics.solution.module.screen.dto;

public class ClientStatusDTO {
    private String lineNo;
    private String faceNo;
    private Integer order;
    private Integer lineId;
    private Integer cameraStatus;
    private Integer eliminatorStatus;
    private Integer clientStatus;

    public String getLineNo() {
        return this.lineNo;
    }

    public String getFaceNo() {
        return this.faceNo;
    }

    public Integer getOrder() {
        return this.order;
    }

    public Integer getLineId() {
        return this.lineId;
    }

    public Integer getCameraStatus() {
        return this.cameraStatus;
    }

    public Integer getEliminatorStatus() {
        return this.eliminatorStatus;
    }

    public Integer getClientStatus() {
        return this.clientStatus;
    }

    public void setLineNo(String lineNo) {
        this.lineNo = lineNo;
    }

    public void setFaceNo(String faceNo) {
        this.faceNo = faceNo;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public void setCameraStatus(Integer cameraStatus) {
        this.cameraStatus = cameraStatus;
    }

    public void setEliminatorStatus(Integer eliminatorStatus) {
        this.eliminatorStatus = eliminatorStatus;
    }

    public void setClientStatus(Integer clientStatus) {
        this.clientStatus = clientStatus;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ClientStatusDTO)) {
            return false;
        }
        ClientStatusDTO other = (ClientStatusDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$order = this.getOrder();
        Integer other$order = other.getOrder();
        if (this$order == null ? other$order != null : !((Object)this$order).equals(other$order)) {
            return false;
        }
        Integer this$lineId = this.getLineId();
        Integer other$lineId = other.getLineId();
        if (this$lineId == null ? other$lineId != null : !((Object)this$lineId).equals(other$lineId)) {
            return false;
        }
        Integer this$cameraStatus = this.getCameraStatus();
        Integer other$cameraStatus = other.getCameraStatus();
        if (this$cameraStatus == null ? other$cameraStatus != null : !((Object)this$cameraStatus).equals(other$cameraStatus)) {
            return false;
        }
        Integer this$eliminatorStatus = this.getEliminatorStatus();
        Integer other$eliminatorStatus = other.getEliminatorStatus();
        if (this$eliminatorStatus == null ? other$eliminatorStatus != null : !((Object)this$eliminatorStatus).equals(other$eliminatorStatus)) {
            return false;
        }
        Integer this$clientStatus = this.getClientStatus();
        Integer other$clientStatus = other.getClientStatus();
        if (this$clientStatus == null ? other$clientStatus != null : !((Object)this$clientStatus).equals(other$clientStatus)) {
            return false;
        }
        String this$lineNo = this.getLineNo();
        String other$lineNo = other.getLineNo();
        if (this$lineNo == null ? other$lineNo != null : !this$lineNo.equals(other$lineNo)) {
            return false;
        }
        String this$faceNo = this.getFaceNo();
        String other$faceNo = other.getFaceNo();
        return !(this$faceNo == null ? other$faceNo != null : !this$faceNo.equals(other$faceNo));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ClientStatusDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $order = this.getOrder();
        result = result * 59 + ($order == null ? 43 : ((Object)$order).hashCode());
        Integer $lineId = this.getLineId();
        result = result * 59 + ($lineId == null ? 43 : ((Object)$lineId).hashCode());
        Integer $cameraStatus = this.getCameraStatus();
        result = result * 59 + ($cameraStatus == null ? 43 : ((Object)$cameraStatus).hashCode());
        Integer $eliminatorStatus = this.getEliminatorStatus();
        result = result * 59 + ($eliminatorStatus == null ? 43 : ((Object)$eliminatorStatus).hashCode());
        Integer $clientStatus = this.getClientStatus();
        result = result * 59 + ($clientStatus == null ? 43 : ((Object)$clientStatus).hashCode());
        String $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        String $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        return result;
    }

    public String toString() {
        return "ClientStatusDTO(lineNo=" + this.getLineNo() + ", faceNo=" + this.getFaceNo() + ", order=" + this.getOrder() + ", lineId=" + this.getLineId() + ", cameraStatus=" + this.getCameraStatus() + ", eliminatorStatus=" + this.getEliminatorStatus() + ", clientStatus=" + this.getClientStatus() + ")";
    }
}
