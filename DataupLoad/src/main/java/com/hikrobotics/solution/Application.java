package com.hikrobotics.solution;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * DataupLoad - PSM 复刻项目入口
 * 启动命令：./jdk/bin/hik-java -jar -Dfile.encoding=UTF-8 DataupLoad-1.0-SNAPSHOT-*.jar
 *
 * <p>W-B03 关键调整：</p>
 * <ul>
 *   <li>{@code @MapperScan} 范围由 PSM 模块包放宽到 {@code com.hikrobotics.**.mapper}，
 *       覆盖 framework-starter 内的 AccountDAO / AppAccountDAO / 其他 framework.*.mapper；</li>
 *   <li>{@code @ComponentScan} 显式列出 cn.hutool.extra.spring + com.hikrobotics.* 以注册
 *       framework-starter 的 {@code @Component} 类（JsonArrayTypeHandler 等）；</li>
 *   <li>{@code excludeFilters} 屏蔽 PSM 非迁移模块：加密狗、SSL、相机 SDK、FTP、版本检查等
 *       （依赖 native 库或外部服务，与 detect 任务无关）。
 *       W-AUTH-01 反转：放开 account / appaccount / oauth2 / auth 4 个包（PSM 标准登录链路），
 *       配合 hik-security 白名单 /web/auth/** + /web/account/** 启用。</li>
 * </ul>
 *
 * <p>保留：framework.component.log（trace filter）、framework.component.json（JsonArrayTypeHandler）、
 * framework.component.upload（HikUploadService，detect 模块会读图）、framework.component.db、
 * framework.component.json / framework.util（共用工具类）。</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ServletComponentScan
@EnableTransactionManagement(proxyTargetClass = true)
@ComponentScan(
   basePackages = {"cn.hutool.extra.spring", "com.hikrobotics.*"},
   excludeFilters = {
      @ComponentScan.Filter(
         type = FilterType.REGEX,
         pattern = {
            // W-AUTH-01 反转：移除 account/appaccount/oauth2/auth 4 个 excludeFilters
            // 加密狗（ADR-0004 不移植）
            "com\\.hikrobotics\\.solution\\.framework\\.component\\.dongle\\..*",
            // SSL 证书加载（不在 detect 范围）
            "com\\.hikrobotics\\.solution\\.framework\\.component\\.ssl\\..*",
            // 相机 SDK（visionsensor 依赖 JNA native lib，DataupLoad detect 端不需要）
            "com\\.hikrobotics\\.solution\\.framework\\.component\\.camera\\..*",
            // FTP（detect 模块用 HikUploadService，FTP 是另一个模块）
            "com\\.hikrobotics\\.solution\\.framework\\.component\\.ftp\\..*",
            // 版本检查 / listenner（仅 PSM 启动检查）
            "com\\.hikrobotics\\.solution\\.framework\\.component\\.version\\..*"
         }
      )
   }
)
@MapperScan("com.hikrobotics.**.mapper")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
