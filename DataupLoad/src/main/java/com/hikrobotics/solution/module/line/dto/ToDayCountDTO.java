package com.hikrobotics.solution.module.line.dto;

import java.text.DecimalFormat;

public class ToDayCountDTO {
    private int rightCount;
    private int errorCount;
    private String rightPercentage;
    private String errorPercentage;

    public ToDayCountDTO calPercentage(Integer rightCount, Integer errorCount) {
        float right = rightCount.floatValue();
        float error = errorCount.floatValue();
        DecimalFormat df = new DecimalFormat("0.00");
        this.rightPercentage = df.format(right / (right + error) * 100.0f);
        this.errorPercentage = df.format(error / (right + error) * 100.0f);
        return this;
    }

    public int getRightCount() {
        return this.rightCount;
    }

    public int getErrorCount() {
        return this.errorCount;
    }

    public String getRightPercentage() {
        return this.rightPercentage;
    }

    public String getErrorPercentage() {
        return this.errorPercentage;
    }

    public void setRightCount(int rightCount) {
        this.rightCount = rightCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public void setRightPercentage(String rightPercentage) {
        this.rightPercentage = rightPercentage;
    }

    public void setErrorPercentage(String errorPercentage) {
        this.errorPercentage = errorPercentage;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ToDayCountDTO)) {
            return false;
        }
        ToDayCountDTO other = (ToDayCountDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getRightCount() != other.getRightCount()) {
            return false;
        }
        if (this.getErrorCount() != other.getErrorCount()) {
            return false;
        }
        String this$rightPercentage = this.getRightPercentage();
        String other$rightPercentage = other.getRightPercentage();
        if (this$rightPercentage == null ? other$rightPercentage != null : !this$rightPercentage.equals(other$rightPercentage)) {
            return false;
        }
        String this$errorPercentage = this.getErrorPercentage();
        String other$errorPercentage = other.getErrorPercentage();
        return !(this$errorPercentage == null ? other$errorPercentage != null : !this$errorPercentage.equals(other$errorPercentage));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ToDayCountDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getRightCount();
        result = result * 59 + this.getErrorCount();
        String $rightPercentage = this.getRightPercentage();
        result = result * 59 + ($rightPercentage == null ? 43 : $rightPercentage.hashCode());
        String $errorPercentage = this.getErrorPercentage();
        result = result * 59 + ($errorPercentage == null ? 43 : $errorPercentage.hashCode());
        return result;
    }

    public String toString() {
        return "ToDayCountDTO(rightCount=" + this.getRightCount() + ", errorCount=" + this.getErrorCount() + ", rightPercentage=" + this.getRightPercentage() + ", errorPercentage=" + this.getErrorPercentage() + ")";
    }
}

