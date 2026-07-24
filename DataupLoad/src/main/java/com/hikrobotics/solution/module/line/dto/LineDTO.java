/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.LineDTO
 */
package com.hikrobotics.solution.module.line.dto;

public class LineDTO {
    private Integer id;
    private String name;
    private String lineNo;
    private String faceNo;
    private String color;
    private String clientNo;
    private Integer planId;
    private String planName;

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

    public Integer getPlanId() {
        return this.planId;
    }

    public String getPlanName() {
        return this.planName;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLineNo(String lineNo) {
        this.lineNo = lineNo;
    }

    public void setFaceNo(String faceNo) {
        this.faceNo = faceNo;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setClientNo(String clientNo) {
        this.clientNo = clientNo;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LineDTO)) {
            return false;
        }
        LineDTO other = (LineDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$id = this.getId();
        Integer other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$planId = this.getPlanId();
        Integer other$planId = other.getPlanId();
        if (this$planId == null ? other$planId != null : !((Object)this$planId).equals(other$planId)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$lineNo = this.getLineNo();
        String other$lineNo = other.getLineNo();
        if (this$lineNo == null ? other$lineNo != null : !this$lineNo.equals(other$lineNo)) {
            return false;
        }
        String this$faceNo = this.getFaceNo();
        String other$faceNo = other.getFaceNo();
        if (this$faceNo == null ? other$faceNo != null : !this$faceNo.equals(other$faceNo)) {
            return false;
        }
        String this$color = this.getColor();
        String other$color = other.getColor();
        if (this$color == null ? other$color != null : !this$color.equals(other$color)) {
            return false;
        }
        String this$clientNo = this.getClientNo();
        String other$clientNo = other.getClientNo();
        if (this$clientNo == null ? other$clientNo != null : !this$clientNo.equals(other$clientNo)) {
            return false;
        }
        String this$planName = this.getPlanName();
        String other$planName = other.getPlanName();
        return !(this$planName == null ? other$planName != null : !this$planName.equals(other$planName));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LineDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $planId = this.getPlanId();
        result = result * 59 + ($planId == null ? 43 : ((Object)$planId).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        String $faceNo = this.getFaceNo();
        result = result * 59 + ($faceNo == null ? 43 : $faceNo.hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        String $clientNo = this.getClientNo();
        result = result * 59 + ($clientNo == null ? 43 : $clientNo.hashCode());
        String $planName = this.getPlanName();
        result = result * 59 + ($planName == null ? 43 : $planName.hashCode());
        return result;
    }

    public String toString() {
        return "LineDTO(id=" + this.getId() + ", name=" + this.getName() + ", lineNo=" + this.getLineNo() + ", faceNo=" + this.getFaceNo() + ", color=" + this.getColor() + ", clientNo=" + this.getClientNo() + ", planId=" + this.getPlanId() + ", planName=" + this.getPlanName() + ")";
    }
}

