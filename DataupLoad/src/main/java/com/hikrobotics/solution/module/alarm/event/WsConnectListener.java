package com.hikrobotics.solution.module.alarm.event;

import com.hikrobotics.solution.framework.component.ws.handler.WsActionEvent;
import com.hikrobotics.solution.module.alarm.service.IAlarmRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WsConnectListener {
    @Autowired
    private IAlarmRecordService alarmRecordService;

    @EventListener(value={WsActionEvent.class})
    public void sendAlarmMessage(WsActionEvent event) {
        if (event.getAction().equals("connected")) {
            this.alarmRecordService.sendAlarmTextMessage();
        }
    }
}
