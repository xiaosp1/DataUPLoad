using IntcoEdge.Common;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Services;

/// <summary>
/// 产线检测记录服务（接收视觉软件推送后的入库 + 派生计算）。
/// ⚠️ 当前实现：占位 + 结构化日志。真正的 SQLite INSERT 留给 W-A5 写仓。
/// </summary>
public interface ILineRecordService
{
    /// <summary>处理一条检测数据：参数校验 + 入库 + 派生统计。</summary>
    /// <returns>写入的行数（含派生行）。失败抛异常。</returns>
    Task<int> HandleDetectDataAsync(DetectDataDto data, CancellationToken ct = default);
}

public class LineRecordService : ILineRecordService
{
    private readonly ILogger<LineRecordService> _logger;

    public LineRecordService(ILogger<LineRecordService> logger)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public Task<int> HandleDetectDataAsync(DetectDataDto data, CancellationToken ct = default)
    {
        if (data == null)
        {
            throw new ArgumentNullException(nameof(data));
        }
        if (string.IsNullOrWhiteSpace(data.LineNo))
        {
            throw new ArgumentException("lineNo 必填", nameof(data));
        }
        if (string.IsNullOrWhiteSpace(data.FaceNo))
        {
            throw new ArgumentException("faceNo 必填", nameof(data));
        }
        if (data.TodayData == null)
        {
            throw new ArgumentException("todayData 必填", nameof(data));
        }
        if (data.RealTimeData == null)
        {
            throw new ArgumentException("realTimeData 必填", nameof(data));
        }

        _logger.LogInformation(
            "HandleDetectDataAsync lineNo={LineNo} faceNo={FaceNo} totalNum={Total} ngNum={Ng} rtTotal={RtTotal}",
            data.LineNo,
            data.FaceNo,
            data.TodayData.TotalNum,
            data.TodayData.NgNum,
            data.RealTimeData.Total);

        // TODO(W-A5): 真正写入 SQLite line_day_record + status_record。
        // INSERT INTO line_day_record (right_count, error_count, line_no, time)
        //   VALUES (@right, @error, @lineNo, @time)
        //   ON CONFLICT (line_no, time) DO UPDATE SET right_count=excluded.right_count, error_count=excluded.error_count;
        // INSERT INTO status_record (...) VALUES (...);

        var rows = 1; // 占位：A5 落地后改为真实行数
        return Task.FromResult(rows);
    }
}
