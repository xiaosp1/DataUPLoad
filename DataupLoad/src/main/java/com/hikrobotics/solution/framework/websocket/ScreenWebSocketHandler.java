package com.hikrobotics.solution.framework.websocket;

import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.framework.component.ws.handler.WsActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 大屏 WebSocket 推送处理器（screen 端点）
 * <p>
 * 沿用 PSM 反编译产物：
 * <ul>
 *   <li>框架层 framework-starter-2.2.3 已提供 {@link WebSocketHandler}，session 池 + 广播能力</li>
 *   <li>本类负责把 defect_day_record / line_day_record 实时数据通过 framework 的广播接口推给所有 type=screen 的客户端</li>
 *   <li>沿用 PSM 风格：{@code @EventListener} 监听 {@link WsActionEvent}（连接建立 / 断开）</li>
 * </ul>
 * 端点路径：{@code /ws/screen} —— 由 {@link WebSocketConfig} 注册
 *
 * @since 2026-07-22
 */
@Component
public class ScreenWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ScreenWebSocketHandler.class);

    /** framework-starter 的 WebSocketHandler 单例 —— 复用同一份 session 池 */
    private final WebSocketHandler delegate;

    public ScreenWebSocketHandler(WebSocketHandler delegate) {
        this.delegate = delegate;
        log.info("[SCREEN] ScreenWebSocketHandler 已注入 framework-starter WebSocketHandler");
    }

    /**
     * 监听 WsActionEvent —— 当有 screen 端点客户端连接时，
     * 把当前累积的 defect_day_record / line_day_record 实时数据立即推送过去。
     * <p>
     * PSM 反编译产物中 {@code WsConnectListener.sendAlarmMessage(...)} 用了同样模式。
     */
    @EventListener(WsActionEvent.class)
    public void onWsAction(WsActionEvent event) {
        if (!"screen".equals(event.getClientType())) {
            return;
        }
        if ("connected".equals(event.getAction())) {
            log.info("[SCREEN] client connected, sessionId={}, uid={}",
                    event.getSession().getId(), event.getClientUid());
            // TODO W-B06：连接建立后立即推送全量 defect_day_record / line_day_record
            // 当前先打日志，等 defect_day_record / line_day_record 任务跑起来后挂推送
        } else if ("disconnected".equals(event.getAction())) {
            log.info("[SCREEN] client disconnected, sessionId={}", event.getSession().getId());
        }
    }

    /**
     * 推送大屏数据（对外暴露的便捷方法）
     *
     * @param jsonString 已序列化的 WsMessage JSON
     */
    public void push(String jsonString) {
        delegate.broadcastByType(jsonString, "screen");
    }
}
