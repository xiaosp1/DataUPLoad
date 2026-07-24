# W-X01B：1:1 仿 PSM —— 把 ticket 续期搬到 YKServiceImpl 内部（老板 20:44 B 方案）

## 老板原话（20:44 重复一次）
"选择B，意思是一比一复刻psm的逻辑 你别理解错了呀。"

## 范围（只能改 1 个文件 + 删 1 个文件）
- **改** `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\yingke\service\impl\YKServiceImpl.java`
- **删** `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\yingke\scheduled\YKScheduledTicketRenewer.java`

## PSM 字节码金标准（yk_impl_disasm.txt 第 19/24-49 行）
```java
public class YKServiceImpl {
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @PostConstruct
    private void init() {
        this.threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        this.threadPoolTaskScheduler.setPoolSize(1);
        this.threadPoolTaskScheduler.setThreadNamePrefix("yk-renew-");
        this.threadPoolTaskScheduler.initialize();

        this.updateTicket();  // 第一次登录

        // 每 yk.login-interval 分钟续一次
        long intervalMin = ykConfig.getLoginInterval();  // 默认 50
        this.threadPoolTaskScheduler.schedule(
            this::updateTicket,
            Instant.now().plusMillis(intervalMin * 60 * 1000)
        );
    }

    private void updateTicket() {
        // 调 yk API 拿 ticket
        ...
    }
}
```

## DataupLoad 当前现状（错的）
- W-F01-A 写了 `YKScheduledTicketRenewer`（新类）用 `@Autowired YKServiceImpl` + 反射
- **老板说错**：1:1 复刻 PSM，不是 hack 改注入
- 必须把续 ticket 逻辑搬进 YKServiceImpl 内部

## 任务（4 步）

### Step 1：删掉 W-F01-A 写的废类
```powershell
Remove-Item "E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\yingke\scheduled\YKScheduledTicketRenewer.java"
```

### Step 2：改 YKServiceImpl（仿 PSM）
在 `YKServiceImpl.java` 顶部加 import：
```java
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
```

加字段：
```java
private ThreadPoolTaskScheduler threadPoolTaskScheduler;
```

加 init 方法（**PSM 是 private void**，DataupLoad 保持一致）：
```java
@PostConstruct
private void init() throws Exception {
    this.threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    this.threadPoolTaskScheduler.setPoolSize(1);
    this.threadPoolTaskScheduler.setThreadNamePrefix("yk-renew-");
    this.threadPoolTaskScheduler.initialize();
    this.updateTicket();  // PSM 第一次就登录（拿 ticket）
    
    // PSM 是用 threadPoolTaskScheduler.schedule(this::updateTicket, ...) 自调
    // DataupLoad 可以直接调 updateTicket（已经登录过），下一次让 Spring Boot 的 @Scheduled 触发
    // 注意：PSM 是登录成功后才 schedule；DataupLoad 这里也要登录成功后才 schedule
}
```

**等等**：PM 重读 PSM 字节码，PSM 是 `init() → updateTicket() + schedule(updateTicket, login-interval分钟后)`。

**DataupLoad 改法**：把 init() 里的 schedule 逻辑搬过来，每 `yk.login-interval` 分钟（默认 50）调一次 updateTicket。

### Step 3：编译 + 启动
```powershell
cd E:\DEMO\数据采集\DataupLoad
# 全量编译
Get-ChildItem "src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName } > all-java.txt
.\jdk\bin\javac.exe -encoding UTF-8 -d target\classes -cp "lib\*" -sourcepath src\main\java @(Get-Content all-java.txt)
# 启动
cmd /c "start /B scripts\start-app.bat"
Start-Sleep 60
Get-NetTCPConnection -State Listen -LocalPort 80  # 必须 LISTEN
```

### Step 4：验证
- 80 端口起来
- 看 log：`log/DataupLoad/DataupLoad.log` 找 "yk ticket" 或 "ThreadPoolTaskScheduler"
- **不准 POST 报警**（老板 SOP）

## 约束
- ❌ **不准改 yk.enable**（必须保持 false）
- ❌ **不准 POST /client/data/alarm**
- ❌ **不准重启超过 1 次**
- ❌ **不准改 W-X03/W-X05 的文件**（其他 Worker 自己干）

## 报错时
- 启动失败 → 看 `log/DataupLoad/DataupLoad.log` 最后 30 行 → PM 群里汇报
- YKServiceImpl 当前已经有 init() 方法？→ 看现有代码，不能重复定义
- YKServiceImpl 当前已经有 ThreadPoolTaskScheduler 字段？→ 不能重复

## 回报格式
```
W-X01B 完成：[success/partial/blocked]
文件改动：[YKServiceImpl.java +N 行，删除 X 文件]
方案：[完整 B 方案描述]
启动 PID：[数字]
80 端口：[up/down]
log 关键行：[从 DataupLoad.log 摘 3 行]
```
