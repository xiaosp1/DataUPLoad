using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Services;

/// <summary>
/// 英科网关业务服务：
///   - 登录拿 ticket（带缓存）
///   - 推送报警
///   - 产线-缺陷字典查询（占位：W-A5 完成后从 PSM 取）
///
/// Controllers 只调本服务，不直接动 YingkeGatewayClient，便于切实现 + 集中日志。
/// </summary>
public interface IYingkeService
{
    /// <summary>英科登录，返回用户信息（不返回 ticket，ticket 在内部缓存）。</summary>
    Task<YkLoginResponse?> LoginAsync(CancellationToken ct = default);

    /// <summary>取 ticket（懒加载 + 缓存）。</summary>
    Task<(string? Ticket, int? InvOrgId)> GetTicketAsync(CancellationToken ct = default);

    /// <summary>推送报警到英科网关（自动管 ticket + 批量）。</summary>
    /// <returns>英科业务 code（200/400）。通道失败返回 null。</returns>
    Task<int?> PushAlarmAsync(IReadOnlyList<AlarmPushDto> alarms, CancellationToken ct = default);

    /// <summary>英科产线-缺陷字典（占位：PSM 端 `LineAndDefectDTO` 的查询接口细节待补）。</summary>
    Task<IReadOnlyList<string>> GetLineDefectDictionaryAsync(string workshopCode, CancellationToken ct = default);
}

public class YingkeService : IYingkeService
{
    private readonly YingkeGatewayClient _ykClient;
    private readonly ILogger<YingkeService> _logger;

    public YingkeService(YingkeGatewayClient ykClient, ILogger<YingkeService> logger)
    {
        _ykClient = ykClient ?? throw new ArgumentNullException(nameof(ykClient));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public Task<YkLoginResponse?> LoginAsync(CancellationToken ct = default)
    {
        _logger.LogInformation("YingkeService.LoginAsync");
        return _ykClient.LoginAsync(ct);
    }

    public Task<(string? Ticket, int? InvOrgId)> GetTicketAsync(CancellationToken ct = default)
        => _ykClient.GetTicketAsync(ct);

    public async Task<int?> PushAlarmAsync(IReadOnlyList<AlarmPushDto> alarms, CancellationToken ct = default)
    {
        if (alarms == null || alarms.Count == 0)
        {
            return null;
        }

        _logger.LogInformation("YingkeService.PushAlarmAsync count={Count}", alarms.Count);
        var code = await _ykClient.PushAlarmAsync(alarms, ct).ConfigureAwait(false);

        if (code == 200)
        {
            _logger.LogInformation("YingkeService.PushAlarmAsync 成功 count={Count}", alarms.Count);
        }
        else
        {
            _logger.LogWarning("YingkeService.PushAlarmAsync 业务失败 code={Code} count={Count}",
                code, alarms.Count);
        }
        return code;
    }

    public Task<IReadOnlyList<string>> GetLineDefectDictionaryAsync(string workshopCode, CancellationToken ct = default)
    {
        // TODO(W-A5): PSM 端 `LineAndDefectDTO` 的查询接口细节待补，
        // 当前仅返回空集合避免阻塞 Controller。
        _logger.LogInformation("YingkeService.GetLineDefectDictionaryAsync 占位 workshopCode={Workshop}", workshopCode);
        return Task.FromResult<IReadOnlyList<string>>(Array.Empty<string>());
    }
}
