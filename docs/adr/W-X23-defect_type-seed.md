# W-X23: 补 defect_type 表种子数据 + 验证白名单

## 背景
老板 2026-07-23 21:55 拍板：补 defect_type 表，uploadEnabled 维持 false（不真推 yk）。

W-X22/W-X22b 1h 灰盒结果：3028 报警 → 100% 被 defect_type 模板过滤掉 → 0 进入白名单 → 0 推送。
结论：之前 0 推送是"假阳性"，不是白名单生效。需要补 defect_type 让真实缺陷进 interesting 列表，验证 4 条派生白名单是否生效。

## 目标
1. 查清 defect_type 表当前状态（schema + 现存数据 + 模板判断逻辑）
2. 从 PSM 文档/SQL 迁移/Java 类反编译产物中找出"该进 interesting"的真实 defect_type
3. 派生一组种子数据（PM 验收）
4. INSERT（不上传 yk，红线）
5. 重启 hik-java，跑 1h 灰盒，看白名单是否真生效

## 红线（老板硬约束）
- ❌ yk.uploadEnabled 不改成 true
- ❌ 不能改 yml / 不能改业务代码 / 不能删数据
- ❌ 不动 ignore_alarm 表（W-C05 已经 PASS，不重做）
- ✅ alarm.global-enabled=true（让报警正常走全链路）
- ✅ 5min 起不来立即回滚老 PID

## 验证标准（DOD）
- defect_type 至少有 N 条 PSM 可溯源的种子数据（待 W-X23 worker 调研后定）
- 1h 灰盒：报警漏斗能看到 isIgnore 白名单命中 > 0（证明 4 条派生白名单真生效）
- yk 推送 = 0（红线守住）
- BadSqlGrammarException = 0（W-X15a fix 不退化）
- alarm_record 入库 > 0（说明 defect_type 模板不再 100% 拦）

## 不要做
- 不要碰 yk.push URL / token / cert
- 不要改 AlarmRecordService / IgnoreAlarmService 的过滤顺序
- 不要批量塞几百条 defect_type（先小批量验证，能跑通再扩）

## 派工
- 模型：sonnet
- 写入路径：E:\DEMO\数据采集\
- 参考资料：docs/domain/海康视觉接口/、tmp_psm_decompile/、tmp_psm_extracted/

## 完成回执
- defect_type 当前 schema + 行数 + 模板判断字段
- 派生 N 条种子的 PSM 文档/SQL 溯源
- INSERT SQL（含注释）
- 1h 灰盒 5 次快照（T0/T15/T30/T45/T60）
- 报警漏斗：received / not_interesting / is_ignore_hit / yk_push / alarm_record_insert
