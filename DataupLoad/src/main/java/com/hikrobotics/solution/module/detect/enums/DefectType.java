package com.hikrobotics.solution.module.detect.enums;

public enum DefectType {
    BOTTOM_BREAK("底面破损", 1),
    SIDE_BREAK("侧面破损", 2),
    SIDE_BREAK_BIG("侧面破损Big", 3),
    SIDE_BREAK_SMALL("侧面破损Small", 4),
    SIDE_DIRTY("侧面脏污", 5),
    SECOND_MATERIAL("二次料", 6),
    NOT_DEMOULDED("未脱模", 7);

    private final String type;
    private final Integer value;

    DefectType(String type, Integer value) {
        this.type = type;
        this.value = value;
    }

    public String getType() { return type; }
    public Integer getValue() { return value; }
}
