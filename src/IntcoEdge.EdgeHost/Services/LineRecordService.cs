using IntcoEdge.Common;
using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Services;

/// <summary>
/// 产线检测记录服务（接收视觉软件推送后的入库 + 派生计算）。
/// W-A4：直接落到 SQLite `line_day_record` + `status_record` 表。
/// </summary>
public interface ILineRecordService
{
    /// <summary>处理一条检测数据：参数校验 + 入库 + 派生统计。</summary>
    /// <returns>写入的业务行数（line_day_record + status_record 新增之和，UPSERT 命中 UPDATE 不计）。失败抛异常。</returns>
    Task<int> HandleDetectDataAsync(DetectDataDto data, CancellationToken ct = default);
}

public class LineRecordService : ILineRecordService
{
    private readonly ILogger<LineRecordService> _logger;
    private readonly ILineRecordRepository _repo;

    public LineRecordService(ILogger<LineRecordService> logger, ILineRecordRepository repo)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _repo = repo ?? throw new ArgumentNullException(nameof(repo));
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

        // DTO → POJO 映射（仓储层不接受 DTO，避免反向依赖）
        var today = new LineDayRecordInput(
            TotalNum: data.TodayData.TotalNum ?? 0,
            NgNum: data.TodayData.NgNum ?? 0,
            StatisticTime: data.TodayData.StatisticTime ?? string.Empty,
            LineNo: data.LineNo!,
            FaceNo: data.FaceNo!);

        // 客户端设备标识：PSM 端没在 DTO 里传 deviceNo，用 lineNo + faceNo 拼一个稳定标识，
        // 这样 status_record 表里的 device_no 不会为空、device_name 也不会为 NULL。
        var clientDeviceNo = $"{data.LineNo}-{data.FaceNo}-client";
        var clientDeviceName = $"产线 {data.LineNo} 面 {data.FaceNo} 客户端";

        var statusTime = !string.IsNullOrWhiteSpace(data.RealTimeData.StartTime)
            ? data.RealTimeData.StartTime!
            : (data.TodayData.StatisticTime ?? DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));

        var status = new StatusRecordInput(
            Time: statusTime,
            LineNo: data.LineNo!,
            FaceNo: data.FaceNo!,
            DeviceNo: clientDeviceNo,
            DeviceName: clientDeviceName,
            Status: 1,    // 在线
            Type: 3);     // 客户端

        var rows = _repo.UpsertLineAndStatus(today, status);
        _logger.LogInformation(
            "HandleDetectDataAsync 入库成功 lineNo={LineNo} faceNo={FaceNo} rows={Rows}",
            data.LineNo, data.FaceNo, rows);

        return Task.FromResult(rows);
    }
}
