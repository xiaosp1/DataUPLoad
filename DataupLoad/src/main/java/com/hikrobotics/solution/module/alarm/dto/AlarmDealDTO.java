package com.hikrobotics.solution.module.alarm.dto;

import jakarta.validation.constraints.NotEmpty;

public class AlarmDealDTO {
    @NotEmpty
    private String uuid;

    public String getUuid() { return uuid; }
    public AlarmDealDTO setUuid(String uuid) { this.uuid = uuid; return this; }
}
