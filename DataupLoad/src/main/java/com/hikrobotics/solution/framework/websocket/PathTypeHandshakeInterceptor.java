package com.hikrobotics.solution.framework.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 路径感知的 WebSocket 握手拦截器
 * <p>
 * 沿用 framework-starter 的协议：把 URL 路径翻译成 {@code type} session 属性，
 * 让 framework-starter 的 {@code WebSocketHandler.broadcastByType(...)} 能精确路由。
 * <p>
 * 本拦截器与 framework-starter 的 {@code WebSocketInterceptor} 串联：
 * <ol>
 *   <li>本拦截器先跑（按 Spring 拦截器顺序），把 path 前缀写入 {@code attributes["type"]}</li>
 *   <li>framework-starter 的拦截器后跑，把 query 中的 {@code type} 覆盖上去（如果客户端传了）</li>
 *   <li>这样既支持 PSM 风格的 {@code /ws?type=screen} 单端点，也支持 W-B06 要求的 {@code /ws/screen} 路径风格</li>
 * </ol>
 *
 * @author DataupLoad W-B06
 * @since 2026-07-22
 */
@Component
public class PathTypeHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PathTypeHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String path = request.getURI().getPath();
        String type = null;
        if (path.startsWith("/ws/screen")) {
            type = "screen";
        } else if (path.startsWith("/ws/alarm")) {
            type = "alarm";
        } else if (path.startsWith("/ws/sound")) {
            type = "sound";
        }
        if (type != null) {
            attributes.put("type", type);
            log.debug("[PathType] {} -> type={}", path, type);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
