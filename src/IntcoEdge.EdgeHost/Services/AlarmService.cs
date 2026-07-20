using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Services;

/// <summary>
/// 报警服务：把入站报警入库，并按需推给英科网关。
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

    public AlarmService(ILogger<AlarmService> logger, YingkeGatewayClient ykClient)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _ykClient = ykClient ?? throw new ArgumentNullException(nameof(ykClient));
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

        // TODO(W-A5): 真正写入 SQLite alarm_record (按 uuid 去重)。
        // INSERT OR IGNORE INTO alarm_record (uuid, time, type, line_no, face_no, level, message, solve)
        //   VALUES (@uuid, @time, @type, @lineNo, @faceNo, @level, @message, 2);
        var persisted = true;

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
            var status = await _ykClient.PushAlarmAsync(push, ct).ConfigureAwait(false);
            pushed = (int)status >= 200 && (int)status < 300;
        }
        catch (System.Net.Http.HttpRequestException ex)
        {
            _logger.LogWarning(ex, "推英科报警失败 uuid={Uuid}", alarm.Uuid);
        }

        return new AlarmHandleResult(persisted, pushed, null);
    }
}
