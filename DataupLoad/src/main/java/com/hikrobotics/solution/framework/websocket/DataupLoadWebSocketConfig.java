package com.hikrobotics.solution.framework.websocket;

import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.framework.component.ws.interceptor.WebSocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * DataupLoad 额外 WebSocket 端点注册
 * <p>
 * 沿用 PSM 反编译产物 + 工单 W-B06 要求：
 * <ul>
 *   <li>framework-starter 的 {@code WebSocketConfig} 已注册 {@code /ws} 单一端点（带 {@code ?type=screen|alarm|sound} 过滤），
 *       这是 PSM 前端 1:1 复制后实际访问的端点</li>
 *   <li>本配置额外注册 {@code /ws/screen} + {@code /ws/alarm} 两个独立路径映射（工单 W-B06 要求），
 *       复用 framework-starter 的同一份 session 池（{@link WebSocketHandler} 单例）</li>
 *   <li>{@link PathTypeHandshakeInterceptor} 先于 framework-starter 的 {@link WebSocketInterceptor} 运行，
 *       根据路径写入 {@code type} 属性；framework-starter 的拦截器再叠加白名单 + sa-token + uid 提取</li>
 * </ul>
 * 两个 configurer 都会被 Spring 的 {@code @EnableWebSocket} 拾取，路径自动合并。
 *
 * <p><b>NOTE</b>：本类名故意命名为 {@code DataupLoadWebSocketConfig}（不是 {@code WebSocketConfig}），
 * 以避免与 framework-starter 的 {@code com.hikrobotics.solution.framework.component.ws.config.WebSocketConfig}
 * 产生 Spring bean name 冲突（两者默认 bean name 都是 {@code webSocketConfig}）。</p>
 *
 * @since 2026-07-22
 */
@Configuration("hiksDataupLoadWebSocketConfig")
@EnableWebSocket
public class DataupLoadWebSocketConfig implements WebSocketConfigurer {

    /** 复用 framework-starter 的同一份 session 池 —— 单例 */
    private final WebSocketHandler wsHandler;

    /** 复用 framework-starter 的握手拦截器（白名单 + sa-token + uid/type 提取） */
    private final WebSocketInterceptor wsInterceptor;

    /** DataupLoad 自己的路径感知拦截器 —— 先跑，把 path 翻译成 type */
    private final PathTypeHandshakeInterceptor pathTypeInterceptor;

    @Autowired
    public DataupLoadWebSocketConfig(WebSocketHandler wsHandler,
                                     WebSocketInterceptor wsInterceptor,
                                     PathTypeHandshakeInterceptor pathTypeInterceptor) {
        this.wsHandler = wsHandler;
        this.wsInterceptor = wsInterceptor;
        this.pathTypeInterceptor = pathTypeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // /ws/screen —— 推 defect_day_record / line_day_record 实时数据
        registry.addHandler(wsHandler, "/ws/screen")
                .addInterceptors(pathTypeInterceptor, wsInterceptor)
                .setAllowedOrigins("*");
        // /ws/alarm —— 推 alarm_record 实时数据
        registry.addHandler(wsHandler, "/ws/alarm")
                .addInterceptors(pathTypeInterceptor, wsInterceptor)
                .setAllowedOrigins("*");
    }
}
