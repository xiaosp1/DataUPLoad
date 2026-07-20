using IntcoEdge.Common;
using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Services;

/// <summary>
/// 英科网关业务服务：登录拿 ticket + 缺陷查询 + 字典拉取。
/// Controllers 只调本服务，不直接动 YingkeGatewayClient，便于切实现 + 集中日志。
/// </summary>
public interface IYingkeService
{
    /// <summary>英科登录并缓存 ticket（首次会远程登录）。</summary>
    Task<YkLoginResponse?> LoginAsync(string workshopCode, CancellationToken ct = default);

    /// <summary>查英科缺陷记录。</summary>
    Task<YkDefectQueryResponse?> QueryDefectAsync(SearchDefectRecordDto query, CancellationToken ct = default);

    /// <summary>英科产线-缺陷字典（占位：PSM 端 `LineAndDefectDTO` 的 Get 接口待补）。</summary>
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

    public async Task<YkLoginResponse?> LoginAsync(string workshopCode, CancellationToken ct = default)
    {
        _logger.LogInformation("YingkeService.LoginAsync workshopCode={Workshop}", workshopCode);
        return await _ykClient.LoginAsync(workshopCode, ct).ConfigureAwait(false);
    }

    public async Task<YkDefectQueryResponse?> QueryDefectAsync(SearchDefectRecordDto query, CancellationToken ct = default)
    {
        if (query == null)
        {
            throw new ArgumentNullException(nameof(query));
        }

        _logger.LogInformation(
            "YingkeService.QueryDefectAsync startTime={Start} endTime={End} lindGroup={LindCount} defectGroup={DefectCount} faceGroup={FaceCount}",
            query.StartTime,
            query.EndTime,
            query.LindGroup?.Count ?? 0,
            query.DefectGroup?.Count ?? 0,
            query.FaceGroup?.Count ?? 0);

        var request = new YkDefectQueryRequest { Parameters = new List<SearchDefectRecordDto> { query } };
        return await _ykClient.QueryDefectAsync(request, ct).ConfigureAwait(false);
    }

    public Task<IReadOnlyList<string>> GetLineDefectDictionaryAsync(string workshopCode, CancellationToken ct = default)
    {
        // TODO(W-A5): PSM 端 `LineAndDefectDTO` 的查询接口细节待补，
        // 当前仅返回空集合避免阻塞 Controller。
        _logger.LogInformation("YingkeService.GetLineDefectDictionaryAsync 占位 workshopCode={Workshop}", workshopCode);
        return Task.FromResult<IReadOnlyList<string>>(Array.Empty<string>());
    }
}
