using System;

namespace IntcoEdge.Common.Models;

/// <summary>
/// Edge 端全局选项（MES 模块必需的最小引用点）。
/// </summary>
public sealed class EdgeOptions
{
    public const string SectionName = "Edge";

    public string NodeId { get; set; } = Environment.MachineName;
    public string DataPath { get; set; } = @"D:\IntcoEdge\data";
    public bool OfflineMode { get; set; } = false;
}
