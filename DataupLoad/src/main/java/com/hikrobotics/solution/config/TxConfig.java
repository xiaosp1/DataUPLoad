package com.hikrobotics.solution.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * W-B03 detect 模块配套：注册事务管理器。
 *
 * <p>PSM framework-starter 依赖 dynamic-datasource-spring-boot3-starter 4.x，
 * Boot 3 下其自动装配 {@code DataSourceTransactionManager} 缺失（dynamic-datasource
 * 只负责多数据源路由，不接管事务管理器）。{@code @EnableTransactionManagement} 已
 * 在 {@link com.hikrobotics.solution.Application} 启用，但缺少 {@link PlatformTransactionManager}
 * bean 会让所有 {@code @Transactional} service 调用报
 * {@code NoSuchBeanDefinitionException: TransactionManager}。</p>
 *
 * <p>这里基于 dynamic-datasource 暴露的 {@link DynamicRoutingDataSource} 注册
 * 单个 {@link DataSourceTransactionManager}。detect/line 模块均使用 master 数据源，
 * 事务足够。如未来引入多数据源事务，再切换到 {@code ChainedTransactionManager} 或
 * dynamic-datasource 的 LazyConnectionDataSourceProxy。</p>
 */
@Configuration
public class TxConfig {

    @Bean
    public PlatformTransactionManager transactionManager(DynamicRoutingDataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
