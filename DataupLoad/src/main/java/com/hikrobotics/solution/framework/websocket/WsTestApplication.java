package com.hikrobotics.solution.framework.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * W-B06 临时最小化启动类 —— 仅用于验证 WebSocket + 大屏前端 1:1 复制能跑通
 * <p>
 * 设计思路：
 * <ul>
 *   <li>只扫描 <code>com.hikrobotics.solution.framework.websocket</code>（我们自己写的）</li>
 *   <li>只扫描 <code>com.hikrobotics.solution.framework.component.ws</code>（framework-starter 提供的 WebSocket 内核，
 *       仅含 {@code WebSocketConfig}, {@code WebSocketHandler}, {@code WebSocketInterceptor}, {@code WsKeepAliveTask}）</li>
 *   <li>排除 {@code component.auth}、{@code account}、{@code appaccount} 等强依赖 DB 的子包</li>
 *   <li>禁用 {@code DataSourceAutoConfiguration}、{@code FlywayAutoConfiguration}、{@code HibernateJpaAutoConfiguration}、
 *       {@code DynamicDataSourceAutoConfiguration} 等 DB 自动装配</li>
 * </ul>
 * 验证完成后删除本类，恢复 {@code com.hikrobotics.solution.Application} 启动。
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.hikrobotics.solution.framework.websocket",
                "com.hikrobotics.solution.framework.component.ws"
        },
        exclude = {
                DataSourceAutoConfiguration.class,
                FlywayAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
                // com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration.class  // 暂未引入 dynamic-datasource 依赖
        })
@ComponentScan(
        basePackages = {
                "com.hikrobotics.solution.framework.websocket",
                "com.hikrobotics.solution.framework.component.ws"
        },
        excludeFilters = {
                // 防御性兜底：万一后续 framework-starter 加新 bean
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = {
                                "com\\.hikrobotics\\.solution\\.framework\\.component\\.account\\..*",
                                "com\\.hikrobotics\\.solution\\.framework\\.component\\.appaccount\\..*",
                                "com\\.hikrobotics\\.solution\\.framework\\.component\\.auth\\..*",
                                "com\\.hikrobotics\\.solution\\.framework\\.component\\.log\\..*",
                                "com\\.hikrobotics\\.solution\\.framework\\.component\\.oauth2\\..*",
                                "com\\.hikrobotics\\.solution\\.framework\\.component\\.file\\..*"
                        }
                )
        })
@EnableAsync
@EnableScheduling
public class WsTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(WsTestApplication.class, args);
    }
}
