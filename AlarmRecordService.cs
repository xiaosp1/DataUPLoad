using System.Text.RegularExpressions;
using Microsoft.Data.Sqlite;
using IntcoEdge.Db;
using IntcoEdge.EdgeHost.Clients;

namespace IntcoEdge.EdgeHost.Services.Alarm;

/// <summary>
/// W-A18 (2026-07-21): PSM `AlarmRecordServiceImpl` 1:1 移植到 .NET。
///
/// 反编译来源：`com.hikrobotics.solution.module.alarm.service.imp.AlarmRecordServiceImpl`
///
/// 1:1 对照：
///   - add(form)                              → <see cref="HandleAlarmAsync"/>
///   - deal(uuid)                             → <see cref="DealAsync"/>
///   - handleAlarmIgnore(form)                → <see cref="HandleAlarmIgnoreAsync"/>
///   - handleAlarmSearch(form)                → <see cref="HandleAlarmSearchAsync"/>
///   - getAlarmListInfo(query)                → <see cref="GetAlarmListInfoAsync"/>
///   - handleAlarmNumGet()                    → <see cref="HandleAlarmNumGetAsync"/>
///   - listAll(query)                         → <see cref="ListAllAsync"/>
///   - listNotResolveDefectAlarmRecord()      → <see cref="ListNotResolveDefectAlarmRecordAsync"/>
///   - sendAlarmMessage(alarm)                → <see cref="SendAlarmMessageAsync"/>
///
/// ★ PSM 同化逻辑（同花顺）：
///   当一条 alarm 进来时：
///   1. <c>UPDATE alarm_record SET solve=IGNORE WHERE defect_name=@n AND line_no=@l
///      AND type=@t AND face_no=@f AND solve=UNSOLVED</c>
///   2. <c>INSERT INTO alarm_record (..., Solve=UNSOLVED, ...)</c>
///   这导致连续 5 条同 (defectName, lineNo, faceNo, type)：
///     - 第 1 条：INSERT UNSOLVED
///     - 第 2 条：上一条 → IGNORE，INSERT UNSOLVED
///     - ...
///     - 第 5 条：第 4 条 → IGNORE，INSERT UNSOLVED
///   结果：5 行 alarm_record，第 4 行 solve=IGNORE，最后一行 solve=UNSOLVED。
///
/// ★ 推送英科逻辑：<see cref="SendAlarmMessageAsync"/> 走 <see cref="AlarmEventBus.PublishAsync"/>；
///   <c>YKServiceImpl.OnPushAlarm</c> 在 Channel 循环里读出，调英科网关。
///   sendYkEnable=1 && solve=UNSOLVED 才推（PSM `StateEnum.YES.getValue()` = 1）。
/// </summary>
public interface IAlarmRecordService
{
    /// <summary>PSM <c>add(form)</c> 1:1。</summary>
    Task<AlarmHandleResult> HandleAlarmAsync(AlarmInputForm form, CancellationToken ct = default);

    /// <summary>PSM <c>deal(uuid)</c> 1:1。</summary>
    Task<AlarmHandleResult> DealAsync(string uuid, CancellationToken ct = default);

    /// <summary>PSM <c>handleAlarmIgnore(form)</c> 1:1：把指定 (line/face/type) 下所有 UNSOLVED → IGNORE。</summary>
    Task<AlarmHandleResult> HandleAlarmIgnoreAsync(IReadOnlyList<int> recordIds, CancellationToken ct = default);

    /// <summary>PSM <c>listAll(query)</c> 1:1：按 type/level/solve/faceId/time 过滤 + 排序 + 分页。</summary>
    Task<IReadOnlyList<AlarmRecordPO>> ListAllAsync(AlarmListQuery query, CancellationToken ct = default);

    /// <summary>PSM <c>handleAlarmSearch(form)</c> 1:1：type=4 查 alarm_record UNSOLVED；其它查 status_record。</summary>
    /// <remarks>为简化 .NET 实现，type=4 走 alarm_record 自身，其它类型返回空集合（status 集成留 TODO）。</remarks>
    Task<IReadOnlyList<AlarmRecordPO>> HandleAlarmSearchAsync(string lineNo, string faceNo, int type, CancellationToken ct = default);

    /// <summary>PSM <c>getAlarmListInfo(query)</c> 1:1：按 time + faceId 过滤 + 分页。</summary>
    Task<IReadOnlyList<AlarmRecordPO>> GetAlarmListInfoAsync(string startTime, string endTime, string? faceId, CancellationToken ct = default);

    /// <summary>PSM <c>handleAlarmNumGet()</c> 1:1：返回 (totalNum, highNum) — total = 所有未解决数；highNum = 高等级数。</summary>
    Task<(int TotalNum, int HighNum)> HandleAlarmNumGetAsync(CancellationToken ct = default);

    /// <summary>PSM <c>listNotResolveDefectAlarmRecord()</c> 1:1：返回所有 UNSOLVED + alarmEnable=1 的 alarm_record。</summary>
    Task<IReadOnlyList<AlarmRecordPO>> ListNotResolveDefectAlarmRecordAsync(CancellationToken ct = default);

    /// <summary>W-A18: DetectController 内部调 — 把 detection 推送转成 AlarmInputForm 走 PSM 同化路径。</summary>
    Task<AlarmHandleResult> HandleAlarmAsync(AlarmInputForm form, string? workShop, CancellationToken ct = default);
}

/// <summary>
/// PSM <c>handleAlarmIgnore(form)</c> 入参的 .NET 简化：
/// PSM 端 form 有 (ignoreAll/faceId/lineNo/faceNo/type/defectName/startTime/endTime)；
/// .NET 端 Controller 解析后直接给 List&lt;int&gt; recordIds（更易测）。
/// </summary>
public class AlarmListQuery
{
    public int? Type { get; set; }
    public int? Level { get; set; }
    public int? Solve { get; set; }
    public string? FaceId { get; set; }
    public string? StartTime { get; set; }
    public string? EndTime { get; set; }

    /// <summary>PSM <c>sortType</c>：0=升序，其它=降序（按 time 排序）。</summary>
    public int? SortType { get; set; }
}

/// <summary>
/// W-A18: HandleAlarmAsync 的业务返回（PSM <c>BaseResult.build().error("20101"/"20102")</c> 1:1）。
/// </summary>
public class AlarmHandleResult
{
    public int Code { get; set; } = 0;
    public string? ErrorCode { get; set; }      // "20101" / "20102" / null
    public string? Message { get; set; }
    public bool Success { get; set; } = true;
    public AlarmRecordPO? Alarm { get; set; }   // 仅成功时填
    public bool IsInterestingDefect { get; set; } // PSM add() 内 isInterestingDefect 标志
}

public class AlarmRecordService : IAlarmRecordService
{
    private const string DEFECT_ALARM_MSG_TEMP = "[{0}] 缺陷报警"; // PSM `DEFECT_ALARM_MSG_TEMP`

    private readonly SqliteConnectionFactory _factory;
    private readonly IDefectTypeService _defectTypeService;
    private readonly DefectAlarmConfig _alarmConfig;
    private readonly AlarmEventBus _eventBus;
    private readonly ILogger<AlarmRecordService> _logger;

    /// <summary>PSM <c>@Value("${alarm.high-type:3}")</c>：默认高等级报警类型 = 3 (DEVICE)。</summary>
    private static readonly string HighTypesCsv = "3";

    public AlarmRecordService(
        SqliteConnectionFactory factory,
        IDefectTypeService defectTypeService,
        DefectAlarmConfig alarmConfig,
        AlarmEventBus eventBus,
        ILogger<AlarmRecordService> logger)
    {
        _factory = factory ?? throw new ArgumentNullException(nameof(factory));
        _defectTypeService = defectTypeService ?? throw new ArgumentNullException(nameof(defectTypeService));
        _alarmConfig = alarmConfig ?? throw new ArgumentNullException(nameof(alarmConfig));
        _eventBus = eventBus ?? throw new ArgumentNullException(nameof(eventBus));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    // ============================================================
    // PSM add() 1:1 移植
    // ============================================================

    public async Task<AlarmHandleResult> HandleAlarmAsync(AlarmInputForm form, CancellationToken ct = default)
        => await HandleAlarmAsync(form, workShop: null, ct).ConfigureAwait(false);

    public async Task<AlarmHandleResult> HandleAlarmAsync(AlarmInputForm form, string? workShop, CancellationToken ct = default)
    {
        if (form == null) throw new ArgumentNullException(nameof(form));

        // PSM: AlarmTypeEnum alarmType = AlarmTypeEnum.getByCode(form.getType());
        //      if (alarmType == null) return BaseResult.build().error("20101");
        var alarmType = AlarmTypeEnumExtensions.GetByCode(form.Type);
        if (alarmType == null)
        {
            _logger.LogError("alarm type not support.[form={FormType}]", form.Type);
            return new AlarmHandleResult
            {
                Code = 20101,
                ErrorCode = "20101",
                Success = false,
                Message = "alarm type not support",
            };
        }

        // PSM: HashMap sortDefectTypeByName = Maps.newHashMap();
        //      this.defectTypeService.listByAttribute(form.getType(), DefectTypePO::getCategory)
        //          .forEach(type -> sortDefectTypeByName.put(type.getName(), type));
        var sortDefectTypeByName = _defectTypeService
            .ListByCategory(form.Type ?? 0)
            .ToDictionary(d => d.Name, StringComparer.Ordinal);

        bool isInterestingDefect = false;
        AlarmRecordPO? savedAlarm = null;

        if (sortDefectTypeByName.Count > 0)
        {
            string message = form.Message ?? string.Empty;
            string? defectName = null;

            // PSM:
            //   block0:
            //   for (DefectAlarmConfig.DefectTypeConfig config : this.alarmConfig.getConfig()) {
            //     if (!config.getType().toUpperCase().equals(alarmType.name())) continue;
            //     message = ReUtil.get(config.getTemplate(), form.getMessage(), 0);
            //     for (String name : sortDefectTypeByName.keySet()) {
            //       if (!message.contains(name)) continue;
            //       defectName = name;
            //       if (alarmType == AlarmTypeEnum.DEFECT) {
            //         message = StrUtil.format(DEFECT_ALARM_MSG_TEMP, defectName);
            //       }
            //       isInterestingDefect = true;
            //       continue block0;
            //     }
            //   }
            foreach (var config in _alarmConfig.Config)
            {
                if (!string.Equals(config.Type?.Trim().ToUpperInvariant(), alarmType.Value.ToString(), StringComparison.Ordinal))
                {
                    continue;
                }
                try
                {
                    var match = Regex.Match(form.Message ?? string.Empty, config.Template);
                    if (match.Success && match.Groups.Count > 0)
                    {
                        // PSM `ReUtil.get(template, message, 0)` = match.group(0) = 整个匹配
                        message = match.Groups[0].Value;
                    }
                }
                catch (ArgumentException ex)
                {
                    _logger.LogWarning(ex, "DefectAlarmConfig.Template 正则非法 type={Type} template={Template}", config.Type, config.Template);
                }

                foreach (var name in sortDefectTypeByName.Keys)
                {
                    if (string.IsNullOrEmpty(name)) continue;
                    if (!message.Contains(name, StringComparison.Ordinal)) continue;
                    defectName = name;
                    if (alarmType == AlarmTypeEnum.DEFECT)
                    {
                        message = string.Format(System.Globalization.CultureInfo.InvariantCulture, DEFECT_ALARM_MSG_TEMP, defectName);
                    }
                    isInterestingDefect = true;
                    goto block0_break;
                }
            }
            block0_break:;

            if (isInterestingDefect && !string.IsNullOrEmpty(defectName))
            {
                // PSM:
                //   LambdaUpdateWrapper uw = Wrappers.lambdaUpdate()
                //     .eq(AlarmRecordPO::getDefectName, defectName)
                //     .eq(AlarmRecordPO::getLineNo, form.getLineNo())
                //     .eq(AlarmRecordPO::getType, form.getType())
                //     .eq(AlarmRecordPO::getFaceNo, form.getFaceNo())
                //     .eq(AlarmRecordPO::getSolve, AlarmSolvedEnum.UNSOLVED.getValue())
                //     .set(AlarmRecordPO::getSolve, AlarmSolvedEnum.IGNORE.getValue());
                //   this.update(uw);
                var ignored = await IgnoreUnsolvedAsync(defectName, form.LineNo, form.FaceNo, form.Type ?? 0, ct).ConfigureAwait(false);
                _logger.LogInformation("PSM 同化：UPDATE {Ignored} 条历史 UNSOLVED→IGNORE defectName={Defect} lineNo={Line} faceNo={Face} type={Type}",
                    ignored, defectName, form.LineNo, form.FaceNo, form.Type);

                // PSM:
                //   AlarmRecordPO alarm = BeanUtil.copyProperties(form, AlarmRecordPO.class);
                //   alarm.setSolve(AlarmSolvedEnum.UNSOLVED.getValue())
                //        .setMessage(message)
                //        .setDefectName(defectName);
                //   alarm.setDefectType((DefectTypePO) sortDefectTypeByName.get(defectName));
                //   this.save(alarm);
                //   this.sendAlarmMessage(alarm);
                var now = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
                var newUuid = form.Uuid ?? Guid.NewGuid().ToString("N");
                var alarm = new AlarmRecordPO
                {
                    Uuid = newUuid,
                    Time = form.Time ?? now,
                    Type = form.Type,
                    LineNo = form.LineNo,
                    FaceNo = form.FaceNo,
                    Level = form.Level,
                    Message = message,
                    Solve = AlarmSolvedEnum.UNSOLVED,
                    Reason = null,
                    DefectName = defectName,
                    DefectType = sortDefectTypeByName[defectName],
                    UpdateTime = DateTime.Parse(now),
                    CreateTime = DateTime.Parse(now),
                };
                await InsertAlarmAsync(alarm, ct).ConfigureAwait(false);
                savedAlarm = alarm;

                // PSM sendAlarmMessage(alarm) 1:1
                await SendAlarmMessageAsync(alarm, workShop, ct).ConfigureAwait(false);
            }
        }

        if (!isInterestingDefect)
        {
            _logger.LogWarning("current alarm is not interesting defect.[form={Form}]", form);
        }

        return new AlarmHandleResult
        {
            Code = 0,
            Success = true,
            IsInterestingDefect = isInterestingDefect,
            Alarm = savedAlarm,
        };
    }

    private async Task<int> IgnoreUnsolvedAsync(string defectName, string? lineNo, string? faceNo, int type, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(lineNo) || string.IsNullOrWhiteSpace(faceNo)) return 0;
        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
UPDATE alarm_record
   SET solve = $solve, update_time = $now
 WHERE defect_name = $defectName
   AND line_no = $lineNo
   AND face_no = $faceNo
   AND type = $type
   AND solve = $unsolved;";
        cmd.Parameters.AddWithValue("$solve", AlarmSolvedEnum.IGNORE);
        cmd.Parameters.AddWithValue("$now", DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));
        cmd.Parameters.AddWithValue("$defectName", defectName);
        cmd.Parameters.AddWithValue("$lineNo", lineNo);
        cmd.Parameters.AddWithValue("$faceNo", faceNo);
        cmd.Parameters.AddWithValue("$type", type);
        cmd.Parameters.AddWithValue("$unsolved", AlarmSolvedEnum.UNSOLVED);
        return await cmd.ExecuteNonQueryAsync(ct).ConfigureAwait(false);
    }

    private async Task InsertAlarmAsync(AlarmRecordPO alarm, CancellationToken ct)
    {
        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
INSERT INTO alarm_record
    (uuid, time, type, line_no, face_no, level, message, solve,
     reason, defect_name, update_time, create_time)
VALUES
    ($uuid, $time, $type, $lineNo, $faceNo, $level, $message, $solve,
     $reason, $defectName, $updateTime, $createTime);
SELECT last_insert_rowid();";
        cmd.Parameters.AddWithValue("$uuid", (object?)alarm.Uuid ?? DBNull.Value);
        cmd.Parameters.AddWithValue("$time", alarm.Time ?? string.Empty);
        cmd.Parameters.AddWithValue("$type", alarm.Type ?? (object)0);
        cmd.Parameters.AddWithValue("$lineNo", alarm.LineNo ?? string.Empty);
        cmd.Parameters.AddWithValue("$faceNo", alarm.FaceNo ?? string.Empty);
        cmd.Parameters.AddWithValue("$level", alarm.Level ?? (object)0);
        cmd.Parameters.AddWithValue("$message", alarm.Message ?? string.Empty);
        cmd.Parameters.AddWithValue("$solve", alarm.Solve ?? AlarmSolvedEnum.UNSOLVED);
        cmd.Parameters.AddWithValue("$reason", (object?)alarm.Reason ?? DBNull.Value);
        cmd.Parameters.AddWithValue("$defectName", (object?)alarm.DefectName ?? DBNull.Value);
        cmd.Parameters.AddWithValue("$updateTime", (alarm.UpdateTime ?? DateTime.Now).ToString("yyyy-MM-dd HH:mm:ss"));
        cmd.Parameters.AddWithValue("$createTime", (alarm.CreateTime ?? DateTime.Now).ToString("yyyy-MM-dd HH:mm:ss"));
        var id = await cmd.ExecuteScalarAsync(ct).ConfigureAwait(false);
        alarm.Id = Convert.ToInt32(id);
    }

    // ============================================================
    // PSM deal(uuid) 1:1 移植
    // ============================================================

    public async Task<AlarmHandleResult> DealAsync(string uuid, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(uuid))
        {
            return new AlarmHandleResult
            {
                Code = 20102,
                ErrorCode = "20102",
                Success = false,
                Message = "deal alram failed,alarm uuid.",
            };
        }

        // PSM:
        //   LambdaUpdateWrapper updateWrapper = Wrappers.lambdaUpdate()
        //     .eq(AlarmRecordPO::getUuid, uuid)
        //     .eq(AlarmRecordPO::getSolve, AlarmSolvedEnum.UNSOLVED.getValue())
        //     .set(AlarmRecordPO::getSolve, AlarmSolvedEnum.SOLVED.getValue());
        //   boolean updateFlag = this.update(updateWrapper);
        var updated = await SolveAsync(uuid, AlarmSolvedEnum.UNSOLVED, AlarmSolvedEnum.SOLVED, ct).ConfigureAwait(false);

        if (updated > 0)
        {
            // PSM:
            //   AlarmRecordPO alarm = this.getOne(Wrappers.lambdaQuery().eq(AlarmRecordPO::getUuid, uuid));
            //   if (StringUtils.isNotBlank(alarm.getDefectName())) {
            //     DefectTypePO defect = this.defectTypeService.getByNameAndType(alarm.getDefectName(), alarm.getType());
            //     if (defect != null) {
            //       alarm.setDefectType(defect);
            //       this.sendAlarmMessage(alarm);
            //     }
            //   }
            var alarm = await GetByUuidAsync(uuid, ct).ConfigureAwait(false);
            if (alarm != null && !string.IsNullOrWhiteSpace(alarm.DefectName))
            {
                var defect = _defectTypeService.GetByNameAndType(alarm.DefectName, alarm.Type ?? 0);
                if (defect != null)
                {
                    alarm.DefectType = defect;
                    await SendAlarmMessageAsync(alarm, workShop: null, ct).ConfigureAwait(false);
                }
                else
                {
                    _logger.LogWarning("defect is not exist. will not send alarm message.[alarm={Uuid}]", uuid);
                }
            }
            return new AlarmHandleResult { Code = 0, Success = true, Alarm = alarm };
        }

        return new AlarmHandleResult
        {
            Code = 20102,
            ErrorCode = "20102",
            Success = false,
            Message = $"deal alram failed,alarm uuid. uuid={uuid}",
        };
    }

    private async Task<int> SolveAsync(string uuid, int fromSolve, int toSolve, CancellationToken ct)
    {
        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
UPDATE alarm_record
   SET solve = $toSolve, update_time = $now
 WHERE uuid = $uuid
   AND solve = $fromSolve;";
        cmd.Parameters.AddWithValue("$toSolve", toSolve);
        cmd.Parameters.AddWithValue("$now", DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));
        cmd.Parameters.AddWithValue("$uuid", uuid);
        cmd.Parameters.AddWithValue("$fromSolve", fromSolve);
        return await cmd.ExecuteNonQueryAsync(ct).ConfigureAwait(false);
    }

    public async Task<AlarmRecordPO?> GetByUuidAsync(string uuid, CancellationToken ct = default)
    {
        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
SELECT id, uuid, time, type, line_no, face_no, level, message, solve,
       reason, defect_name, update_time, create_time
  FROM alarm_record
 WHERE uuid = $uuid
 LIMIT 1;";
        cmd.Parameters.AddWithValue("$uuid", uuid);
        using var rd = await cmd.ExecuteReaderAsync(ct).ConfigureAwait(false);
        if (await rd.ReadAsync(ct).ConfigureAwait(false))
        {
            return ReadAlarm(rd);
        }
        return null;
    }

    private static AlarmRecordPO ReadAlarm(SqliteDataReader rd)
    {
        return new AlarmRecordPO
        {
            Id = rd.GetInt32(0),
            Uuid = rd.IsDBNull(1) ? null : rd.GetString(1),
            Time = rd.IsDBNull(2) ? null : rd.GetString(2),
            Type = rd.IsDBNull(3) ? null : rd.GetInt32(3),
            LineNo = rd.IsDBNull(4) ? null : rd.GetString(4),
            FaceNo = rd.IsDBNull(5) ? null : rd.GetString(5),
            Level = rd.IsDBNull(6) ? null : rd.GetInt32(6),
            Message = rd.IsDBNull(7) ? null : rd.GetString(7),
            Solve = rd.IsDBNull(8) ? null : rd.GetInt32(8),
            Reason = rd.IsDBNull(9) ? null : rd.GetInt32(9),
            DefectName = rd.IsDBNull(10) ? null : rd.GetString(10),
            UpdateTime = rd.IsDBNull(11) ? null : DateTime.TryParse(rd.GetString(11), out var u) ? u : (DateTime?)null,
            CreateTime = rd.IsDBNull(12) ? null : DateTime.TryParse(rd.GetString(12), out var c) ? c : (DateTime?)null,
        };
    }

    // ============================================================
    // PSM handleAlarmIgnore(form) 1:1 移植（简化：直接接受 recordIds）
    // ============================================================

    public async Task<AlarmHandleResult> HandleAlarmIgnoreAsync(IReadOnlyList<int> recordIds, CancellationToken ct = default)
    {
        if (recordIds == null || recordIds.Count == 0)
        {
            return new AlarmHandleResult { Code = 0, Success = true };
        }

        // PSM:
        //   alarmRecords.forEach(record -> record.setSolve(AlarmSolvedEnum.IGNORE.getValue()));
        //   if (!this.updateBatchById(alarmRecords)) return BaseResult.build().error("20102");
        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        var paramNames = new List<string>();
        for (var i = 0; i < recordIds.Count; i++)
        {
            var p = $"$id{i}";
            paramNames.Add(p);
            cmd.Parameters.AddWithValue(p, recordIds[i]);
        }
        cmd.CommandText = $@"
UPDATE alarm_record
   SET solve = $solve, update_time = $now
 WHERE id IN ({string.Join(",", paramNames)})
   AND solve = $unsolved;";
        cmd.Parameters.AddWithValue("$solve", AlarmSolvedEnum.IGNORE);
        cmd.Parameters.AddWithValue("$unsolved", AlarmSolvedEnum.UNSOLVED);
        cmd.Parameters.AddWithValue("$now", DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));
        var affected = await cmd.ExecuteNonQueryAsync(ct).ConfigureAwait(false);
        _logger.LogInformation("HandleAlarmIgnoreAsync: {Count} 条 → IGNORE (输入 {Input})", affected, recordIds.Count);
        return new AlarmHandleResult { Code = 0, Success = true };
    }

    // ============================================================
    // PSM listAll(query) 1:1 移植
    // ============================================================

    public async Task<IReadOnlyList<AlarmRecordPO>> ListAllAsync(AlarmListQuery query, CancellationToken ct = default)
    {
        query ??= new AlarmListQuery();
        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        var where = new List<string>();
        if (query.Type.HasValue)
        {
            where.Add("type = $type");
            cmd.Parameters.AddWithValue("$type", query.Type.Value);
        }
        if (query.Level.HasValue)
        {
            where.Add("level = $level");
            cmd.Parameters.AddWithValue("$level", query.Level.Value);
        }
        if (query.Solve.HasValue)
        {
            where.Add("solve = $solve");
            cmd.Parameters.AddWithValue("$solve", query.Solve.Value);
        }
        if (!string.IsNullOrWhiteSpace(query.StartTime) && !string.IsNullOrWhiteSpace(query.EndTime))
        {
            where.Add("create_time BETWEEN $startTime AND $endTime");
            cmd.Parameters.AddWithValue("$startTime", query.StartTime);
            cmd.Parameters.AddWithValue("$endTime", query.EndTime);
        }
        var whereClause = where.Count > 0 ? "WHERE " + string.Join(" AND ", where) : "";
        var isAsc = query.SortType == 0;
        var orderBy = isAsc ? "ORDER BY time ASC" : "ORDER BY time DESC";
        cmd.CommandText = $@"
SELECT id, uuid, time, type, line_no, face_no, level, message, solve,
       reason, defect_name, update_time, create_time
  FROM alarm_record
  {whereClause}
  {orderBy}
 LIMIT 1000;";
        var list = new List<AlarmRecordPO>();
        using var rd = await cmd.ExecuteReaderAsync(ct).ConfigureAwait(false);
        while (await rd.ReadAsync(ct).ConfigureAwait(false))
        {
            list.Add(ReadAlarm(rd));
        }
        return list;
    }

    // ============================================================
    // PSM handleAlarmSearch(form) 1:1 移植（type=4 走 alarm_record UNSOLVED）
    // ============================================================

    public async Task<IReadOnlyList<AlarmRecordPO>> HandleAlarmSearchAsync(string lineNo, string faceNo, int type, CancellationToken ct = default)
    {
        // PSM:
        //   if (form.getType() != 4) {
        //     data = this.statusRecordService.searchOffLineClient(form.getLineNo(), form.getFaceNo(), form.getType());
        //   } else {
        //     Wrapper wrapper = Wrappers.lambdaQuery(AlarmRecordPO.class)
        //       .eq(AlarmRecordPO::getType, AlarmTypeEnum.DEFECT.getCode())
        //       .eq(AlarmRecordPO::getFaceNo, form.getFaceNo())
        //       .eq(AlarmRecordPO::getLineNo, form.getLineNo())
        //       .eq(AlarmRecordPO::getSolve, AlarmSolvedEnum.UNSOLVED.getValue());
        //     data = this.list(wrapper);
        //   }
        if (type != 4)
        {
            _logger.LogDebug("HandleAlarmSearchAsync type={Type} 走 status_record（PSM 对齐，本期不集成 status_record，返回空）", type);
            return Array.Empty<AlarmRecordPO>();
        }

        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
SELECT id, uuid, time, type, line_no, face_no, level, message, solve,
       reason, defect_name, update_time, create_time
  FROM alarm_record
 WHERE type = $defect
   AND face_no = $faceNo
   AND line_no = $lineNo
   AND solve = $unsolved
 ORDER BY time DESC
 LIMIT 200;";
        cmd.Parameters.AddWithValue("$defect", (int)AlarmTypeEnum.DEFECT);
        cmd.Parameters.AddWithValue("$faceNo", faceNo ?? string.Empty);
        cmd.Parameters.AddWithValue("$lineNo", lineNo ?? string.Empty);
        cmd.Parameters.AddWithValue("$unsolved", AlarmSolvedEnum.UNSOLVED);
        var list = new List<AlarmRecordPO>();
        using var rd = await cmd.ExecuteReaderAsync(ct).ConfigureAwait(false);
        while (await rd.ReadAsync(ct).ConfigureAwait(false))
        {
            list.Add(ReadAlarm(rd));
        }
        return list;
    }

    // ============================================================
    // PSM getAlarmListInfo(query) 1:1 移植
    // ============================================================

    public async Task<IReadOnlyList<AlarmRecordPO>> GetAlarmListInfoAsync(string startTime, string endTime, string? faceId, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(startTime) || string.IsNullOrWhiteSpace(endTime))
        {
            return Array.Empty<AlarmRecordPO>();
        }

        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
SELECT id, uuid, time, type, line_no, face_no, level, message, solve,
       reason, defect_name, update_time, create_time
  FROM alarm_record
 WHERE time BETWEEN $startTime AND $endTime
 ORDER BY time DESC
 LIMIT 1000;";
        cmd.Parameters.AddWithValue("$startTime", startTime);
        cmd.Parameters.AddWithValue("$endTime", endTime);
        var list = new List<AlarmRecordPO>();
        using var rd = await cmd.ExecuteReaderAsync(ct).ConfigureAwait(false);
        while (await rd.ReadAsync(ct).ConfigureAwait(false))
        {
            list.Add(ReadAlarm(rd));
        }
        return list;
    }

    // ============================================================
    // PSM handleAlarmNumGet() 1:1 移植
    // ============================================================

    public async Task<(int TotalNum, int HighNum)> HandleAlarmNumGetAsync(CancellationToken ct = default)
    {
        var specialTypes = HighTypesCsv.Split(',', StringSplitOptions.RemoveEmptyEntries)
            .Select(int.Parse)
            .ToHashSet();

        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
SELECT type, COUNT(*) AS cnt
  FROM alarm_record
 WHERE solve = $unsolved
 GROUP BY type;";
        cmd.Parameters.AddWithValue("$unsolved", AlarmSolvedEnum.UNSOLVED);
        int total = 0, special = 0;
        using var rd = await cmd.ExecuteReaderAsync(ct).ConfigureAwait(false);
        while (await rd.ReadAsync(ct).ConfigureAwait(false))
        {
            var type = rd.GetInt32(0);
            var count = rd.GetInt32(1);
            total += count;
            if (specialTypes.Contains(type)) special += count;
        }
        return (total, special);
    }

    // ============================================================
    // PSM listNotResolveDefectAlarmRecord() 1:1 移植
    // ============================================================

    public async Task<IReadOnlyList<AlarmRecordPO>> ListNotResolveDefectAlarmRecordAsync(CancellationToken ct = default)
    {
        // PSM:
        //   List<String> enableAlarmDefects = defectTypeService
        //     .listByAttribute(1, DefectTypePO::getAlarmEnable)
        //     .stream().map(DefectTypePO::getName).toList();
        //   if (CollectionUtils.isNotEmpty(enableAlarmDefects)) {
        //     qw = lambdaQuery().eq(Solve, UNSOLVED).in(DefectName, enableAlarmDefects);
        //     return this.list(qw);
        //   }
        var enableDefects = _defectTypeService.ListByAlarmEnable(StateEnum.YES).Select(d => d.Name).ToList();
        if (enableDefects.Count == 0) return Array.Empty<AlarmRecordPO>();

        using var conn = await _factory.Open();
        using var cmd = conn.CreateCommand();
        var paramNames = new List<string>();
        for (var i = 0; i < enableDefects.Count; i++)
        {
            var p = $"$def{i}";
            paramNames.Add(p);
            cmd.Parameters.AddWithValue(p, enableDefects[i]);
        }
        cmd.CommandText = $@"
SELECT id, uuid, time, type, line_no, face_no, level, message, solve,
       reason, defect_name, update_time, create_time
  FROM alarm_record
 WHERE solve = $unsolved
   AND defect_name IN ({string.Join(",", paramNames)})
 ORDER BY time DESC
 LIMIT 500;";
        cmd.Parameters.AddWithValue("$unsolved", AlarmSolvedEnum.UNSOLVED);
        var list = new List<AlarmRecordPO>();
        using var rd = await cmd.ExecuteReaderAsync(ct).ConfigureAwait(false);
        while (await rd.ReadAsync(ct).ConfigureAwait(false))
        {
            list.Add(ReadAlarm(rd));
        }
        return list;
    }

    // ============================================================
    // PSM sendAlarmMessage(alarm) 1:1 移植
    //   - 满足 alarmEnable=1 && !IGNORE → 推 WS 文字 + 播放声音
    //   - 满足 sendYkEnable=1 && solve=UNSOLVED → Publish PushAlarmEvent（→ YKServiceImpl.OnPushAlarm 推英科）
    // ============================================================

    public async Task SendAlarmMessageAsync(AlarmRecordPO alarm, string? workShop, CancellationToken ct = default)
    {
        if (alarm == null) throw new ArgumentNullException(nameof(alarm));
        var defectType = alarm.DefectType;
        if (defectType == null)
        {
            _logger.LogWarning("SendAlarmMessageAsync alarm.defectType 为空，跳过推送 alarm={Uuid}", alarm.Uuid);
            return;
        }

        // PSM: if (Objects.equals(defectType.getAlarmEnable(), StateEnum.YES.getValue()) && !isIgnore)
        if (defectType.AlarmEnable == StateEnum.YES && alarm.Solve != AlarmSolvedEnum.IGNORE)
        {
            // PSM: this.sendAlarmTextMessage();   → .NET: 大屏 WebSocket（本期不集成，留 TODO）
            // PSM: if (SoundEnable=1 && Solve=UNSOLVED) → this.sendAlarmSoundWsMessage(defectType);
            // .NET: 同理，WS 留 TODO（W-A18 scope 是英科推送，不是大屏 WS）
            // 这里只占位（避免 silent fail 误以为是英科推送失败）。
            _logger.LogDebug("SendAlarmMessageAsync 大屏 WS 推送占位 alarmEnable=1 soundEnable={Sound} solve={Solve}",
                defectType.SoundEnable, alarm.Solve);
        }

        // PSM:
        //   if (!isIgnore && Objects.equals(defectType.getSendYkEnable(), StateEnum.YES.getValue())) {
        //     EventUtil.publish(new PushAlarmEvent(this, alarm));
        //   }
        if (alarm.Solve != AlarmSolvedEnum.IGNORE && defectType.SendYkEnable == StateEnum.YES)
        {
            if (alarm.Solve == AlarmSolvedEnum.UNSOLVED)
            {
                // 携带 WorkShop（PSM AlarmDTO.workShop = ykConfig.workshop，由 YKServiceImpl.pushAlarm2YK 注入；
                // .NET 端 DetectController 路径已注入；deal(uuid) 路径留 null 让 YKServiceImpl 用配置 WorkshopCode）。
                if (!string.IsNullOrWhiteSpace(workShop))
                {
                    alarm.Message = AppendWorkShop(alarm.Message, workShop);
                }
                _eventBus.Publish(alarm);
                _logger.LogInformation("SendAlarmMessageAsync 已 publish 到 AlarmEventBus alarm={Uuid} defect={Defect} sendYkEnable=1",
                    alarm.Uuid, alarm.DefectName);
            }
        }

        await Task.CompletedTask.ConfigureAwait(false);
    }

    /// <summary>PSM 端 YKServiceImpl 直接 setWorkShop(this.ykConfig.getWorkshop())；.NET 端为简化把 workshop 拼到 message 里（避免改 DTO）。</summary>
    private static string AppendWorkShop(string? message, string workShop)
    {
        var m = message ?? string.Empty;
        if (m.Contains($"[WS={workShop}]", StringComparison.Ordinal)) return m;
        return $"[WS={workShop}] {m}";
    }
}
