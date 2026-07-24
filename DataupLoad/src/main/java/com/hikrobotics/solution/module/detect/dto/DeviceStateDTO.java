package com.hikrobotics.solution.module.detect.dto;

import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import java.time.LocalDateTime;

public class DeviceStateDTO {
    private Integer id;
    private Integer type;
    private Integer status;
    private String deviceNo;
    private String deviceName;
    private LocalDateTime lastConnectTime;

    public DeviceStateDTO() {}

    public DeviceStateDTO(StatusRecord status) {
        this.id = status.getId();
        this.status = status.getStatus();
        this.type = status.getType();
        this.deviceNo = status.getDeviceNo();
        this.deviceName = status.getDeviceName();
        this.lastConnectTime = status.getUpdateTime();
    }

    public Integer getId() { return id; }
    public Integer getType() { return type; }
    public Integer getStatus() { return status; }
    public String getDeviceNo() { return deviceNo; }
    public String getDeviceName() { return deviceName; }
    public LocalDateTime getLastConnectTime() { return lastConnectTime; }

    public void setId(Integer id) { this.id = id; }
    public void setType(Integer type) { this.type = type; }
    public void setStatus(Integer status) { this.status = status; }
    public void setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public void setLastConnectTime(LocalDateTime lastConnectTime) { this.lastConnectTime = lastConnectTime; }
}
