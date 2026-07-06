using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using IntcoEdge.Common.Contracts;
using IntcoEdge.Common.Models;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace IntcoEdge.MesUpload.Clients;

/// <summary>
/// MES Mock 客户端：模拟延迟、故障注入，落地收到的消息用于对账。
/// </summary>
public sealed class MockMesClient : IMesUploadClient
{
    private readonly MesServerOptions _serverOpts;
    private readonly MesMockOptions _opts;
    private readonly ILogger<MockMesClient>? _logger;
    private readonly string _storeDir;
    private readonly string _receivedFile;
    private readonly object _fileLock = new();
    private long _receivedCount;
    private long _failedCount;
    private long _duplicateCount;
    private string? _lastError;
    private DateTimeOffset _lastErrorAt;
    private readonly ConcurrentDictionary<Guid, byte> _seenEventIds = new();

    public MockMesClient(IOptions<MesServerOptions> serverOpts, ILogger<MockMesClient>? logger = null)
    {
        _serverOpts = serverOpts?.Value ?? throw new ArgumentNullException(nameof(serverOpts));
        _opts = _serverOpts.Mock ?? new MesMockOptions();
        _logger = logger;
        _storeDir = _opts.StorePath;
        Directory.CreateDirectory(_storeDir);
        _receivedFile = Path.Combine(_storeDir, "mock_received.jsonl");
    }

    public Task<MesLoginResult> LoginAsync(CancellationToken ct)
    {
        if (ShouldInject(_opts.AuthFailureRate))
        {
            _lastError = "Mock 401 Unauthorized";
            _lastErrorAt = DateTimeOffset.UtcNow;
            Interlocked.Increment(ref _failedCount);
            return Task.FromResult(MesLoginResult.Fail("Mock 401 Unauthorized"));
        }
        var token = "mock-jwt-" + Guid.NewGuid().ToString("N");
        return Task.FromResult(MesLoginResult.OkJwt(token, DateTimeOffset.UtcNow.AddHours(1)));
    }

    public async Task<UploadResult> SendAsync(MesEvent evt, CancellationToken ct)
    {
        await SimulateDelay(ct).ConfigureAwait(false);

        if (ShouldInject(_opts.TimeoutRate))
        {
            _lastError = "Mock 超时";
            _lastErrorAt = DateTimeOffset.UtcNow;
            Interlocked.Increment(ref _failedCount);
            throw new TimeoutException("Mock: 模拟超时");
        }
        if (ShouldInject(_opts.ServerErrorRate))
        {
            _lastError = "Mock 500";
            _lastErrorAt = DateTimeOffset.UtcNow;
            Interlocked.Increment(ref _failedCount);
            return UploadResult.Fail(evt.EventId, HttpStatusCode.InternalServerError, "Mock 500", retryable: true);
        }
        if (ShouldInject(_opts.AuthFailureRate))
        {
            _lastError = "Mock 401";
            _lastErrorAt = DateTimeOffset.UtcNow;
            Interlocked.Increment(ref _failedCount);
            return UploadResult.Fail(evt.EventId, HttpStatusCode.Unauthorized, "Mock 401", retryable: false);
        }

        bool duplicate = !_seenEventIds.TryAdd(evt.EventId, 0);
        if (duplicate)
        {
            Interlocked.Increment(ref _duplicateCount);
            AppendToFile(evt, duplicate: true);
            return UploadResult.Duplicated(evt.EventId, "mock-trace-" + Guid.NewGuid().ToString("N")[..8]);
        }
        if (ShouldInject(_opts.DuplicateRate))
        {
            Interlocked.Increment(ref _duplicateCount);
            AppendToFile(evt, duplicate: true);
            return UploadResult.Duplicated(evt.EventId, "mock-trace-" + Guid.NewGuid().ToString("N")[..8]);
        }

        AppendToFile(evt, duplicate: false);
        Interlocked.Increment(ref _receivedCount);
        var traceId = "mock-trace-" + Guid.NewGuid().ToString("N")[..8];
        return UploadResult.Ok(evt.EventId, traceId, "{\"ok\":true}");
    }

    public async Task<IReadOnlyList<UploadResult>> SendBatchAsync(IEnumerable<MesEvent> evts, CancellationToken ct)
    {
        var list = evts?.ToList() ?? throw new ArgumentNullException(nameof(evts));
        var results = new List<UploadResult>(list.Count);
        foreach (var e in list)
        {
            ct.ThrowIfCancellationRequested();
            results.Add(await SendAsync(e, ct).ConfigureAwait(false));
        }
        return results;
    }

    public MockStats GetStats() => new()
    {
        ReceivedCount = Interlocked.Read(ref _receivedCount),
        FailedCount = Interlocked.Read(ref _failedCount),
        DuplicateCount = Interlocked.Read(ref _duplicateCount),
        LastError = _lastError,
        LastErrorAt = _lastErrorAt
    };

    private async Task SimulateDelay(CancellationToken ct)
    {
        var min = Math.Max(0, _opts.MinDelayMs);
        var max = Math.Max(min, _opts.MaxDelayMs);
        var ms = Random.Shared.Next(min, max + 1);
        await Task.Delay(ms, ct).ConfigureAwait(false);
    }

    private static bool ShouldInject(double rate)
    {
        if (rate <= 0) return false;
        if (rate >= 1) return true;
        return Random.Shared.NextDouble() < rate;
    }

    private void AppendToFile(MesEvent evt, bool duplicate)
    {
        var record = new
        {
            ts = DateTimeOffset.UtcNow.ToString("o"),
            eventId = evt.EventId,
            sourceEventId = evt.SourceEventId,
            machineId = evt.MachineId,
            eventType = evt.EventType.ToString(),
            eventTime = evt.EventTime.ToString("o"),
            duplicate,
            payload = evt.Payload
        };
        var line = JsonSerializer.Serialize(record) + "\n";
        lock (_fileLock) { File.AppendAllText(_receivedFile, line); }
        _logger?.LogDebug("Mock 接收事件 {EventId} duplicate={Duplicate}", evt.EventId, duplicate);
    }
}

public sealed class MockStats
{
    public long ReceivedCount { get; set; }
    public long FailedCount { get; set; }
    public long DuplicateCount { get; set; }
    public string? LastError { get; set; }
    public DateTimeOffset LastErrorAt { get; set; }
}
