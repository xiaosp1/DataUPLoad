using System.Net;
using System.Text.Json;
using IntcoEdge.Common;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace IntcoEdge.EdgeHost.Clients;

/// <summary>
/// 英科网关 HTTP 客户端（封装 `YingkeGatewayOptions.Url`）。
/// 反编译参考：`com.hikrobotics.solution.module.yingke.service.YingkeService` / `YingkeServiceImpl`。
/// 所有调用都按 PSM `YKRequestDTO` / `YKResponseDTO` 的双层包装走。
/// </summary>
public class YingkeGatewayClient
{
    private readonly IntcoHttpClient _http;
    private readonly YingkeGatewayOptions _options;
    private readonly ILogger<YingkeGatewayClient> _logger;

    /// <summary>本地缓存的 ticket（key = workshopCode）。</summary>
    private readonly Dictionary<string, string> _ticketCache = new(StringComparer.OrdinalIgnoreCase);

    /// <summary>本地缓存的库存组织 ID（key = workshopCode）。</summary>
    private readonly Dictionary<string, int> _invOrgCache = new(StringComparer.OrdinalIgnoreCase);

    private readonly SemaphoreSlim _ticketLock = new(1, 1);

    public YingkeGatewayClient(
        IntcoHttpClient http,
        IOptions<YingkeGatewayOptions> options,
        ILogger<YingkeGatewayClient> logger)
    {
        _http = http ?? throw new ArgumentNullException(nameof(http));
        _options = options?.Value ?? throw new ArgumentNullException(nameof(options));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>英科网关 base URL（含 invoke 路径）。</summary>
    public string BaseUrl => _options.Url;

    /// <summary>登录英科网关，换 ticket。</summary>
    /// <param name="workshopCode">车间编码。</param>
    public async Task<YkLoginResponse?> LoginAsync(string workshopCode, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(workshopCode))
        {
            throw new ArgumentException("workshopCode 不能为空", nameof(workshopCode));
        }

        var request = new YkRequestDto<YkLoginRequest>
        {
            ApiType = Constants.YkApiTypeLogin,
            Method = "login",
            Parameters = new List<YkLoginRequest>
            {
                new() { WorkShopCode = workshopCode }
            },
        };

        var response = await SendAsync<YkLoginRequest, YkResponseDto>(request, ct).ConfigureAwait(false);
        if (response == null)
        {
            _logger.LogWarning("英科网关登录返回为空 workshopCode={Workshop}", workshopCode);
            return null;
        }

        if (response.Success != true)
        {
            _logger.LogWarning("英科网关登录失败 workshopCode={Workshop} message={Message}", workshopCode, response.Message);
            return null;
        }

        // PSM 端 Result 是 Object，需要按 YkLoginResponse 反序列化
        if (!response.Result.HasValue || response.Result.Value.ValueKind == JsonValueKind.Null)
        {
            _logger.LogWarning("英科网关登录返回 Result 为空 workshopCode={Workshop}", workshopCode);
            return null;
        }

        try
        {
            var loginResult = response.Result.Value.Deserialize<YkLoginResponse>(IntcoHttpClient.DefaultJsonOptions);
            return loginResult;
        }
        catch (JsonException ex)
        {
            _logger.LogError(ex, "英科网关登录返回 Result 反序列化失败 workshopCode={Workshop}", workshopCode);
            return null;
        }
    }

    /// <summary>
    /// 获取 ticket（懒加载 + 缓存：每个 workshopCode 首次调用触发登录，后续命中缓存）。
    /// </summary>
    public async Task<string?> GetTicketAsync(string workshopCode, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(workshopCode))
        {
            throw new ArgumentException("workshopCode 不能为空", nameof(workshopCode));
        }

        if (_ticketCache.TryGetValue(workshopCode, out var cached) && !string.IsNullOrEmpty(cached))
        {
            return cached;
        }

        await _ticketLock.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            // double-check
            if (_ticketCache.TryGetValue(workshopCode, out cached) && !string.IsNullOrEmpty(cached))
            {
                return cached;
            }

            var login = await LoginAsync(workshopCode, ct).ConfigureAwait(false);
            if (login == null || string.IsNullOrEmpty(login.UserId))
            {
                _logger.LogWarning("获取英科 ticket 失败 workshopCode={Workshop}", workshopCode);
                return null;
            }

            // PSM 端约定：login 返回的 UserId 就是后续调用的 Ticket 凭证。
            _ticketCache[workshopCode] = login.UserId;
            if (login.InvOrg.HasValue)
            {
                _invOrgCache[workshopCode] = login.InvOrg.Value;
            }

            _logger.LogInformation("已缓存英科 ticket workshopCode={Workshop}", workshopCode);
            return login.UserId;
        }
        finally
        {
            _ticketLock.Release();
        }
    }

    /// <summary>查询英科网关缺陷记录。</summary>
    public async Task<YkDefectQueryResponse?> QueryDefectAsync(YkDefectQueryRequest req, CancellationToken ct = default)
    {
        if (req == null)
        {
            throw new ArgumentNullException(nameof(req));
        }

        // 把 SearchDefectRecordDto 列表装进 Parameters
        var request = new YkRequestDto<SearchDefectRecordDto>
        {
            ApiType = _options.ApiType,
            Method = "queryDefect",
            Parameters = req.Parameters ?? new List<SearchDefectRecordDto>(),
        };

        var response = await SendAsync<SearchDefectRecordDto, YkDefectQueryResponse>(request, ct).ConfigureAwait(false);
        return response;
    }

    /// <summary>推送报警到英科网关（调英科内部 ApiType）。</summary>
    public async Task<HttpStatusCode> PushAlarmAsync(AlarmPushDto alarm, CancellationToken ct = default)
    {
        if (alarm == null)
        {
            throw new ArgumentNullException(nameof(alarm));
        }

        var request = new YkRequestDto<AlarmPushDto>
        {
            ApiType = _options.ApiType,
            Method = "pushAlarm",
            Parameters = new List<AlarmPushDto> { alarm },
        };

        var (status, _) = await _http.PostRawAsync(_options.Url, request, ct).ConfigureAwait(false);
        return status;
    }

    /// <summary>底层：把 YKRequestDTO 发到英科网关，反序列化 YKResponseDTO。</summary>
    private async Task<TResponse?> SendAsync<TParam, TResponse>(YkRequestDto<TParam> request, CancellationToken ct)
        where TResponse : class
    {
        var (status, body) = await _http.PostRawAsync(_options.Url, request, ct).ConfigureAwait(false);
        if (status != HttpStatusCode.OK)
        {
            _logger.LogWarning("英科网关 HTTP {Status} apiType={Api}", (int)status, request.ApiType);
            return null;
        }
        if (string.IsNullOrEmpty(body))
        {
            return null;
        }
        try
        {
            return JsonSerializer.Deserialize<TResponse>(body, IntcoHttpClient.DefaultJsonOptions);
        }
        catch (JsonException ex)
        {
            _logger.LogError(ex, "英科网关响应反序列化失败 apiType={Api} body={Body}", request.ApiType, body);
            return null;
        }
    }
}
