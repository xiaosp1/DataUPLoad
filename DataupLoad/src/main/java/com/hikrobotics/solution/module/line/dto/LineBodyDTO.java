/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.LineBodyDTO
 *  jakarta.validation.constraints.NotEmpty
 */
package com.hikrobotics.solution.module.line.dto;

import jakarta.validation.constraints.NotEmpty;

public class LineBodyDTO {
    @NotEmpty
    private String name;
    @NotEmpty
    private String lineNo;
    @NotEmpty
    private String faceNo;
    private String color;
    private String clientNo;

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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LineBodyDTO)) {
            return false;
        }
        LineBodyDTO other = (LineBodyDTO)o;
        if (!other.canEqual((Object)this)) {
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
        return !(this$clientNo == null ? other$clientNo != null : !this$clientNo.equals(other$clientNo));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LineBodyDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
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
        return result;
    }

    public String toString() {
        return "LineBodyDTO(name=" + this.getName() + ", lineNo=" + this.getLineNo() + ", faceNo=" + this.getFaceNo() + ", color=" + this.getColor() + ", clientNo=" + this.getClientNo() + ")";
    }
}

