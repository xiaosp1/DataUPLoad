# W-FIX-01 Report — javac -parameters + searchOffLineClient stub

**Worker**: Java developer (subagent W-FIX-01)
**Date**: 2026-07-24
**Status**: ✅ 完成

---

## 1. 改动文件清单

| 文件 | 改动类型 | 说明 |
|---|---|---|
| `X:\compile.bat` | 修改 | 加入 `-parameters` |
| `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\service\IStatusRecordService.java` | 修改 | 接口签名 PSM 1:1 |
| `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\service\impl\StatusRecordServiceImpl.java` | 修改 | 实现 searchOffLineClient + class Javadoc |
| `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\AlarmRecordServiceImpl.java` | 修改（仅 Javadoc 注释）| 旧 "stub 返回空集" 注释已过期，改为指向 W-FIX-01 |

未触碰其它逻辑。

---

## 2. 任务 A: javac -parameters

### 改动 diff（`X:\compile.bat`）

```diff
- "%JAVA_HOME%\bin\javac" -encoding UTF-8 -cp "%CP%" -d "%OUT%" @X:\sources.txt 2> X:\compile.err
+ "%JAVA_HOME%\bin\javac" -encoding UTF-8 -parameters -cp "%CP%" -d "%OUT%" @X:\sources.txt 2> X:\compile.err
```

`go.bat` 仅启动 JVM、不调用 javac，无需改。

### 验证：字节码含 MethodParameters

```
$ javap -p -v StatusRecordServiceImpl.class | grep -A1 MethodParameters
  MethodParameters:
    Name                           Flags
    records
  MethodParameters:
    Name                           Flags
    lineNo
  ...
  public java.util.List<...DeviceStateDTO> searchOffLineClient(java.lang.String,
                                                                java.lang.String,
                                                                java.lang.Integer);
    MethodParameters:
      Name                           Flags
      lineNo
```

✅ 参数名 `lineNo / faceNo / type / records / lineIds` 均写入 `MethodParameters` 属性。
Spring `@RequestParam` 反射可正常解析。

---

## 3. 任务 B: searchOffLineClient stub 1:1 实现

### PSM 反编译（参考）原文

```java
public List<DeviceStateDTO> searchOffLineClient(String lineNo, String faceNo, Integer type) {
    LambdaQueryWrapper qw = (LambdaQueryWrapper)((LambdaQueryWrapper)((LambdaQueryWrapper)
        ((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(StatusRecordPO::getLineNo, lineNo))
            .eq(StatusRecordPO::getFaceNo, faceNo))
            .eq(StatusRecordPO::getType, type))
            .eq(StatusRecordPO::getStatus, DeviceStatus.OUTLINE.getValue());
    return this.list(qw).stream().map(DeviceStateDTO::new).toList();
}
```

### DataupLoad 实现要点

1. **接口签名 PSM 1:1**：返回类型从 `Object` → `List<DeviceStateDTO>`。
   含义：按 `(lineNo, faceNo, type, status=OUTLINE)` 过滤 status_record，
   每行映射成 `DeviceStateDTO`。
2. **字段对应**：DataupLoad 用 `StatusRecord`（W-B03 迁移后的 entity 名称）替代 PSM 的
   `StatusRecordPO`，属性一一对应：`lineNo / faceNo / type / status / deviceNo /
   deviceName / updateTime`。
3. **`DeviceStateDTO::new` 转换器**：DataupLoad 的 `DeviceStateDTO(StatusRecord)`
   构造函数已存在（W-B03 引入），与 PSM 字段一一对应。
4. **调用方兼容性**：`AlarmRecordServiceImpl.handleAlarmSearch` 已用
   `data = (List<?>) statusRecordService.searchOffLineClient(...)`，
   强转 `List<?>` 接受 `List<DeviceStateDTO>`，无需改动业务代码。
5. **去掉 stub**：移除原 `log.debug("searchOffLineClient W-B03 stub: ...")` 日志和
   `Collections.emptyList()` 返回。

### 完整实现

```java
@Override
public List<DeviceStateDTO> searchOffLineClient(String lineNo, String faceNo, Integer type) {
   // W-FIX-01：1:1 抄自反编译 PSM StatusRecordServiceImpl.searchOffLineClient
   // SELECT * FROM status_record
   // WHERE line_no = #{lineNo}
   //   AND face_no = #{faceNo}
   //   AND type    = #{type}
   //   AND status  = DeviceStatus.OUTLINE (2)
   // → 转换为 DeviceStateDTO 列表
   LambdaQueryWrapper<StatusRecord> qw = Wrappers.<StatusRecord>lambdaQuery()
       .eq(StatusRecord::getLineNo, lineNo)
       .eq(StatusRecord::getFaceNo, faceNo)
       .eq(StatusRecord::getType, type)
       .eq(StatusRecord::getStatus, DeviceStatus.OUTLINE.getValue());
   return this.list(qw).stream().map(DeviceStateDTO::new).toList();
}
```

---

## 4. 编译结果

### 命令（任务指定）

```cmd
cd E:\DEMO\数据采集 && javac -encoding UTF-8 -parameters ^
  -d X:\DataupLoad\target\classes ^
  -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" ^
  -sourcepath DataupLoad\src\main\java ^
  DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\service\impl\StatusRecordServiceImpl.java
```

```
警告: 注释处理不适用于隐式编译的文件。
  使用 -proc:none 禁用注释处理或使用 -implicit 指定用于隐式编译的策略。
1 个警告
```

- 0 errors
- 唯一警告：JDK 17 在「单文件 + -sourcepath 隐式编译」模式下的注释处理提示（与本次改动无关，编译多个文件即消失）

### 全量重编（`X:\compile.bat`，启用 `-parameters`）

```
javac exit code: 0
```

✅ **0 errors**。

`compile.err` 内容（GBK 解读）：
```
注: 某些输入文件使用了未经检查或不安全的操作。
注: 有关详细信息, 请使用 -Xlint:unchecked 重新编译。
```
均为 JDK 17 标准 informational notes（不是 error）。`-Xlint:unchecked` 是 MyBatis-Plus
泛型擦除导致的、与本次改动无关。

---

## 5. 已知限制 / 注意事项

1. **mybatis plus `this.list(qw)` 行为**：`list()` 在 null qw 时会 selectList(null) —
   本实现 qw 已构造，不会为 null。安全。
2. **status 字段 DB 类型**：`status_record.status` 为 `int4`，
   `DeviceStatus.OUTLINE.getValue() = 2`，与 `Integer type` 参数独立无歧义。
3. **`type` 为 null 时的语义**：PSM 同样直接 `.eq(type, null)`，MyBatis-Plus 会
   生成 `type IS NULL`。DataupLoad 保持一致。
4. **`handleAlarmSearch` type!=4 分支现状**：调用方 `(List<?>)` 强转依旧成立；
   `BaseResult.data(Object)` 接受任意类型，HTTP 序列化时 Jackson 自动派生字段。
5. **遗留噪音**：javac 旧日志 `compile.err` 仍含 GBK 显示乱码（cmd 控制台 codepage 导致），
   不影响构建状态。
6. **未触及其它逻辑**：service 接口仅 `searchOffLineClient` 签名变更；AlarmRecordServiceImpl
   仅更新过期 Javadoc，业务代码不变。

---

## 6. 与上下游工作订单的衔接

- 上游 W-B03（PSM 1:1 迁移）：已完整，`searchClientStatus` / `receiveStatus` 不变。
- W-LIN-01（`LineServiceImpl.delete` 调用 `searchClientStatus`）：未受影响。
- W-X30b（`DealAlarmEvent` 发布）：未受影响。
- 下游 `AlarmRecordServiceImpl.handleAlarmSearch`：现在 type!=4 分支可返回真实离线
  客户端列表（业务效果与 PSM 一致）。

---

## 7. 完成状态

- [x] Task A: `compile.bat` 加 `-parameters`，全量编译 exit 0
- [x] Task B: `searchOffLineClient` 1:1 抄 PSM
- [x] 字节码含 MethodParameters
- [x] 调用方兼容（无需改业务代码）
- [x] 报告写到 `docs/work-orders/W-FIX-01-report.md`
