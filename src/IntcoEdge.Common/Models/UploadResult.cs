using System;
using System.Collections.Generic;
using System.Net;

namespace IntcoEdge.Common.Models;

public sealed class UploadResult
{
    public bool Success { get; set; }
    public Guid? EventId { get; set; }
    public string? TraceId { get; set; }
    public HttpStatusCode StatusCode { get; set; }
    public string? Error { get; set; }
    public bool Retryable { get; set; }
    public TimeSpan? RetryAfter { get; set; }
    public bool Duplicate { get; set; }
    public string? RawResponse { get; set; }

    public static UploadResult Ok(Guid eventId, string traceId, string? raw = null) => new()
    { Success = true, EventId = eventId, TraceId = traceId, StatusCode = HttpStatusCode.OK, RawResponse = raw };
    public static UploadResult Duplicated(Guid eventId, string traceId) => new()
    { Success = true, EventId = eventId, TraceId = traceId, StatusCode = HttpStatusCode.OK, Duplicate = true };
    public static UploadResult Fail(Guid eventId, HttpStatusCode code, string error, bool retryable, TimeSpan? retryAfter = null) => new()
    { Success = false, EventId = eventId, StatusCode = code, Error = error, Retryable = retryable, RetryAfter = retryAfter };
}

public sealed class MesLoginResult
{
    public bool Success { get; set; }
    public string? Token { get; set; }
    public string? Ticket { get; set; }
    public DateTimeOffset? ExpiresAt { get; set; }
    public string? Error { get; set; }

    public static MesLoginResult OkJwt(string token, DateTimeOffset expiresAt) => new()
    { Success = true, Token = token, ExpiresAt = expiresAt };
    public static MesLoginResult OkTicket(string ticket) => new()
    { Success = true, Ticket = ticket };
    public static MesLoginResult Fail(string error) => new() { Success = false, Error = error };
}
