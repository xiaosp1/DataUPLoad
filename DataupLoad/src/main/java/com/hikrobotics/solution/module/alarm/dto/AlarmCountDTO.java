package com.hikrobotics.solution.module.alarm.dto;

/**
 * PSM 1:1 AlarmCountDTO — 报警按级别/类型计数。
 */
public class AlarmCountDTO {
    private Integer count;
    private String countTime;
    private Integer level;
    private Integer type;

    public Integer getCount() { return count; }
    public AlarmCountDTO setCount(Integer count) { this.count = count; return this; }

    public String getCountTime() { return countTime; }
    public AlarmCountDTO setCountTime(String countTime) { this.countTime = countTime; return this; }

    public Integer getLevel() { return level; }
    public AlarmCountDTO setLevel(Integer level) { this.level = level; return this; }

    public Integer getType() { return type; }
    public AlarmCountDTO setType(Integer type) { this.type = type; return this; }
}
