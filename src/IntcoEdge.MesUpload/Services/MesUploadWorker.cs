using System;
using System.Collections.Generic;
using System.Net;
using System.Threading;
using System.Threading.Tasks;
using IntcoEdge.Common.Contracts;
using IntcoEdge.Common.Models;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace IntcoEdge.MesUpload.Services;

/// <summary>
/// MES 上传后台 Worker：轮询队列、批量发送、标记结果、退避重试、离线降级。
/// </summary>
public sealed class MesUploadWorker : BackgroundService
{
    private readonly IMesUploadClient _client;
    private readonly IMesUploadQueue _queue;
    private readonly UploadPolicy _policy;
    private readonly EdgeOptions _edge;
    private readonly ILogger<MesUploadWorker> _logger;

    private volatile bool _offline;
    private int _consecutiveErrors;

    public MesUploadWorker(
        IMesUploadClient client,
        IMesUploadQueue queue,
        IOptions<UploadPolicy> policy,
        IOptions<EdgeOptions> edge,
        ILogger<MesUploadWorker> logger)
    {
        _client = client ?? throw new ArgumentNullException(nameof(client));
        _queue = queue ?? throw new ArgumentNullException(nameof(queue));
        _policy = policy?.Value ?? throw new ArgumentNullException(nameof(policy));
        _edge = edge?.Value ?? throw new ArgumentNullException(nameof(edge));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("MesUploadWorker 启动 Flush={Flush}s OfflineFlush={Off}s Batch={Batch}",
            _policy.FlushIntervalSec, _policy.OfflineFlushIntervalSec, _policy.MaxBatchSize);

        try { await _client.LoginAsync(stoppingToken).ConfigureAwait(false); }
        catch (Exception ex) { _logger.LogWarning(ex, "MES 初次登录失败，稍后重试"); }

        while (!stoppingToken.IsCancellationRequested)
        {
            try { await TickAsync(stoppingToken).ConfigureAwait(false); }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested) { break; }
            catch (Exception ex)
            {
                _logger.LogError(ex, "MesUploadWorker Tick 异常");
                EnterOffline();
            }

            var interval = _offline || _edge.OfflineMode
                ? TimeSpan.FromSeconds(Math.Max(1, _policy.OfflineFlushIntervalSec))
                : TimeSpan.FromSeconds(Math.Max(1, _policy.FlushIntervalSec));

            try { await Task.Delay(interval, stoppingToken).ConfigureAwait(false); }
            catch (OperationCanceledException) { break; }
        }

        _logger.LogInformation("MesUploadWorker 收到停止信号，尝试发送最后一批...");
        using var drainCts = new CancellationTokenSource(TimeSpan.FromSeconds(5));
        try { await TickAsync(drainCts.Token).ConfigureAwait(false); }
        catch (OperationCanceledException) { }
        catch (Exception ex) { _logger.LogWarning(ex, "MesUploadWorker drain 最后一批异常"); }
        _logger.LogInformation("MesUploadWorker 已停止");
    }

    private async Task TickAsync(CancellationToken ct)
    {
        var batch = await _queue.DequeuePendingAsync(_policy.MaxBatchSize, ct).ConfigureAwait(false);
        if (batch.Count == 0)
        {
            if (_consecutiveErrors > 0 && !_offline) _consecutiveErrors = 0;
            return;
        }

        IReadOnlyList<UploadResult> results;
        try
        {
            results = await _client.SendBatchAsync(batch, ct).ConfigureAwait(false);
            LeaveOffline();
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "SendBatch 抛出异常，整批按失败处理");
            EnterOffline();
            foreach (var e in batch)
                await HandleFailureAsync(e, ex.Message, retryable: true, retryAfter: null, ct).ConfigureAwait(false);
            return;
        }

        for (int i = 0; i < batch.Count; i++)
        {
            var evt = batch[i];
            var r = i < results.Count ? results[i] : UploadResult.Fail(evt.EventId, (HttpStatusCode)0, "客户端未返回结果", true);
            if (r.Success)
            {
                await _queue.MarkSentAsync(evt.EventId, r.TraceId ?? "", (int)r.StatusCode, ct).ConfigureAwait(false);
                _consecutiveErrors = 0;
            }
            else
            {
                await HandleFailureAsync(evt, r.Error ?? "发送失败", r.Retryable, r.RetryAfter, ct).ConfigureAwait(false);
            }
        }
    }

    private async Task HandleFailureAsync(MesEvent evt, string error, bool retryable, TimeSpan? retryAfter, CancellationToken ct)
    {
        if (!retryable)
        {
            _logger.LogWarning("事件 {EventId} 不可重试，移入死信: {Error}", evt.EventId, error);
            await _queue.MarkFailedAsync(evt.EventId, error, ct).ConfigureAwait(false);
            await _queue.MarkDeadLetterAsync(evt.EventId, error, ct).ConfigureAwait(false);
            return;
        }

        await _queue.MarkFailedAsync(evt.EventId, error, ct).ConfigureAwait(false);

        double backoff;
        if (retryAfter.HasValue) backoff = retryAfter.Value.TotalSeconds;
        else backoff = Math.Min(300, _policy.RetryBackoffBaseSec * Math.Pow(2, Math.Max(0, _consecutiveErrors)));

        var retryCount = await _queue.RequeueForRetryAsync(evt.EventId, backoff, ct).ConfigureAwait(false);

        if (retryCount >= _policy.DeadLetterAfterRetries)
        {
            _logger.LogWarning("事件 {EventId} 超过重试上限 {N}，移入死信", evt.EventId, _policy.DeadLetterAfterRetries);
            await _queue.MarkDeadLetterAsync(evt.EventId, error, ct).ConfigureAwait(false);
        }
        _consecutiveErrors++;
    }

    private void EnterOffline()
    {
        if (!_offline)
        {
            _offline = true;
            _logger.LogWarning("MES 进入离线模式，降级轮询 {Off}s", _policy.OfflineFlushIntervalSec);
        }
    }

    private void LeaveOffline()
    {
        if (_offline)
        {
            _offline = false;
            _logger.LogInformation("MES 网络恢复，退出离线模式");
        }
    }
}
