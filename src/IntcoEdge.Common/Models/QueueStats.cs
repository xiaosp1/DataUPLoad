using System;

namespace IntcoEdge.Common.Models;

public sealed class QueueStats
{
    public int Pending { get; set; }
    public int InFlight { get; set; }
    public int Sent { get; set; }
    public int Failed { get; set; }
    public int DeadLetter { get; set; }
    public double? SuccessRate1H { get; set; }
    public DateTimeOffset GeneratedAt { get; set; } = DateTimeOffset.UtcNow;
}
