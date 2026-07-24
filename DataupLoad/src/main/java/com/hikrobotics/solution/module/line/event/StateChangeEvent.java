package com.hikrobotics.solution.module.line.event;

import com.hikrobotics.solution.module.detect.enums.DeviceStatus;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEvent;

public class StateChangeEvent
extends ApplicationEvent {
    private String lineNo;
    private String faceNo;
    private DeviceStatus status;
    private LocalDateTime time;

    public StateChangeEvent(Object source) {
        super(source);
    }

    public String getLineNo() {
        return this.lineNo;
    }

    public String getFaceNo() {
        return this.faceNo;
    }

    public DeviceStatus getStatus() {
        return this.status;
    }

    public LocalDateTime getTime() {
        return this.time;
    }

    public StateChangeEvent setLineNo(String lineNo) {
        this.lineNo = lineNo;
        return this;
    }

    public StateChangeEvent setFaceNo(String faceNo) {
        this.faceNo = faceNo;
        return this;
    }

    public StateChangeEvent setStatus(DeviceStatus status) {
        this.status = status;
        return this;
    }

    public StateChangeEvent setTime(LocalDateTime time) {
        this.time = time;
        return this;
    }
}
