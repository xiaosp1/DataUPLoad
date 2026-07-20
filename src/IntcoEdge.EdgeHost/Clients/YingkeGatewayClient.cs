using System.Net;
using System.Text.Json;
using IntcoEdge.Common;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace IntcoEdge.EdgeHost.Clients;

/// <summary>
/// 英科网关 HTTP 客户端（封装 `YingkeGatewayOptions.Url`）。
/// 反编译参考：`com.hikrobotics.solution.module.yingke.service.impl.YKServiceImpl`。
/// 所有调用都按 PSM `YKRequestDTO` / `YKResponseDTO` 的双层包装走：
///   - `ApiType` = 控制器类名（如 `AuthenticationController`）
///   - `Method` = 方法名（如 `Login`）
///   - `Parameters` = `{Value: ...}` 包装器的数组（按顺序对应方法形参）
///   - `Context` = `{Ticket, InvOrgId}`（登录时为 null / 业务接口必填）
///
/// ★ 关键事实（PM 反编译 + 权威 docx）：
///   - 英科网关的 ticket **不在 `Result.UserId`**，而在 `Context.Ticket`
///   - 登录返回的 `Result` 是用户信息（UserId/EmployeeId/UserCode/UserName/InvOrg）
///   - 登录返回的 `Context.Ticket` 才是后续业务接口的调用凭证
///   - PSM 端 50 分钟重登一次，我们缓存 45 分钟（少 5 分钟提前）
/// </summary>
public class YingkeGatewayClient
{
    private readonly IntcoHttpClient _http;
    private readonly YingkeGatewayOptions _options;
    private readonly YkTicketCache _ticketCache;
    private readonly ILogger<YingkeGatewayClient> _logger;

    public YingkeGatewayClient(
        IntcoHttpClient http,
        IOptions<YingkeGatewayOptions> options,
        YkTicketCache ticketCache,
        ILogger<YingkeGatewayClient> logger)
    {
        _http = http ?? throw new ArgumentNullException(nameof(http));
        _options = options?.Value ?? throw new ArgumentNullException(nameof(options));
        _ticketCache = ticketCache ?? throw new ArgumentNullException(nameof(ticketCache));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>英科网关 base URL（含 invoke 路径）。</summary>
    public string BaseUrl => _options.Url;

    /// <summary>当前 ticket 缓存（调试用）。</summary>
    public YkTicketCache TicketCache => _ticketCache;

    /// <summary>
    /// 登录英科网关，拿到 LoginResult（用户信息）。这只是底层调用，
    /// 一般业务代码请用 <see cref="GetTicketAsync"/>。
    /// </summary>
    public async Task<YkLoginResponse?> LoginAsync(CancellationToken ct = default)
    {
        if (!_options.Enabled)
        {
            _logger.LogDebug("英科网关对接已禁用，跳过登录");
            return null;
        }

        var request = new YkRequestDto
        {
            ApiType = Constants.YkApiTypeAuth,
            Method = Constants.YkMethodLogin,
            Parameters = new List<object>
            {
                YkLoginRequest.Wrap(_options.Username),
                YkLoginRequest.Wrap(_options.Password),
            },
            Context = null, // 登录时 Context 为空（PSM 端 setContext 之前是 null）
        };

        var response = await SendAsync(request, ct).ConfigureAwait(false);
        if (response == null) return null;
        if (response.Success != true)
        {
            _logger.LogWarning("英科网关登录 Success=false message={Message}", response.Message);
            return null;
        }

        try
        {
            return response.DeserializeResult<YkLoginResponse>();
        }
        catch (JsonException ex)
        {
            _logger.LogError(ex, "英科网关登录 Result 反序列化失败");
            return null;
        }
    }

    /// <summary>
    /// 获取 ticket（懒加载 + 缓存，TTL = 45 分钟）。
    /// ★ 关键：ticket 来自登录响应 `Context.Ticket`，**不是** `LoginResult.UserId`。
    /// </summary>
    /// <returns>(ticket, invOrgId)，登录失败时返回 (null, null)。</returns>
    public async Task<(string? Ticket, int? InvOrgId)> GetTicketAsync(CancellationToken ct = default)
    {
        if (!_options.Enabled)
        {
            return (null, null);
        }

        return await _ticketCache.GetOrLoginAsync(
            async (token) => await LoginForTicketAsync(token).ConfigureAwait(false),
            ct).ConfigureAwait(false);
    }

    /// <summary>
    /// 推送报警到英科网关（<c>ApiType=VisualInspectionController, Method=HandleVisualInspectionAlarm</c>）。
    /// 反编译对应：`YKServiceImpl.pushAlarm2YK`。
    /// </summary>
    /// <param name="alarms">要推送的报警列表（PSM 端 `AlarmDTO` 形态）。</param>
    /// <returns>英科网关业务 code（200=成功，400=业务失败）。网络/HTTP 失败返回 null。</returns>
    public async Task<int?> PushAlarmAsync(IReadOnlyList<AlarmPushDto> alarms, CancellationToken ct = default)
    {
        if (alarms == null || alarms.Count == 0)
        {
            _logger.LogDebug("PushAlarmAsync 入参为空，跳过");
            return null;
        }
        if (!_options.Enabled)
        {
            _logger.LogDebug("英科网关对接已禁用，跳过报警推送 count={Count}", alarms.Count);
            return null;
        }

        var (ticket, invOrg) = await GetTicketAsync(ct).ConfigureAwait(false);
        if (string.IsNullOrEmpty(ticket))
        {
            _logger.LogWarning("推送报警失败：ticket 为空");
            return null;
        }

        var request = new YkRequestDto
        {
            ApiType = Constants.YkApiTypeVisualInspection,
            Method = Constants.YkMethodHandleVisualInspectionAlarm,
            Parameters = new List<object>
            {
                YkListParam<AlarmPushDto>.Wrap(alarms.ToList()),
            },
            Context = new YkContextDto
            {
                Ticket = ticket,
                InvOrgId = invOrg ?? _options.InvOrgId,
            },
        };

        var response = await SendAsync(request, ct).ConfigureAwait(false);
        if (response == null) return null;
        if (response.Success != true)
        {
            _logger.LogWarning("英科网关推报警 Success=false message={Message}", response.Message);
            return null;
        }

        // 英科业务结果：{ code: 200, message: ... }
        if (response.Result is null || response.Result.Value.ValueKind != JsonValueKind.Object)
        {
            _logger.LogWarning("英科网关推报警返回 Result 不是对象 kind={Kind}", response.Result?.ValueKind);
            return null;
        }

        if (response.Result.Value.TryGetProperty("code", out var codeEl) && codeEl.ValueKind == JsonValueKind.Number)
        {
            var code = codeEl.GetInt32();
            if (code != 200)
            {
                var msgEl = response.Result.Value.TryGetProperty("message", out var m) ? m : default;
                _logger.LogWarning("英科网关推报警业务失败 code={Code} message={Message}", code, msgEl.GetString());
            }
            return code;
        }

        _logger.LogWarning("英科网关推报警返回 Result 缺 code 字段");
        return null;
    }

    /// <summary>登录并取完整上下文（Context.Ticket + Context.InvOrgId）。</summary>
    private async Task<(string? Ticket, int? InvOrgId)> LoginForTicketAsync(CancellationToken ct)
    {
        var request = new YkRequestDto
        {
            ApiType = Constants.YkApiTypeAuth,
            Method = Constants.YkMethodLogin,
            Parameters = new List<object>
            {
                YkLoginRequest.Wrap(_options.Username),
                YkLoginRequest.Wrap(_options.Password),
            },
            Context = null,
        };

        var response = await SendAsync(request, ct).ConfigureAwait(false);
        if (response == null)
        {
            _logger.LogWarning("英科网关登录返回为空");
            return (null, null);
        }
        if (response.Success != true)
        {
            _logger.LogWarning("英科网关登录 Success=false message={Message}", response.Message);
            return (null, null);
        }
        if (response.Context == null || string.IsNullOrEmpty(response.Context.Ticket))
        {
            _logger.LogWarning("英科网关登录未拿到 Context.Ticket success={Success}", response.Success);
            return (null, null);
        }

        _logger.LogInformation("英科网关登录成功 ticketLen={Len} invOrg={InvOrg}",
            response.Context.Ticket.Length, response.Context.InvOrgId);
        return (response.Context.Ticket, response.Context.InvOrgId);
    }

    /// <summary>底层：把 YKRequestDTO 发到英科网关，反序列化 YKResponseDTO。</summary>
    private async Task<YkResponseDto?> SendAsync(YkRequestDto request, CancellationToken ct)
    {
        try
        {
            var (status, body) = await _http.PostRawAsync(_options.Url, request, ct).ConfigureAwait(false);
            if (status != HttpStatusCode.OK)
            {
                _logger.LogWarning("英科网关 HTTP {Status} apiType={Api} method={Method}",
                    (int)status, request.ApiType, request.Method);
                return null;
            }
            if (string.IsNullOrEmpty(body))
            {
                return null;
            }
            return JsonSerializer.Deserialize<YkResponseDto>(body, IntcoHttpClient.DefaultJsonOptions);
        }
        catch (JsonException ex)
        {
            _logger.LogError(ex, "英科网关响应反序列化失败 apiType={Api} method={Method}",
                request.ApiType, request.Method);
            return null;
        }
        catch (HttpRequestException ex)
        {
            _logger.LogWarning(ex, "英科网关 HTTP 异常 apiType={Api}", request.ApiType);
            return null;
        }
    }
}
