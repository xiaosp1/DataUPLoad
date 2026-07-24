package com.hikrobotics.solution.module.detect.enums;

public enum DefectResult {
    RIGHT("良品", 1),
    ERROR("次品", 2);

    private final String status;
    private final Integer value;

    DefectResult(String status, Integer value) {
        this.status = status;
        this.value = value;
    }

    public String getStatus() { return status; }
    public Integer getValue() { return value; }
}
