using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using IntcoEdge.Common.Models;

namespace IntcoEdge.Common.Contracts;

/// <summary>
/// MES 本地队列（SQLite 实现），负责持久化待发送事件、状态机与审计。
/// </summary>
public interface IMesUploadQueue
{
    Task EnqueueAsync(MesEvent evt, CancellationToken ct);
    Task EnqueueBatchAsync(IEnumerable<MesEvent> evts, CancellationToken ct);
    Task<IReadOnlyList<MesEvent>> DequeuePendingAsync(int limit, CancellationToken ct);
    Task MarkSentAsync(Guid eventId, string traceId, int statusCode, CancellationToken ct);
    Task MarkFailedAsync(Guid eventId, string error, CancellationToken ct);
    Task MarkDeadLetterAsync(Guid eventId, string error, CancellationToken ct);
    Task<QueueStats> GetStatsAsync(CancellationToken ct);
    Task<int> RequeueForRetryAsync(Guid eventId, double backoffSeconds, CancellationToken ct);
    Task<int> GetRetryCountAsync(Guid eventId, CancellationToken ct);
}
