namespace IntcoEdge.Common.Models;

public sealed class MesServerOptions
{
    public const string SectionName = "MesServer";

    public string BaseUrl { get; set; } = "http://127.0.0.1:8100/";
    public string ApiPrefix { get; set; } = "/api/v1";
    public MesAuthType AuthType { get; set; } = MesAuthType.Jwt;
    public string? UserName { get; set; }
    public string? Password { get; set; }
    public string? AppKey { get; set; }
    public int TimeoutMs { get; set; } = 5_000;
    public bool UseMock { get; set; } = true;
    public MesMockOptions Mock { get; set; } = new();
}

public enum MesAuthType { None = 0, Jwt = 1, Ticket = 2 }

public sealed class MesMockOptions
{
    public string StorePath { get; set; } = @"D:\IntcoEdge\data\mes-mock";
    public int MinDelayMs { get; set; } = 50;
    public int MaxDelayMs { get; set; } = 200;
    public double FailureRate { get; set; } = 0.0;
    public double AuthFailureRate { get; set; } = 0.0;
    public double ServerErrorRate { get; set; } = 0.0;
    public double TimeoutRate { get; set; } = 0.0;
    public double DuplicateRate { get; set; } = 0.0;
}
