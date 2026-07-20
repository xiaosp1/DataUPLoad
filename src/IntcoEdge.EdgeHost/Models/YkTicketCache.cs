namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关 ticket 缓存。
/// 反编译参考：`com.hikrobotics.solution.module.yingke.service.impl.YKServiceImpl.updateTicket`
/// （PSM 端每 50 分钟重新登录拿 ticket）。
///
/// 设计要点：
///   - **线程安全**：用 lock + double-check 避免并发登录风暴
///   - **过期重登**：TTL 默认 45 分钟（PSM 重登周期 50 分钟 - 5 分钟提前）
///   - **缓存粒度**：单实例缓存一个 ticket（PSM 也是单账号单 ticket）
///   - **可测试**：`loginFunc` 由调用方注入，方便 mock
/// </summary>
public class YkTicketCache
{
    private readonly object _lock = new();
    private string? _ticket;
    private int? _invOrgId;
    private DateTime _expiresAtUtc = DateTime.MinValue;

    /// <summary>TTL（默认 45 分钟，PSM 是 50 分钟重登，留 5 分钟缓冲）。</summary>
    public TimeSpan Ttl { get; }

    /// <summary>构造。</summary>
    /// <param name="ttl">过期时间，默认 45 分钟。</param>
    public YkTicketCache(TimeSpan? ttl = null)
    {
        Ttl = ttl ?? TimeSpan.FromMinutes(45);
    }

    /// <summary>
    /// 取 ticket：未过期直接返回；过期或不存在就调 <paramref name="loginFunc"/> 重新登录。
    /// 返回 (ticket, invOrgId) 元组；登录失败时返回 (null, null)。
    /// </summary>
    public async Task<(string? Ticket, int? InvOrgId)> GetOrLoginAsync(
        Func<CancellationToken, Task<(string? Ticket, int? InvOrgId)>> loginFunc,
        CancellationToken ct = default)
    {
        if (loginFunc == null) throw new ArgumentNullException(nameof(loginFunc));

        // 快速路径：未过期
        lock (_lock)
        {
            if (_ticket != null && DateTime.UtcNow < _expiresAtUtc)
            {
                return (_ticket, _invOrgId);
            }
        }

        // 慢速路径：重新登录
        var (ticket, invOrg) = await loginFunc(ct).ConfigureAwait(false);
        if (string.IsNullOrEmpty(ticket))
        {
            // 登录失败：保留旧值（避免反复重试）
            lock (_lock)
            {
                return (_ticket, _invOrgId);
            }
        }

        lock (_lock)
        {
            _ticket = ticket;
            _invOrgId = invOrg;
            _expiresAtUtc = DateTime.UtcNow + Ttl;
            return (_ticket, _invOrgId);
        }
    }

    /// <summary>主动失效缓存（用于登录接口接收到 401 时强制重登）。</summary>
    public void Invalidate()
    {
        lock (_lock)
        {
            _ticket = null;
            _invOrgId = null;
            _expiresAtUtc = DateTime.MinValue;
        }
    }

    /// <summary>调试用：当前 ticket + 过期时间。</summary>
    public (string? Ticket, DateTime ExpiresAtUtc) DebugState
    {
        get
        {
            lock (_lock)
            {
                return (_ticket, _expiresAtUtc);
            }
        }
    }
}
