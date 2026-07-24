package com.hikrobotics.solution.framework.websocket;

import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.framework.component.ws.model.WsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 调试 / 测试 Controller（沿用 PSM 风格 hik-security 白名单，仅 /debug/** 开放）
 * <p>
 * 用于验证 WebSocket 推送链路 + 给前端联调用：
 * <ul>
 *   <li>{@code GET /debug/ws/push/screen?msg=...} 主动向所有 type=screen 客户端推送测试消息</li>
 *   <li>{@code GET /debug/ws/push/alarm?msg=...} 主动向所有 type=alarm 客户端推送测试消息</li>
 *   <li>{@code GET /debug/ws/sessions} 查看当前所有连接的 session 信息</li>
 * </ul>
 *
 * @since 2026-07-22
 */
@RestController
@RequestMapping("/debug/ws")
public class WebSocketDebugController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketDebugController.class);

    private final WebSocketHandler wsHandler;

    public WebSocketDebugController(WebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    @GetMapping("/push/screen")
    public Map<String, Object> pushScreen(@RequestParam(defaultValue = "hello from screen") String msg) {
        WsMessage payload = WsMessage.build()
                .type("screen")
                .data(buildScreenData(msg), "data");
        wsHandler.broadcastByType(payload.toJsonString(), "screen");
        log.info("[SCREEN-DEBUG] pushed: {}", payload.toJsonString());
        Map<String, Object> resp = new HashMap<>();
        resp.put("pushed", payload.toJsonString());
        resp.put("sessions", wsHandler.getClientSession().size());
        return resp;
    }

    @GetMapping("/push/alarm")
    public Map<String, Object> pushAlarm(@RequestParam(defaultValue = "test alarm from debug") String msg) {
        WsMessage payload = WsMessage.build()
                .type("alarm")
                .data(buildAlarmData(msg), "data");
        wsHandler.broadcastByType(payload.toJsonString(), "alarm");
        log.info("[ALARM-DEBUG] pushed: {}", payload.toJsonString());
        Map<String, Object> resp = new HashMap<>();
        resp.put("pushed", payload.toJsonString());
        resp.put("sessions", wsHandler.getClientSession().size());
        return resp;
    }

    @GetMapping("/sessions")
    public Map<String, Object> sessions() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("total", wsHandler.getClientSession().size());
        resp.put("types", wsHandler.getClientSession().stream()
                .map(s -> s.getAttributes().get("type"))
                .toList());
        return resp;
    }

    private static Map<String, Object> buildScreenData(String msg) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "screen");
        data.put("ts", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("msg", msg);
        data.put("source", "DataupLoad-debug");
        return data;
    }

    private static Map<String, Object> buildAlarmData(String msg) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "alarm");
        data.put("ts", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("msg", msg);
        data.put("level", "info");
        data.put("source", "DataupLoad-debug");
        return data;
    }
}
