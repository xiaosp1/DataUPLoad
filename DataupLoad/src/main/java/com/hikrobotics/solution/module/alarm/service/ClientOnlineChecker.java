package com.hikrobotics.solution.module.alarm.service;

import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.alarm.dto.AlarmDTO;
import com.hikrobotics.solution.module.alarm.service.IAlarmRecordService;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DataupLoad 客户端在线监测器（W-F04）。
 *
 * <p>职责：维护每个客户端（clientNo）最近一次心跳时间戳，每 30 秒扫描一次：
 * 若某客户端超过 60 秒未上报心跳（心跳停止即视作 WS 已断开），
 * 自动通过 {@link IAlarmRecordService#add(AlarmDTO)} 入口生成 type=3（设备报警）记录落 PG。</p>
 *
 * <p>设计要点：
 * <ul>
 *   <li>心跳时间戳存 {@link ConcurrentHashMap}，线程安全；</li>
 *   <li>检查间隔 30s、超时阈值 60s，两倍间隔是为了规避调度抖动导致的漏报；</li>
 *   <li>触发报警后从 map 中移除该 clientNo，避免 30s 后重复触发；</li>
 *   <li>走 {@code add()} 入口而不是直接入库，是为了复用 PSM AlarmRecordServiceImpl.add() 中
 *       的缺陷类型匹配 / 旧报警忽略 / yk 推送等链路。当前 yk.enable=false（老板 SOP），
 *       走 add() 不会真正推 yk，只落 PG；后续 yk.enable=true 时也无需改本类。</li>
 * </ul>
 *
 * <p>约束：仅新增本类 + 修改 AlarmRecordServiceImpl，不动其他文件；不重启服务、不推测试报警、
 * 不改 application-prod.yml。WS 接入侧可后续调用 {@link #recordHeartbeat} /
 * {@link #unregister} 接入心跳与断开事件，本类不依赖 WS 框架具体实现。</p>
 */
@Component
public class ClientOnlineChecker {

    private static final Logger log = LoggerFactory.getLogger(ClientOnlineChecker.class);

    /** 心跳停止多久后视作离线（毫秒）。 */
    private static final long HEARTBEAT_TIMEOUT_MS = 60L * 1000L;

    /** 单条客户端记录：clientNo → (lineNo, faceNo, lastHeartbeatMs)。 */
    private static final class ClientState {
        final String lineNo;
        final String faceNo;
        volatile long lastHeartbeatMs;

        ClientState(String lineNo, String faceNo, long lastHeartbeatMs) {
            this.lineNo = lineNo;
            this.faceNo = faceNo;
            this.lastHeartbeatMs = lastHeartbeatMs;
        }
    }

    private final ConcurrentHashMap<String, ClientState> clients = new ConcurrentHashMap<>();

    @Autowired
    private IAlarmRecordService alarmRecordService;

    /**
     * 注册 / 刷新一个客户端的心跳。WS 接入侧在收到心跳报文时调用。
     *
     * @param clientNo 客户端唯一标识（WS sessionId 或业务约定的 clientId）
     * @param lineNo   产线号
     * @param faceNo   工位号
     */
    public void recordHeartbeat(String clientNo, String lineNo, String faceNo) {
        if (clientNo == null || lineNo == null || faceNo == null) {
            return;
        }
        long now = System.currentTimeMillis();
        clients.compute(clientNo, (key, old) -> {
            if (old == null) {
                return new ClientState(lineNo, faceNo, now);
            }
            old.lastHeartbeatMs = now;
            return old;
        });
    }

    /**
     * 主动摘除一个客户端（例如 WS 收到正常 close 帧）。摘除后即使断网也不会触发离线报警。
     */
    public void unregister(String clientNo) {
        if (clientNo == null) {
            return;
        }
        clients.remove(clientNo);
    }

    /**
     * 每 30 秒扫描一次，> 60 秒无心跳的客户端触发 type=3 离线报警。
     */
    @Scheduled(fixedDelay = 30 * 1000L)
    public void scanOfflineClients() {
        if (clients.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, ClientState>> it = clients.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ClientState> entry = it.next();
            String clientNo = entry.getKey();
            ClientState state = entry.getValue();
            long age = now - state.lastHeartbeatMs;
            if (age < HEARTBEAT_TIMEOUT_MS) {
                continue;
            }
            // 先从 map 中摘除，避免重复触发
            it.remove();
            try {
                AlarmDTO dto = new AlarmDTO();
                dto.setUuid(UUID.randomUUID().toString());
                dto.setTime(HikDateUtil.getCurrentTime());
                dto.setType(3); // 设备报警
                dto.setLineNo(state.lineNo);
                dto.setFaceNo(state.faceNo);
                dto.setLevel(2); // 严重
                dto.setMessage("客户端掉线[" + clientNo + "]");
                alarmRecordService.add(dto);
                log.warn("client offline alarm generated. clientNo={}, lineNo={}, faceNo={}, ageMs={}",
                    clientNo, state.lineNo, state.faceNo, age);
            } catch (Exception ex) {
                log.error("generate client offline alarm failed. clientNo={}, cause={}",
                    clientNo, ex.toString(), ex);
            }
        }
    }
}
