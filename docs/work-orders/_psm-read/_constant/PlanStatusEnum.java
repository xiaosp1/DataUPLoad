/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.constant.PlanStatusEnum
 */
package com.hikrobotics.solution.module.line.constant;

public enum PlanStatusEnum {
    ENABLE("\u542f\u7528", Integer.valueOf(1)),
    DISABLE("\u672a\u542f\u7528", Integer.valueOf(2));

    private final String status;
    private final Integer value;

    private PlanStatusEnum(String status, Integer value) {
        this.status = status;
        this.value = value;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getValue() {
        return this.value;
    }
}

