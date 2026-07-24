package com.hikrobotics.solution.framework.websocket;

import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.framework.component.ws.handler.WsActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 报警 WebSocket 推送处理器（alarm 端点）
 * <p>
 * 沿用 PSM 反编译产物：
 * <ul>
 *   <li>复用 framework-starter-2.2.3 的 {@link WebSocketHandler} 单例 session 池</li>
 *   <li>{@code @EventListener(WsActionEvent.class)} 模式：监听连接 / 断开事件，沿用 PSM {@code WsConnectListener} 风格</li>
 *   <li>alarm_record 实时数据由 defect / alarm 任务模块 publish {@code PushAlarmEvent}，本处理器订阅后转发</li>
 * </ul>
 * 端点路径：{@code /ws/alarm} —— 由 {@link WebSocketConfig} 注册
 *
 * @author DataupLoad W-B06
 * @since 2026-07-22
 */
@Component
public class AlarmWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AlarmWebSocketHandler.class);

    /** framework-starter 的 WebSocketHandler 单例 —— 复用同一份 session 池 */
    private final WebSocketHandler delegate;

    public AlarmWebSocketHandler(WebSocketHandler delegate) {
        this.delegate = delegate;
        log.info("[ALARM] AlarmWebSocketHandler 已注入 framework-starter WebSocketHandler");
    }

    /**
     * 监听 WsActionEvent —— 当有 alarm 端点客户端连接时，
     * 把当前累积的未处理 alarm_record 立即推送过去（PSM WsConnectListener 风格）。
     */
    @EventListener(WsActionEvent.class)
    public void onWsAction(WsActionEvent event) {
        if (!"alarm".equals(event.getClientType())) {
            return;
        }
        if ("connected".equals(event.getAction())) {
            log.info("[ALARM] client connected, sessionId={}, uid={}",
                    event.getSession().getId(), event.getClientUid());
            // TODO W-B06：连接建立后调用 IAlarmRecordService.sendAlarmTextMessage() 推送历史报警
        } else if ("disconnected".equals(event.getAction())) {
            log.info("[ALARM] client disconnected, sessionId={}", event.getSession().getId());
        }
    }

    /**
     * 推送报警数据（对外暴露的便捷方法）
     *
     * @param jsonString 已序列化的 WsMessage JSON
     */
    public void push(String jsonString) {
        delegate.broadcastByType(jsonString, "alarm");
    }
}
