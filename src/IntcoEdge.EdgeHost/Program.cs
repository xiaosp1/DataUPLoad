// IntcoEdge.EdgeHost - MVP 骨架阶段最小入口
// 说明：不引入 WPF/Serilog/ASP.NET 依赖；采集器/MES上传Worker/健康检查/Windows Service 等后续 Sprint 补齐。

using System;

namespace IntcoEdge.EdgeHost;

internal static class Program
{
    public static void Main(string[] args)
    {
        Console.WriteLine("IntcoEdge.EdgeHost started.");
    }
}
