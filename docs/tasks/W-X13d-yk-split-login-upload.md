# W-X13d: yk 语义拆分 login/upload 双开关

**派工时间**：2026-07-23 08:10 GMT+8
**派工人**：PM 锋卫
**优先级**：🔴 P0
**预计工时**：1.5~2h（代码改 + 编译 + 重启 + 1h 验收）

---

## 🎯 工单目标

把当前 `yk.enable` 一个开关拆成两个语义：
- **`yk.loginEnabled`**：是否调 MES `AuthenticationController.Login` 拿 ticket / 续约
- **`yk.uploadEnabled`**：报警来了是否真推 yk 到 MES

**目标效果**：灰盒期 `loginEnabled=true` + `uploadEnabled=false` → ticket 正常拿到手（凭证预热）→ 报警来了静默跳过推送 → **0 ERROR**。正式上线改 `uploadEnabled=true`。

---

## 📐 设计方案

### 1. YKConfig.java 改造

```java
@Component
@ConfigurationProperties("yk")
public class YKConfig {
   // 灰盒拆双开关
   private boolean loginEnabled = true;   // 默认开：始终去 login 拿/续 ticket
   private boolean uploadEnabled = false; // 默认关：灰盒不推 MES
   private String url;
   private String username;
   private String password;
   private Integer loginInterval;
   private String workshop;
   private boolean searchRemove = true;
   
   // getter/setter 都要加（11 个）
}
```

**字段命名建议**（跟老板商量过）：
- `loginEnabled`（替代 `enable`，原字段可保留为 `@Deprecated` 标记）
- `uploadEnabled`（新字段）

**兼容老字段**：保留 `private boolean enable`，标注 `@Deprecated`，getter 返回 `loginEnabled || uploadEnabled`（只要有一个开就算开，**保留 0.x 版本平滑过渡**）。或者直接干掉老字段，看老板拍。

### 2. YKServiceImpl.java 改造

```java
private void updateTicket() {
   if (this.ykConfig.isLoginEnabled()) {   // ← 改：loginEnabled
      // login + 续约逻辑不变
   }
}

@EventListener
public void pushAlarm2YK(PushAlarmEvent event) {
   if (StringUtils.isNotBlank(this.ticket) 
       && this.ykConfig.isUploadEnabled()) {  // ← 改：uploadEnabled
      // 推
   } else if (!this.ykConfig.isUploadEnabled()) {
      log.debug("upload disabled, skip push.[alarm={}]", event.getAlarmRecord());  // ← 改 debug
   } else {
      log.error("push alarm to yk error, ticket is null.[alarm={}]", event.getAlarmRecord());  // 真正的 bug 才报 error
   }
}
```

### 3. application-prod.yml 改造

```yaml
yk:
  # enable: false   ← 保留注释，标 DEPRECATED
  loginEnabled: true    # 灰盒拿 ticket（凭证预热）
  uploadEnabled: false  # 灰盒不推 MES（上线后改 true）
  workshop: QZN2
  username: HKSJSB
  password: HKSJSB123
  login-interval: 50
  url: http://192.168.80.33:10031/api/dataportal/invoke
```

---

## 🧪 验收清单（DoD）

### 代码层
- [ ] `YKConfig.java` 加 `loginEnabled` / `uploadEnabled` 字段 + getter/setter
- [ ] `YKServiceImpl.java` `updateTicket()` 改判 `loginEnabled`
- [ ] `YKServiceImpl.java` `pushAlarm2YK()` 改判 `uploadEnabled`
- [ ] `pushAlarm2YK()` else 分支拆：uploadEnabled=false 走 debug 日志，ticket 为 null 才走 error
- [ ] `application-prod.yml` 加新字段，注释掉老的 `enable`

### 编译层
- [ ] `mvn clean package -DskipTests` 0 错 0 警告
- [ ] 新 jar SHA256 ≠ 老 jar SHA256
- [ ] 新 jar 包含新 class（`javap -p` 看 YKConfig 是否多 2 个字段）

### 运行时（PM 验收 — 铁则 41 强制）
- [ ] **备份**：当前 `E:\DataupLoad-final.jar` 复制到 `DataupLoad\backup\pre-W-X13d-{ts}\`
- [ ] **备份**：当前 `application-prod.yml` 复制到同目录
- [ ] **停** PID 33004：`Stop-Process -Id 33004 -Force`
- [ ] **部署**新 jar 到 `E:\DataupLoad-final.jar`
- [ ] **启动**：`Start-Process -FilePath "DataupLoad\jdk\bin\hik-java.exe" -ArgumentList @("-jar","-Dfile.encoding=UTF-8","E:\DataupLoad-final.jar","--spring.config.location=classpath:/,file:E:/DEMO/数据采集/DataupLoad/config/") -WorkingDirectory "E:\DEMO\数据采集\DataupLoad"`
- [ ] **等 30s**，端口 80 LISTEN
- [ ] **查启动 log** 含 `loginEnabled=true` / `uploadEnabled=false`（Spring 默认会打印配置）

### 行为层（关键 — 灰盒期应满足）
- [ ] **ticket 拿到手**：log 出现 `INFO ... success to get ticket from yk.[ticket=xxx]`（在 updateTicket 第一次调用时）
- [ ] **不再每条报警吐 ERROR**：观察 1h，`push alarm to yk error` ERROR **0 增量**
- [ ] **报警仍落 PG**：`alarm_record` 表 1h 内行数 > 0（保持原有入库链路）
- [ ] **50min 后续约**：log 出现第二次 `success to get ticket`（验证 ticket 续约机制）
- [ ] **13 个相机仍 ESTABLISHED**：`Get-NetTCPConnection -LocalPort 80 | Where State -eq Established` count > 0

### 文档层
- [ ] **STATUS.md** 刷新（加 W-X13d DONE / 拆双开关）
- [ ] **铁则 42 立项**：`yk 拆双开关（login/upload），正式上线必须改 uploadEnabled=true`
- [ ] **ADR-0006 增补**：Java 端 yk 拆双开关（原本只讲 C# 端）

---

## 🛡️ 回滚预案

如果 1h 验收发现新 jar 有问题：
1. `Stop-Process -Id <new pid> -Force`
2. `Copy-Item DataupLoad\backup\pre-W-X13d-{ts}\DataupLoad-final.jar.bak E:\DataupLoad-final.jar -Force`
3. `Copy-Item DataupLoad\backup\pre-W-X13d-{ts}\application-prod.yml.bak DataupLoad\config\application-prod.yml -Force`
4. 重新启动 hik-java（同启动命令）

**PM 必读**：所有回滚文件保留 7 天，期间不要删。

---

## ⚠️ 风险与约束

1. **重启会断网 10~30s**：13 个相机 ESTABLISHED 暂时断开。**白天可接受**（灰盒期报警不依赖 yk 推送），但建议改完后**先用 PowerShell 启新进程，等 80 端口起来再 stop 老进程**（灰盒期允许）
2. **PG 进程不动**：hik-java 重启不影响 PG 14.23
3. **i18n 漏问题（W-X11 历史教训）**：Worker 改完必须 `unzip -l E:\DataupLoad-final.jar | grep -i messages` 确认 i18n 资源还在
4. **铁则 41 强制**：本次验收必须含"运行时 1h ERROR 增量观测"，不再只看字节

---

## 📋 派工命令模板

```bash
codex exec -C "E:\DEMO\数据采集" --skip-git-repo-check -s workspace-write "
读 docs/tasks/W-X13d-yk-split-login-upload.md 全部内容。
按工单设计改 YKConfig / YKServiceImpl / application-prod.yml。
改完编译，0 错 0 警告，新 jar SHA256 给我。
不要重启 hik-java，不要覆盖 E:\DataupLoad-final.jar。
PM 会接手部署+验收。
"
```

---

## 🏷️ 关联

- **W-X13a**（PASS / 但漏运行时验收 → 铁则 41 立项）
- **铁则 36**（yk 永久熔断 — 现升级为"yk 双开关：login 永久开，upload 灰盒关 / 上线开"）
- **铁则 41**（Worker 验收必须含运行时验证）
- **铁则 42**（yk 拆双开关 / 上线 checklist）
- **ADR-0006**（C# 端 yk 熔断 → 增补 Java 端）

---

🏭 PM 锋卫 · 2026-07-23 08:10
