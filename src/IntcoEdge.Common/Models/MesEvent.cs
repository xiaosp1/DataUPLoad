using System;
using System.Collections.Generic;
using System.Text.Json;

namespace IntcoEdge.Common.Models;

public sealed class MesEvent
{
    public Guid EventId { get; set; }
    public string? SourceEventId { get; set; }
    public string MachineId { get; set; } = string.Empty;
    public string? LineId { get; set; }
    public string? WorkshopId { get; set; }
    public string? ShiftId { get; set; }
    public MesEventType EventType { get; set; }
    public DateTimeOffset EventTime { get; set; }
    public DateTimeOffset CollectedAt { get; set; }
    public JsonElement Payload { get; set; }
    public Dictionary<string, string>? ExtraProps { get; set; }
}

public enum MesEventType
{
    Unknown = 0,
    Production = 1,
    Defect = 2,
    Alarm = 3,
    Status = 4,
    Parameter = 5,
    WorkOrderResult = 6,
    Custom = 99
}
