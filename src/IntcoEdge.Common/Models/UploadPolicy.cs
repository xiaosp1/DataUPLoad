namespace IntcoEdge.Common.Models;

public sealed class UploadPolicy
{
    public int MaxBatchSize { get; set; } = 50;
    public int MaxRetry { get; set; } = 8;
    public double RetryBackoffBaseSec { get; set; } = 2.0;
    public int DeadLetterAfterRetries { get; set; } = 8;
    public int OfflineCompactionThreshold { get; set; } = 50_000;
    public int FlushIntervalSec { get; set; } = 1;
    public int OfflineFlushIntervalSec { get; set; } = 10;
    public int RequestTimeoutMs { get; set; } = 5_000;
}
