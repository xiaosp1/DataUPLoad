using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Dapper;
using IntcoEdge.Common.Contracts;
using IntcoEdge.Common.Models;
using Microsoft.Data.Sqlite;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.Storage;

public sealed class MesUploadQueue : IMesUploadQueue, IDisposable
{
    private readonly string _dbPath;
    private readonly string _schemaPath;
    private readonly ILogger<MesUploadQueue>? _logger;
    private readonly SemaphoreSlim _mutex = new(1, 1);
    private bool _initialized;

    public MesUploadQueue(string dbPath, string? schemaDirectory = null, ILogger<MesUploadQueue>? logger = null)
    {
        _dbPath = dbPath ?? throw new ArgumentNullException(nameof(dbPath));
        _logger = logger;
        var schemaDir = schemaDirectory ?? Path.Combine(AppContext.BaseDirectory, "Schema");
        _schemaPath = Path.Combine(schemaDir, "schema.sql");
    }

    public async Task InitializeAsync(CancellationToken ct = default)
    {
        if (_initialized) return;
        await _mutex.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            if (_initialized) return;
            var dir = Path.GetDirectoryName(_dbPath);
            if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir)) Directory.CreateDirectory(dir);
            var sql = File.Exists(_schemaPath)
                ? await File.ReadAllTextAsync(_schemaPath, ct).ConfigureAwait(false)
                : EmbeddedSchemaFallback;
            await using var conn = CreateConnection();
            await conn.OpenAsync(ct).ConfigureAwait(false);
            foreach (var stmt in SplitStatements(sql))
            {
                if (string.IsNullOrWhiteSpace(stmt)) continue;
                await conn.ExecuteAsync(new CommandDefinition(stmt, cancellationToken: ct)).ConfigureAwait(false);
            }
            _initialized = true;
        }
        finally { _mutex.Release(); }
    }

    private SqliteConnection CreateConnection()
    {
        var builder = new SqliteConnectionStringBuilder
        {
            DataSource = _dbPath,
            Mode = SqliteOpenMode.ReadWriteCreate,
            Cache = SqliteCacheMode.Private
        };
        return new SqliteConnection(builder.ToString());
    }

    public async Task EnqueueAsync(MesEvent evt, CancellationToken ct)
    {
        if (evt is null) throw new ArgumentNullException(nameof(evt));
        await InitializeAsync(ct).ConfigureAwait(false);
        const string sql = @"
INSERT OR IGNORE INTO mes_outbox
(event_id, source_event_id, machine_id, line_id, workshop_id, shift_id, event_type,
 event_time, collected_at, payload, extra_props, status, retry_count,
 last_error, trace_id, http_status, next_retry_at, created_at, updated_at)
VALUES
(@event_id, @source_event_id, @machine_id, @line_id, @workshop_id, @shift_id, @event_type,
 @event_time, @collected_at, @payload, @extra_props, 'Pending', 0,
 NULL, NULL, NULL, NULL, @now, @now);
INSERT INTO mes_audit_log(event_id, action, http_status, error, trace_id, retry_count, created_at)
SELECT @event_id, 'Enqueue', NULL, NULL, NULL, 0, @now
WHERE changes() > 0;";

        var now = DateTimeOffset.UtcNow;
        if (evt.EventId == Guid.Empty) evt.EventId = Guid.NewGuid();
        if (evt.CollectedAt == default) evt.CollectedAt = now;

        await using var conn = CreateConnection();
        await conn.OpenAsync(ct).ConfigureAwait(false);
        await conn.ExecuteAsync(new CommandDefinition(sql, new
        {
            event_id = evt.EventId.ToString(),
            source_event_id = (string?)evt.SourceEventId,
            machine_id = evt.MachineId,
            line_id = (string?)evt.LineId,
            workshop_id = (string?)evt.WorkshopId,
            shift_id = (string?)evt.ShiftId,
            event_type = (int)evt.EventType,
            event_time = evt.EventTime.ToString("o"),
            collected_at = evt.CollectedAt.ToString("o"),
            payload = JsonSerializer.Serialize(evt.Payload),
            extra_props = evt.ExtraProps == null ? null : JsonSerializer.Serialize(evt.ExtraProps),
            now = now.ToString("o")
        }, cancellationToken: ct)).ConfigureAwait(false);
    }

    public async Task EnqueueBatchAsync(IEnumerable<MesEvent> evts, CancellationToken ct)
    {
        if (evts is null) throw new ArgumentNullException(nameof(evts));
        foreach (var e in evts) await EnqueueAsync(e, ct).ConfigureAwait(false);
    }

    public async Task<IReadOnlyList<MesEvent>> DequeuePendingAsync(int limit, CancellationToken ct)
    {
        if (limit <= 0) throw new ArgumentOutOfRangeException(nameof(limit));
        await InitializeAsync(ct).ConfigureAwait(false);
        await using var conn = CreateConnection();
        await conn.OpenAsync(ct).ConfigureAwait(false);
        await using var tx = await conn.BeginTransactionAsync(IsolationLevel.Serializable, ct).ConfigureAwait(false);

        const string markSql = @"
UPDATE mes_outbox
SET status = 'Sending', updated_at = @now
WHERE event_id IN (
    SELECT event_id FROM mes_outbox
    WHERE status = 'Pending'
      AND (next_retry_at IS NULL OR next_retry_at <= @now)
    ORDER BY created_at ASC
    LIMIT @limit
);
SELECT * FROM mes_outbox WHERE status = 'Sending' ORDER BY created_at ASC;";

        var now = DateTimeOffset.UtcNow;
        var rows = (await conn.QueryAsync<MesOutboxRow>(new CommandDefinition(markSql, new
        {
            now = now.ToString("o"), limit
        }, transaction: (DbTransaction?)tx, cancellationToken: ct)).ConfigureAwait(false)).ToList();

        await tx.CommitAsync(ct).ConfigureAwait(false);
        return rows.Select(MapRowToEvent).ToList();
    }

    public async Task MarkSentAsync(Guid eventId, string traceId, int statusCode, CancellationToken ct)
    {
        await InitializeAsync(ct).ConfigureAwait(false);
        const string sql = @"
UPDATE mes_outbox SET status='Sent', trace_id=@trace_id, http_status=@statusCode, last_error=NULL, updated_at=@now
WHERE event_id=@event_id;
INSERT INTO mes_audit_log(event_id,action,http_status,error,trace_id,retry_count,created_at)
VALUES(@event_id,'MarkSent',@statusCode,NULL,@trace_id,(SELECT retry_count FROM mes_outbox WHERE event_id=@event_id),@now);";
        await using var conn = CreateConnection();
        await conn.OpenAsync(ct).ConfigureAwait(false);
        await conn.ExecuteAsync(new CommandDefinition(sql, new { event_id=eventId.ToString(), trace_id=traceId, statusCode, now=DateTimeOffset.UtcNow.ToString("o") }, cancellationToken: ct)).ConfigureAwait(false);
    }

    public async Task MarkFailedAsync(Guid eventId, string error, CancellationToken ct)
    {
        await InitializeAsync(ct).ConfigureAwait(false);
        const string sql = @"
UPDATE mes_outbox SET status='Failed', last_error=@error, retry_count=retry_count+1, updated_at=@now
WHERE event_id=@event_id;
INSERT INTO mes_audit_log(event_id,action,http_status,error,trace_id,retry_count,created_at)
VALUES(@event_id,'Fail',NULL,@error,NULL,(SELECT retry_count FROM mes_outbox WHERE event_id=@event_id),@now);";
        await using var conn = CreateConnection();
        await conn.OpenAsync(ct).ConfigureAwait(false);
        await conn.ExecuteAsync(new CommandDefinition(sql, new { event_id=eventId.ToString(), error=(object?)error ?? DBNull.Value, now=DateTimeOffset.UtcNow.ToString("o") }, cancellationToken: ct)).ConfigureAwait(false);
    }

    public async Task MarkDeadLetterAsync(Guid eventId, string error, CancellationToken ct)
    {
        await InitializeAsync(ct).ConfigureAwait(false);
        const string sql = @"
UPDATE mes_outbox SET status='DeadLetter', last_error=@error, updated_at=@now WHERE event_id=@event_id;
INSERT INTO mes_audit_log(event_id,action,http_status,error,trace_id,retry_count,created_at)
VALUES(@event_id,'DeadLetter',NULL,@error,NULL,(SELECT retry_count FROM mes_outbox WHERE event_id=@event_id),@now);";
        await using var conn = CreateConnection();
        await conn.OpenAsync(ct).ConfigureAwait(false);
        await conn.ExecuteAsync(new CommandDefinition(sql, new { event_id=eventId.ToString(), error=(object?)error ?? DBNull.Value, now=DateTimeOffset.UtcNow.ToString("o") }, cancellationToken: ct)).ConfigureAwait(false);
    }

    public async Task<int> RequeueForRetryAsync(Guid eventId, double backoffSeconds, CancellationToken ct)
    {
        await InitializeAsync(ct).ConfigureAwait(false);
        const string sql = @"
UPDATE mes_outbox SET status='Pending', next_retry_at=@nextRetryAt, updated_at=@now WHERE event_id=@event_id;
INSERT INTO mes_audit_log(event_id,action,http_status,error,trace_id,retry_count,created_at)
VALUES(@event_id,'Retry',NULL,NULL,NULL,(SELECT retry_count FROM mes_outbox WHERE event_id=@event_id),@now);
SELECT retry_count FROM mes_outbox WHERE event_id=@event_id;";
        await using var conn = CreateConnection();
        await conn.OpenAsync(ct).ConfigureAwait(false);
        return await conn.ExecuteScalarAsync<int>(new CommandDefinition(sql, new
        {
            event_id = eventId.ToString(),
            nextRetryAt = DateTimeOffset.UtcNow.AddSeconds(backoffSeconds).ToString("o"),
            now = DateTimeOffset.UtcNow.ToString("o")
        }, cancellationToken: ct)).ConfigureAwait(false);
    }

    public async Task<int> GetRetryCountAsync(Guid eventId, CancellationToken ct)
    {
        await InitializeAsync(ct).ConfigureAwait(false);
        const string sql = "SELECT COALESCE(retry_count, -1) FROM mes_outbox WHERE event_id = @event_id;";
        await using var conn = CreateConnection();
        await conn.OpenAsync(ct).ConfigureAwait(false);
        var val = await conn.ExecuteScalarAsync<int?>(new CommandDefinition(sql, new { event_id = eventId.ToString() }, cancellationToken: ct)).ConfigureAwait(false);
        return val ?? -1;
    }

    public async Task<QueueStats> GetStatsAsync(CancellationToken ct)
    {
        await InitializeAsync(ct).ConfigureAwait(false);
        const string sql = @"
SELECT
  SUM(CASE WHEN status='Pending' THEN 1 ELSE 0 END) AS Pending,
  SUM(CASE WHEN status='Sending' THEN 1 ELSE 0 END) AS InFlight,
  SUM(CASE WHEN status='Sent' THEN 1 ELSE 0 END) AS Sent,
  SUM(CASE WHEN status='Failed' THEN 1 ELSE 0 END) AS Failed,
  SUM(CASE WHEN status='DeadLetter' THEN 1 ELSE 0 END) AS DeadLetter
FROM mes_outbox;";
        const string rateSql = @"
SELECT
  SUM(CASE WHEN action='MarkSent' THEN 1 ELSE 0 END) AS SentCount,
  SUM(CASE WHEN action IN ('Fail','DeadLetter') THEN 1 ELSE 0 END) AS FailCount
FROM mes_audit_log WHERE created_at >= @since;";

        await using var conn = CreateConnection();
        await conn.OpenAsync(ct).ConfigureAwait(false);
        var agg = await conn.QueryFirstOrDefaultAsync(new CommandDefinition(sql, cancellationToken: ct)).ConfigureAwait(false);
        var since = DateTimeOffset.UtcNow.AddHours(-1).ToString("o");
        var rate = await conn.QueryFirstOrDefaultAsync(new CommandDefinition(rateSql, new { since }, cancellationToken: ct)).ConfigureAwait(false);

        double? rateValue = null;
        long sent = (long)(rate?.SentCount ?? 0L);
        long fail = (long)(rate?.FailCount ?? 0L);
        long total = sent + fail;
        if (total > 0) rateValue = (double)sent / total;

        return new QueueStats
        {
            Pending = (int)(agg?.Pending ?? 0),
            InFlight = (int)(agg?.InFlight ?? 0),
            Sent = (int)(agg?.Sent ?? 0),
            Failed = (int)(agg?.Failed ?? 0),
            DeadLetter = (int)(agg?.DeadLetter ?? 0),
            SuccessRate1H = rateValue,
            GeneratedAt = DateTimeOffset.UtcNow
        };
    }

    private static MesEvent MapRowToEvent(MesOutboxRow r)
    {
        var evt = new MesEvent
        {
            EventId = Guid.Parse(r.event_id),
            SourceEventId = r.source_event_id,
            MachineId = r.machine_id,
            LineId = r.line_id,
            WorkshopId = r.workshop_id,
            ShiftId = r.shift_id,
            EventType = (MesEventType)r.event_type,
            EventTime = DateTimeOffset.Parse(r.event_time),
            CollectedAt = DateTimeOffset.Parse(r.collected_at)
        };
        evt.Payload = string.IsNullOrWhiteSpace(r.payload) ? default : JsonSerializer.Deserialize<JsonElement>(r.payload);
        if (!string.IsNullOrWhiteSpace(r.extra_props))
            evt.ExtraProps = JsonSerializer.Deserialize<Dictionary<string, string>>(r.extra_props);
        return evt;
    }

    private static IEnumerable<string> SplitStatements(string sql)
    {
        foreach (var raw in sql.Split(new[] { ';' }, StringSplitOptions.RemoveEmptyEntries))
        {
            var s = raw.Trim();
            if (s.Length == 0) continue;
            yield return s + ";";
        }
    }

    public void Dispose() => _mutex.Dispose();

    private sealed class MesOutboxRow
    {
        public string event_id { get; set; } = default!;
        public string? source_event_id { get; set; }
        public string machine_id { get; set; } = default!;
        public string? line_id { get; set; }
        public string? workshop_id { get; set; }
        public string? shift_id { get; set; }
        public int event_type { get; set; }
        public string event_time { get; set; } = default!;
        public string collected_at { get; set; } = default!;
        public string payload { get; set; } = default!;
        public string? extra_props { get; set; }
        public string status { get; set; } = default!;
        public int retry_count { get; set; }
        public string? last_error { get; set; }
        public string? trace_id { get; set; }
        public int? http_status { get; set; }
        public string? next_retry_at { get; set; }
        public string created_at { get; set; } = default!;
        public string updated_at { get; set; } = default!;
    }

    private const string EmbeddedSchemaFallback = @"
CREATE TABLE IF NOT EXISTS mes_outbox (
    event_id TEXT NOT NULL PRIMARY KEY,
    source_event_id TEXT, machine_id TEXT NOT NULL, line_id TEXT, workshop_id TEXT, shift_id TEXT,
    event_type INTEGER NOT NULL, event_time TEXT NOT NULL, collected_at TEXT NOT NULL, payload TEXT NOT NULL,
    extra_props TEXT, status TEXT NOT NULL, retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT, trace_id TEXT, http_status INTEGER, next_retry_at TEXT,
    created_at TEXT NOT NULL, updated_at TEXT NOT NULL);
CREATE INDEX IF NOT EXISTS idx_outbox_status ON mes_outbox(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_outbox_machine_time ON mes_outbox(machine_id, event_time);
CREATE INDEX IF NOT EXISTS idx_outbox_sent ON mes_outbox(status, updated_at);
CREATE TABLE IF NOT EXISTS mes_audit_log (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, event_id TEXT NOT NULL, action TEXT NOT NULL,
    http_status INTEGER, error TEXT, trace_id TEXT, retry_count INTEGER, created_at TEXT NOT NULL);
CREATE INDEX IF NOT EXISTS idx_audit_event ON mes_audit_log(event_id, created_at);";
}