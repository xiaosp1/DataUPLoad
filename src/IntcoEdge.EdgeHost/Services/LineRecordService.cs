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
    private readonly IDefectRecordRepository _defectRepo;
    private readonly IDefectConversion _defectConv;

    public LineRecordService(
        ILogger<LineRecordService> logger,
        ILineRecordRepository repo,
        IDefectRecordRepository defectRepo,
        IDefectConversion defectConv)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _repo = repo ?? throw new ArgumentNullException(nameof(repo));
        _defectRepo = defectRepo ?? throw new ArgumentNullException(nameof(defectRepo));
        _defectConv = defectConv ?? throw new ArgumentNullException(nameof(defectConv));
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
            "HandleDetectDataAsync line/status 入库成功 lineNo={LineNo} faceNo={FaceNo} rows={Rows}",
            data.LineNo, data.FaceNo, rows);

        // ★ W-A7-Bug 修复 (PM 20:35): 把 defects 展开写入 defect_record
        // PSM 端 todayData.defects / realTimeData.defects 是按缺陷类型汇总的
        // (DefectCountDto.count=N 表示该类 N 条缺陷)，需展开 N 条 defect_record
        // 这样 /api/defect/query 才能返回真实数据给 MES。
        var defectRows = 0;
        try
        {
            var inputs = new List<DefectRecordInput>();
            if (data.TodayData.Defects != null && data.TodayData.Defects.Count > 0)
            {
                inputs.AddRange(_defectConv.FromDetectData(
                    data.LineNo!, data.FaceNo!,
                    data.TodayData.StatisticTime ?? string.Empty,
                    data.TodayData.Defects));
            }
            if (data.RealTimeData.Defects != null && data.RealTimeData.Defects.Count > 0)
            {
                inputs.AddRange(_defectConv.FromDetectData(
                    data.LineNo!, data.FaceNo!,
                    data.RealTimeData.StartTime ?? data.TodayData.StatisticTime ?? string.Empty,
                    data.RealTimeData.Defects));
            }
            if (inputs.Count > 0)
            {
                defectRows = _defectRepo.InsertBatch(inputs);
                _logger.LogInformation(
                    "HandleDetectDataAsync defect 入库成功 lineNo={LineNo} faceNo={FaceNo} defectRows={Rows}",
                    data.LineNo, data.FaceNo, defectRows);
            }
        }
        catch (Exception ex)
        {
            // defect_record 写失败不影响 line/status 入库（主业务已落库）
            _logger.LogWarning(ex,
                "HandleDetectDataAsync defect_record 写入失败 lineNo={LineNo}（不影响 line/status 业务）",
                data.LineNo);
        }

        return Task.FromResult(rows + defectRows);
    }
}
