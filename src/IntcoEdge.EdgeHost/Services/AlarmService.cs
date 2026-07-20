using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Services;

/// <summary>
/// 报警服务：把入站报警入库，并按需推给英科网关。
/// W-A4：真正写入 SQLite `alarm_record` 表（按 uuid 幂等）。
/// </summary>
public interface IAlarmService
{
    /// <summary>处理一条入站报警：去重 + 入库 + 推英科。</summary>
    /// <returns>已落库 + 已推送的状态摘要。</returns>
    Task<AlarmHandleResult> HandleAlarmAsync(AlarmRecordDto alarm, CancellationToken ct = default);
}

public record class AlarmHandleResult(bool Persisted, bool PushedToYk, string? SkipReason);

public class AlarmService : IAlarmService
{
    private readonly ILogger<AlarmService> _logger;
    private readonly YingkeGatewayClient _ykClient;
    private readonly IAlarmRecordRepository _repo;

    public AlarmService(
        ILogger<AlarmService> logger,
        YingkeGatewayClient ykClient,
        IAlarmRecordRepository repo)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _ykClient = ykClient ?? throw new ArgumentNullException(nameof(ykClient));
        _repo = repo ?? throw new ArgumentNullException(nameof(repo));
    }

    public async Task<AlarmHandleResult> HandleAlarmAsync(AlarmRecordDto alarm, CancellationToken ct = default)
    {
        if (alarm == null)
        {
            throw new ArgumentNullException(nameof(alarm));
        }
        if (string.IsNullOrWhiteSpace(alarm.Uuid))
        {
            throw new ArgumentException("uuid 必填", nameof(alarm));
        }

        _logger.LogInformation(
            "HandleAlarmAsync uuid={Uuid} lineNo={LineNo} faceNo={FaceNo} type={Type} level={Level}",
            alarm.Uuid, alarm.LineNo, alarm.FaceNo, alarm.Type, alarm.Level);

        // DTO → POJO 映射
        var input = new AlarmRecordInput(
            Uuid: alarm.Uuid!,
            Time: alarm.Time ?? string.Empty,
            Type: alarm.Type ?? 0,
            LineNo: alarm.LineNo ?? string.Empty,
            FaceNo: alarm.FaceNo ?? string.Empty,
            Level: alarm.Level ?? 0,
            Message: alarm.Message ?? string.Empty,
            Solve: alarm.Solve ?? 2,
            Reason: alarm.Reason,
            DefectName: alarm.DefectName);

        // 入库（按 uuid 幂等；命中重复返回 0）
        bool persisted;
        try
        {
            var written = _repo.InsertOrIgnore(input);
            persisted = written > 0;
            if (!persisted)
            {
                _logger.LogInformation("HandleAlarmAsync uuid={Uuid} 已存在（去重命中），跳过入库", alarm.Uuid);
            }
        }
        catch (Exception ex)
        {
            // 入库失败抛上去，让 Controller 转 500；不静默吞。
            _logger.LogError(ex, "HandleAlarmAsync 入库失败 uuid={Uuid}", alarm.Uuid);
            throw;
        }

        // 同步推英科：失败也不抛（报警业务是 fire-and-forget）
        bool pushed = false;
        try
        {
            var push = new AlarmPushDto
            {
                Line = alarm.LineNo,
                Face = alarm.FaceNo,
                AlarmTime = alarm.Time,
                AlarmDetails = alarm.Message,
                AlarmCount = 1,
            };
            // W-A6：PushAlarmAsync 现在接收 IReadOnlyList<AlarmPushDto>（批量推送）
            var status = await _ykClient.PushAlarmAsync(new[] { push }, ct).ConfigureAwait(false);
            pushed = status.HasValue && status.Value >= 200 && status.Value < 300;
        }
        catch (System.Net.Http.HttpRequestException ex)
        {
            _logger.LogWarning(ex, "推英科报警失败 uuid={Uuid}", alarm.Uuid);
        }

        return new AlarmHandleResult(persisted, pushed, null);
    }
}
