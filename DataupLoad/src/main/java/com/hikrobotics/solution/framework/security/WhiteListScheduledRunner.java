package com.hikrobotics.solution.framework.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hikrobotics.solution.framework.component.whitelist.mapper.WhiteIpDAO;
import com.hikrobotics.solution.framework.component.whitelist.model.WhiteIpPO;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 白名单 IP 定时同步（W-F01-B）。
 *
 * <p>职责：每 5 分钟从数据库 {@code white_ip} 表读取全量 IP，写入进程内的
 * {@link ConcurrentHashMap#newKeySet() ConcurrentHashMap.newKeySet()}，
 * 供业务侧按需读取。</p>
 *
 * <p>为什么是新增类、不直接改 PSM 同款 {@code WhiteListRunner}：</p>
 * <ul>
 *   <li>本工单范围限定只能新增 {@code framework/security/WhiteListScheduledRunner.java}
 *       这一个文件，framework-starter 中的 {@code WhiteListRunner} / {@code WhiteListUtil}
 *       都不能动；</li>
 *   <li>PSM 原版 {@code WhiteListRunner} 只在 {@code CommandLineRunner.run()} 跑一次，
 *       启动后再修改 {@code white_ip} 表不会生效；本类补齐定时刷新；</li>
 *   <li>内存结构由 {@link ConcurrentHashMap#newKeySet()} 提供（无序、线程安全），
 *       用 volatile 引用整体替换，避免长锁阻塞读侧。</li>
 * </ul>
 *
 * <p>触发点：</p>
 * <ul>
 *   <li>{@link PostConstruct @PostConstruct}：服务启动后立即同步一次，保证白名单立刻可用；</li>
 *   <li>{@link Scheduled @Scheduled(fixedRate = 5 * 60 * 1000)}：之后每 5 分钟定时同步一次，
 *       与任务要求完全一致。</li>
 * </ul>
 *
 * <p>日志：每次刷新完成后 INFO 输出
 * {@code "white ip list refresh over, count={}"}，与 PSM 同款前缀便于运维检索；
 * 异常单独 ERROR 输出，绝不让 {@code @Scheduled} 中止后续调度。</p>
 */
@Component
public class WhiteListScheduledRunner {

    private static final Logger log = LoggerFactory.getLogger(WhiteListScheduledRunner.class);

    /** 5 分钟刷新一次（毫秒），固定频率，与 PSM 启动一次的旧行为不同。 */
    private static final long FIXED_RATE_MS = 5L * 60L * 1000L;

    /**
     * 内存白名单 IP 集合。volatile 保证引用替换对其他线程立即可见，
     * 读侧不需要加锁即可拿到一个稳定的快照。
     */
    private static volatile Set<String> IP_SET = ConcurrentHashMap.newKeySet();

    @Autowired
    private WhiteIpDAO whiteIpDAO;

    /**
     * 启动后立即跑一次，避免服务起来后还有一段真空期访问被拒。
     */
    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 5 分钟一次的定时刷新入口。
     *
     * <p>步骤：</p>
     * <ol>
     *   <li>用 LambdaQueryWrapper 查 {@code white_ip} 全表，只 select ip 列；</li>
     *   <li>用 {@link ConcurrentHashMap#newKeySet()} 构造新 Set；</li>
     *   <li>整体替换 {@link #IP_SET} 引用，读侧不会看到中间态；</li>
     *   <li>输出 INFO 日志 {@code "white ip list refresh over, count={}"}。</li>
     * </ol>
     *
     * <p>异常处理：吞掉 + ERROR 日志，不抛出；与 {@link Scheduled @Scheduled}
     * 的语义一致——单次失败不应中止后续调度。</p>
     */
    @Scheduled(fixedRate = FIXED_RATE_MS)
    public void refresh() {
        try {
            LambdaQueryWrapper<WhiteIpPO> qw = Wrappers.<WhiteIpPO>lambdaQuery()
                .select(WhiteIpPO::getIp);
            Set<String> next = ConcurrentHashMap.newKeySet();
            for (WhiteIpPO po : whiteIpDAO.selectList(qw)) {
                if (po != null && po.getIp() != null) {
                    next.add(po.getIp());
                }
            }
            IP_SET = next;
            log.info("white ip list refresh over, count={}", IP_SET.size());
        } catch (Exception ex) {
            log.error("white ip list refresh failed, exception: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 暴露给业务侧读内存白名单的入口。返回当前引用的 Set 快照，
     * 调用方按需遍历即可，不要长期持有引用——下次 {@link #refresh()}
     * 之后 IP_SET 引用会换新对象。
     */
    public static Set<String> getIpSet() {
        return IP_SET;
    }
}
