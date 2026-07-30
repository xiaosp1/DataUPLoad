# W-BUILD-01 报告：mvn package 重打 jar

**工单**: W-BUILD-01 P0 子单
**执行**: subagent (industry/b44f7586-71b0-4f3b-a669-988c70376917)
**完成时间**: 2026-07-31 01:23 (Asia/Shanghai)
**耗时**: ~5 分钟 (实际编译 18.5s，比预估的 5-15min 快很多 — mvn 增量缓存命中)

---

## 1. ✅ 完成清单

| # | 项 | 状态 | 说明 |
|---|---|---|---|
| 1 | 备份 target/ 老 jar | ⚠️ 部分 | 已备份到 `target/*.jar.bak-20260731-prebuild`，但 **`mvn clean` 把备份一起删了**；老 jar 在 `backup/pre-W-X13d-20260723-081048/DataupLoad-final.jar.bak` 找到等价副本（2026-07-23 01:03:26，时间戳吻合原始 jar） |
| 2 | `mvn clean package -DskipTests` | ✅ PASS | 18.531s，BUILD SUCCESS |
| 3 | 新 jar 时间戳新 | ✅ | `DataupLoad-1.0-SNAPSHOT-20260731012236.jar` (2026/7/31 01:22:49) |
| 4 | 新 jar 含关键修改 | ✅ | DefectTypeServiceImpl / Application / StatusRecordServiceImpl / ExcelUtils / DataMergeStrategy 全部在 fat-jar 内 (见 §3) |
| 5 | 没动 `E:\DEMO\DATALINK\DataupLoad\lib\` 老 jar | ✅ | `E:\DEMO\DATALINK` 是 JUNCTION → `E:\DEMO\数据采集`，从未触碰 |
| 6 | 没重启服务 | ✅ | hik-java PID 6000 仍运行，启动时间 2026/7/30 23:40:39，运行 6260s (104min+) |
| 7 | commit + push 报告 | ✅ (本步) | 仅 commit 报告 + pom.xml 修复，**不 commit jar/log** |
| 8 | W-BUILD-01-report.md 输出 | ✅ | 本文件 |

---

## 2. ⚠️ 关键发现：pom.xml 隐藏 bug（**已修复**）

**问题**: `mvn clean package` 第一次跑 **直接编译失败**，错误：

```
[ERROR] /E:/DEMO/数据采集/DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/util/ExcelUtils.java
        [5,25] 程序包com.alibaba.excel不存在
[ERROR] /E:/DEMO/数据采集/DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/excel/DataMergeStrategy.java
        [4,35] 程序包org.apache.poi.ss.usermodel不存在
... (累计 30+ 错误)
[INFO] BUILD FAILURE
```

**根因追溯**:
- W-X29 P2 提交 `c882101f` (2026-07-24 20:36) **新增** `detect/util/ExcelUtils.java` (254 行) + `detect/excel/DataMergeStrategy.java` (342 行)
- 该 commit message 写明「依赖: easyexcel-2.2.6.jar + poi-3.17.jar (lib 已有)」，但 **pom.xml 没声明这两个 jar**
- 旧的 `DataupLoad-1.0-SNAPSHOT-20260723010315.jar` 是 W-X29 之前的产物，**也没有这俩 class**
- 旧 jar 实际是这么编出来的：之前的 7-23 编译跑的是 **增量 cache** (`Nothing to compile - all classes are up to date`)，所以编译顺利 — 但 target/classes 的最近一次完整重编是 **2026-07-30 23:11**（彼时编译器用了 IDE 的隐式 classpath 或别的 lib 来源）

**修复方案**: 跟随 pom.xml 内已存在的 W-X11b 模式（sa-token-oauth2 / flyway-core 都用 `system scope + lib/ 物理 jar`），**新增 5 个 system-scope 依赖**:

```xml
<!-- W-BUILD-01 01:21：W-X29 P2 (c882101f) 新增 detect/util/ExcelUtils + detect/excel/DataMergeStrategy
     依赖 easyexcel-2.2.6 + poi-3.17 (lib/ 已有物理 jar)，但 pom 没声明 → mvn clean package 编译失败
     同 W-X11b sa-token-oauth2 / flyway-core 模式：system scope + lib/ 物理 jar。
     一起补上 poi-ooxml + poi-ooxml-schemas + xmlbeans 以免 spring-boot:repackage 打包后 NoClassDefFoundError。 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>2.2.6</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/easyexcel-2.2.6.jar</systemPath>
</dependency>
<dependency>  ...  poi-3.17.jar
<dependency>  ...  poi-ooxml-3.17.jar
<dependency>  ...  poi-ooxml-schemas-3.17.jar
<dependency>  ...  xmlbeans-2.6.0.jar
```

**修复后**: 第二次 `mvn clean package -DskipTests` 编译 **18.5s 成功**。

**透明度声明**: 这个 pom 改动 **不在原 W-BUILD-01 工单范围内**（工单只说「跑 mvn」），但若不修就拿不到新 jar。已按现有文档化模式追加，工单末尾会要求老板 review。

---

## 3. 编译产物 (新 jar 详细)

| 产物 | 路径 | 大小 | 时间戳 |
|---|---|---|---|
| 原始 jar | `target/DataupLoad-1.0-SNAPSHOT-20260731012236.jar` | 5,813,476 字节 (5.55 MB) | 2026/7/31 01:22:49 |
| **Spring Boot fat-jar** | `target/dependency-copy/DataupLoad-1.0-SNAPSHOT-20260731012236.jar` | 71,901,368 字节 (68.6 MB) | 2026/7/31 01:22:51 |
| 编译日志 | `build-20260731.log` | 5,435 字节 | 2026/7/31 01:22:51 |
| 新 jar 备份 | `backup/DataupLoad-1.0-SNAPSHOT-20260731012236.jar.bak-postbuild` | 5,813,476 字节 | 2026/7/31 01:22:49 |
| 老 jar 等价副本 | `backup/pre-W-X13d-20260723-081048/DataupLoad-final.jar.bak` | — | 2026/7/23 01:03:26 |

### 新 jar 关键 class 验证（fat-jar BOOT-INF/classes）

```
✓ BOOT-INF/classes/com/hikrobotics/solution/Application.class                  (2026/7/31 01:22:43)
✓ BOOT-INF/classes/com/hikrobotics/solution/module/alarm/service/impl/
    DefectTypeServiceImpl.class                                                  (2026/7/31 01:22:44, 11291 字节)
✓ BOOT-INF/classes/com/hikrobotics/solution/module/detect/service/impl/
    StatusRecordServiceImpl.class                                                (2026/7/31 01:22:43)
✓ BOOT-INF/classes/com/hikrobotics/solution/module/detect/util/ExcelUtils.class (2026/7/31 01:22:44) [+ $SheetConfig / $Table 内部类]
✓ BOOT-INF/classes/com/hikrobotics/solution/module/detect/excel/
    DataMergeStrategy.class                                                      (2026/7/31 01:22:44)
✓ BOOT-INF/classes/com/hikrobotics/solution/module/alarm/service/impl/
    AlarmRecordServiceImpl.class                                                 (2026/7/31 01:22:43)
```

### DefectTypeServiceImpl 方法签名验证（javap -p）

```java
public class DefectTypeServiceImpl
  extends ServiceImpl<DefectTypeMapper, DefectType>
  implements IDefectTypeService {
  public BaseResult handleDefectTypeAdd(DefectTypeDTO);
  public BaseResult handleDefectTypeDel(Integer);
  public BaseResult editDefect(DefectTypeDTO);     // ← ADR-0014/0015 alarmEnable 校验在此方法
  public BaseResult listDefect(SearchDefectDTO);
  public DefectType getByNameAndType(String, Integer);
  ...
}
```

> 工单提到的 `setAlarmEnable` 实际是 `editDefect` 方法内的 `alarmEnable` 字段校验 (DefectTypeServiceImpl.java 第 117-121 行：soundEnable=1 + alarmEnable=0 → 错误 20503)，ADR-0014 / 0015 修复已确认进入新 jar。

### 目标文件 (target/) 状态

```
target/classes/        → 200 个 .class 文件，全部 2026/7/31 01:22 时间戳
target/dependency-copy/DataupLoad-1.0-SNAPSHOT-20260731012236.jar  → 71.9 MB fat-jar
target/DataupLoad-1.0-SNAPSHOT-20260731012236.jar                   → 5.5 MB 原始 jar
target/lib-extract/    → (spring-boot 抽取的 lib)
target/maven-archiver/ → pom 描述文件
target/maven-status/   → 增量编译状态
```

---

## 4. 🚫 未执行的动作（按工单约束）

| 项 | 状态 | 说明 |
|---|---|---|
| ❌ 重启服务 | **未做** | 工单明确禁止；hik-java PID 6000 仍运行 |
| ❌ `Copy-Item target\*.jar E:\DEMO\DATALINK\DataupLoad\lib\` | **未做** | `E:\DEMO\DATALINK` 是 JUNCTION → `E:\DEMO\数据采集`，工单建议重启时再做 |
| ❌ commit jar / log | **未做** | `.gitignore` 已排除 target/，jar 仍在本地 (见 §3) |

**当前在线服务状态**: hik-java.exe PID 6000 (started 2026/7/30 23:40:39)，使用 **内存里的旧 class**（重启前不会自动重载）。新 jar 已就绪，老板可在业务低峰期手动替换并重启。

---

## 5. 📋 老板重启替换流程建议（待决策）

**步骤 1: 业务低峰期通知**（建议凌晨或交接班窗口）
**步骤 2: 停服**
```powershell
# 在 X:\DataupLoad 跑 (或 start-hik.bat 所在的机器)
taskkill /F /IM hik-java.exe
```
**步骤 3: 替换 jar**
```powershell
# XCOPY 保留原 jar 备份
XCOPY /E /I /Y /H /K "E:\DEMO\数据采集\DataupLoad\target\dependency-copy\DataupLoad-1.0-SNAPSHOT-20260731012236.jar" "X:\DataupLoad\lib\DataupLoad-1.0-SNAPSHOT.jar"
# 注: 现部署实际跑 target\classes (start-hik.bat 里 -cp lib\*;target\classes)，
#     重启时 classes 已被 mvn 重生成；但保险起见可同时复制 fat-jar 到 lib\
```
**步骤 4: 启动**
```powershell
cd X:\DataupLoad
jdk\bin\hik-java.exe -cp lib\*;target\classes -Dfile.encoding=UTF-8 -Dspring.config.location=classpath:/,file:X:/DataupLoad/config/ -Dspring.config.name=application -Dserver.port=80 com.hikrobotics.solution.Application
```
**步骤 5: 验证**
- 端口 80 启动正常
- /actuator/health (若有) 返回 UP
- 业务接口抽样 (alarm/list, defect/list, realTime 推送)
- 日志无 ERROR (`log/DataupLoad/error.log`)

**回滚方案**: 拷贝 `backup/pre-W-X13d-20260723-081048/DataupLoad-final.jar.bak` 到 lib/ + 重启（注意：老 jar 不含 W-X29 的 Excel 导出功能，但 alarm/defect 主链路功能完整）。

---

## 6. 📝 风险 & 提示

1. **pom.xml 改动需要 review**: 5 个 system-scope deps 是按现有 W-X11b 模式追加，但严格说不在 W-BUILD-01 范围内。建议老板过一眼，确认是否要走 ADR 流程（这模式在仓库里已经有先例）。
2. **Spring Boot Maven Plugin WARNING**: 编译日志里有 `'dependencies.dependency.systemPath' should not point at files within the project directory` 的黄色警告，是已存在的 system-scope 副作用，非本次新增。
3. **commons-compress**: 原本打算补这个 poi 依赖，但 lib/ 没找到独立 jar — poi-3.17.jar 已内部 bundle 它，无需额外声明。xmlbeans 已补。
4. **target/classes 时间戳**: mvn 重生成后是 2026/7/31 01:22，所以如果老板想直接 hot-restart（不替换 jar），classes 已是最新的。

---

## 7. commit 信息

```
W-BUILD-01: mvn package 重打 jar (待老板决定重启时机)

- mvn clean package -DskipTests BUILD SUCCESS (18.5s)
- 新 jar: target/dependency-copy/DataupLoad-1.0-SNAPSHOT-20260731012236.jar (71.9 MB)
- 含 W-X29 ExcelUtils + DataMergeStrategy + ADR-0014/0015 alarmEnable 校验
- 附带修复 pom.xml 隐藏 bug: W-X29 新增的 easyexcel/poi 依赖从未声明
  (沿用 W-X11b sa-token-oauth2 / flyway-core 的 system-scope + lib/ 物理 jar 模式)
- 未重启服务 (hik-java PID 6000 仍运行)
- 备份: backup/pre-W-X13d-20260723-081048/DataupLoad-final.jar.bak (老 jar 等价副本)
- 重启流程详见 docs/work-orders/W-BUILD-01-report.md §5
```
