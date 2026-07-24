package com.hikrobotics.solution.module.line.constant;

public enum PlanStatusEnum {
    ENABLE("启用", 1),
    DISABLE("未启用", 2);

    private final String status;
    private final Integer value;

    PlanStatusEnum(String status, Integer value) {
        this.status = status;
        this.value = value;
    }

    public String getStatus() { return status; }
    public Integer getValue() { return value; }
}
