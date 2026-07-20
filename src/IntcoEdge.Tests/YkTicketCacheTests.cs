using IntcoEdge.EdgeHost.Models;
using Xunit;

namespace IntcoEdge.Tests;

/// <summary>
/// YkTicketCache 行为测试：
///   - 首次调用 loginFunc 拿 ticket
///   - TTL 内直接返回缓存，不重登
///   - TTL 过期后再次调用 loginFunc
///   - 登录失败时返回 null（不破坏旧缓存）
///   - Invalidate() 强制失效
/// </summary>
public class YkTicketCacheTests
{
    [Fact]
    public async Task GetOrLoginAsync_FirstCall_TriggersLogin()
    {
        var cache = new YkTicketCache(TimeSpan.FromMinutes(45));
        int loginCalls = 0;

        var (t1, _) = await cache.GetOrLoginAsync(_ =>
        {
            loginCalls++;
            return Task.FromResult<(string?, int?)>(("tk-1", 1));
        });

        Assert.Equal("tk-1", t1);
        Assert.Equal(1, loginCalls);
    }

    [Fact]
    public async Task GetOrLoginAsync_SecondCallWithinTtl_HitsCache()
    {
        var cache = new YkTicketCache(TimeSpan.FromMinutes(45));
        int loginCalls = 0;

        await cache.GetOrLoginAsync(_ =>
        {
            loginCalls++;
            return Task.FromResult<(string?, int?)>(("tk-1", 1));
        });
        var (t2, _) = await cache.GetOrLoginAsync(_ =>
        {
            loginCalls++;
            return Task.FromResult<(string?, int?)>(("tk-2", 1));
        });

        Assert.Equal("tk-1", t2); // 第二次拿到的是缓存的 tk-1
        Assert.Equal(1, loginCalls); // 只登录一次
    }

    [Fact]
    public async Task GetOrLoginAsync_AfterTtl_TriggersRelogin()
    {
        var cache = new YkTicketCache(TimeSpan.FromMilliseconds(50));
        int loginCalls = 0;

        await cache.GetOrLoginAsync(_ =>
        {
            loginCalls++;
            return Task.FromResult<(string?, int?)>(("tk-1", 1));
        });

        await Task.Delay(100); // 等待过期

        var (t2, _) = await cache.GetOrLoginAsync(_ =>
        {
            loginCalls++;
            return Task.FromResult<(string?, int?)>(("tk-2", 1));
        });

        Assert.Equal("tk-2", t2);
        Assert.Equal(2, loginCalls);
    }

    [Fact]
    public async Task GetOrLoginAsync_LoginFailure_ReturnsNullWithoutBreakingCache()
    {
        var cache = new YkTicketCache(TimeSpan.FromMinutes(45));
        int loginCalls = 0;

        // 第一次：登录失败
        var (t1, _) = await cache.GetOrLoginAsync(_ =>
        {
            loginCalls++;
            return Task.FromResult<(string?, int?)>((null, null));
        });

        Assert.Null(t1);
        Assert.Equal(1, loginCalls);

        // 第二次：再调一次（缓存里没东西，再登录一次）
        var (t2, _) = await cache.GetOrLoginAsync(_ =>
        {
            loginCalls++;
            return Task.FromResult<(string?, int?)>(("tk-recovered", 1));
        });

        Assert.Equal("tk-recovered", t2);
        Assert.Equal(2, loginCalls);
    }

    [Fact]
    public async Task Invalidate_ForcesRelogin()
    {
        var cache = new YkTicketCache(TimeSpan.FromMinutes(45));
        int loginCalls = 0;

        await cache.GetOrLoginAsync(_ =>
        {
            loginCalls++;
            return Task.FromResult<(string?, int?)>(("tk-1", 1));
        });

        cache.Invalidate();

        await cache.GetOrLoginAsync(_ =>
        {
            loginCalls++;
            return Task.FromResult<(string?, int?)>(("tk-2", 1));
        });

        Assert.Equal(2, loginCalls);
    }

    [Fact]
    public void Constructor_NullTtl_DefaultsTo45Minutes()
    {
        var cache = new YkTicketCache();
        Assert.Equal(TimeSpan.FromMinutes(45), cache.Ttl);
    }
}
