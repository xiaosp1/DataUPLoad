using System.Text.Json;
using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using Xunit;

namespace IntcoEdge.Tests;

/// <summary>
/// DTO 序列化往返测试：确保所有字段名（含 typo `lindGroup` / AlarmPushDto 大写）严格对齐 PSM。
/// </summary>
public class DtoSerializationTests
{
    private static readonly JsonSerializerOptions Opt = IntcoHttpClient.DefaultJsonOptions;

    [Fact]
    public void DetectDataDto_RoundTrip_PreservesAllFields()
    {
        var dto = new DetectDataDto
        {
            FaceNo = "A1",
            LineNo = "L01",
            TodayData = new LineDayRecordDto
            {
                TotalNum = 1000,
                NgNum = 12,
                StatisticTime = "2026-07-20 14:55:00",
                Defects = new List<DefectCountDto>
                {
                    new() { Count = 5, Type = "001", Time = "2026-07-20 14:55:00", ShowFlag = 1 }
                }
            },
            RealTimeData = new RealtimeDataDto
            {
                Total = 500,
                NgCount = 8,
                RemoveTotal = 8,
                RemoveFail = 0,
                Efficiency = 99.2,
                TotalNgRate = 1.6,
                Occupancy = 30,
                OccupancyRate = 60.0,
                StartTime = "2026-07-20 14:55:00",
                Defects = new List<DefectCountDto>
                {
                    new() { Count = 2, Type = "002", Time = "14:55" }
                }
            }
        };

        var json = JsonSerializer.Serialize(dto, Opt);
        var back = JsonSerializer.Deserialize<DetectDataDto>(json, Opt);

        Assert.NotNull(back);
        Assert.Equal(dto.FaceNo, back!.FaceNo);
        Assert.Equal(dto.LineNo, back.LineNo);
        Assert.NotNull(back.TodayData);
        Assert.Equal(dto.TodayData!.TotalNum, back.TodayData!.TotalNum);
        Assert.Equal(dto.TodayData.NgNum, back.TodayData.NgNum);
        Assert.Equal(dto.TodayData.StatisticTime, back.TodayData.StatisticTime);
        Assert.Single(back.TodayData.Defects!);
        Assert.NotNull(back.RealTimeData);
        Assert.Equal(dto.RealTimeData!.Efficiency, back.RealTimeData!.Efficiency);
        Assert.Equal(dto.RealTimeData.OccupancyRate, back.RealTimeData.OccupancyRate);
    }

    [Fact]
    public void DetectDataDto_JsonFieldNames_CamelCase()
    {
        var dto = new DetectDataDto
        {
            FaceNo = "A1",
            LineNo = "L01",
            TodayData = new LineDayRecordDto { TotalNum = 1, NgNum = 0, StatisticTime = "2026-07-20 14:55:00" },
            RealTimeData = new RealtimeDataDto { Total = 1, NgCount = 0, RemoveTotal = 0, RemoveFail = 0, Efficiency = 100, TotalNgRate = 0, Occupancy = 0, OccupancyRate = 0, StartTime = "2026-07-20 14:55:00" }
        };

        var json = JsonSerializer.Serialize(dto, Opt);
        Assert.Contains("\"faceNo\":", json);
        Assert.Contains("\"lineNo\":", json);
        Assert.Contains("\"todayData\":", json);
        Assert.Contains("\"realTimeData\":", json);
        Assert.Contains("\"statisticTime\":", json);
        Assert.Contains("\"efficiency\":", json);
        Assert.Contains("\"occupancyRate\":", json);
    }

    [Fact]
    public void SearchDefectRecordDto_PreservesLindGroupTypo()
    {
        var dto = new SearchDefectRecordDto
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
            LindGroup = new List<string> { "L01", "L02" },
            DefectGroup = new List<string> { "001" },
            FaceGroup = new List<string> { "A1", "A2" }
        };

        var json = JsonSerializer.Serialize(dto, Opt);
        // ⚠️ 关键断言：字段名是 lindGroup（typo），不是 lineGroup
        Assert.Contains("\"lindGroup\":", json);
        Assert.DoesNotContain("\"lineGroup\":", json);
        Assert.Contains("\"defectGroup\":", json);
        Assert.Contains("\"faceGroup\":", json);

        var back = JsonSerializer.Deserialize<SearchDefectRecordDto>(json, Opt);
        Assert.NotNull(back);
        Assert.Equal(new[] { "L01", "L02" }, back!.LindGroup);
    }

    [Fact]
    public void AlarmRecordDto_RoundTrip()
    {
        var dto = new AlarmRecordDto
        {
            Uuid = "uuid-001",
            Time = "2026-07-20 14:55:00",
            Type = 1,
            LineNo = "L01",
            FaceNo = "A1",
            Level = 2,
            Message = "底面破损",
            Solve = 2,
            Reason = 1,
            DefectName = "001"
        };
        var json = JsonSerializer.Serialize(dto, Opt);
        var back = JsonSerializer.Deserialize<AlarmRecordDto>(json, Opt);
        Assert.NotNull(back);
        Assert.Equal(dto.Uuid, back!.Uuid);
        Assert.Equal(dto.Type, back.Type);
        Assert.Equal(dto.Level, back.Level);
        Assert.Equal(dto.Message, back.Message);
    }

    [Fact]
    public void AlarmPushDto_PreservesPascalCaseFieldNames()
    {
        var dto = new AlarmPushDto
        {
            WorkShop = "WS01",
            Line = "L01",
            Face = "A1",
            AlarmTime = "2026-07-20 14:55:00",
            AlarmType = "defect",
            AlarmLevel = "严重",
            AlarmDetails = "底面破损",
            AlarmResult = "未处理",
            AlarmCount = 3
        };

        var json = JsonSerializer.Serialize(dto, Opt);
        // ⚠️ 关键断言：PSM 端用了 @JsonProperty(value="Xxx") 大写，必须保留
        Assert.Contains("\"WorkShop\":", json);
        Assert.Contains("\"Line\":", json);
        Assert.Contains("\"Face\":", json);
        Assert.Contains("\"AlarmTime\":", json);
        Assert.Contains("\"AlarmType\":", json);
        Assert.Contains("\"AlarmLevel\":", json);
        Assert.Contains("\"AlarmDetails\":", json);
        Assert.Contains("\"AlarmResult\":", json);
        Assert.Contains("\"AlarmCount\":", json);

        var back = JsonSerializer.Deserialize<AlarmPushDto>(json, Opt);
        Assert.NotNull(back);
        Assert.Equal("WS01", back!.WorkShop);
        Assert.Equal(3, back.AlarmCount);
    }

    [Fact]
    public void YkLoginRequest_FieldsPascalCase()
    {
        var dto = new YkLoginRequest { WorkShopCode = "WS01" };
        var json = JsonSerializer.Serialize(dto, Opt);
        Assert.Contains("\"WorkShopCode\":", json);
    }

    [Fact]
    public void YkLoginResponse_RoundTrip()
    {
        var dto = new YkLoginResponse
        {
            UserId = "u001",
            EmployeeId = "e001",
            UserCode = "admin",
            UserName = "管理员",
            InvOrg = 100
        };
        var json = JsonSerializer.Serialize(dto, Opt);
        var back = JsonSerializer.Deserialize<YkLoginResponse>(json, Opt);
        Assert.NotNull(back);
        Assert.Equal("u001", back!.UserId);
        Assert.Equal(100, back.InvOrg);
    }

    [Fact]
    public void YkRequestDto_OfLoginRequest_SerializesApiType()
    {
        var req = new YkRequestDto<YkLoginRequest>
        {
            ApiType = "inkey.user.login",
            Method = "login",
            Parameters = new List<YkLoginRequest> { new() { WorkShopCode = "WS01" } }
        };
        var json = JsonSerializer.Serialize(req, Opt);
        Assert.Contains("\"ApiType\":\"inkey.user.login\"", json);
        Assert.Contains("\"Method\":\"login\"", json);
        Assert.Contains("\"Parameters\":", json);
        Assert.Contains("\"WorkShopCode\":\"WS01\"", json);
    }

    [Fact]
    public void DefectRecordDto_RoundTrip()
    {
        var dto = new DefectRecordDto
        {
            LineNo = "L01",
            FaceNo = "A1",
            GloveNo = "G-2026-001",
            Result = 2,
            DefectType = "001",
            ImgList = "[\"img1.jpg\",\"img2.jpg\"]",
            Time = "2026-07-20 14:55:00"
        };
        var json = JsonSerializer.Serialize(dto, Opt);
        var back = JsonSerializer.Deserialize<DefectRecordDto>(json, Opt);
        Assert.NotNull(back);
        Assert.Equal("L01", back!.LineNo);
        Assert.Equal("G-2026-001", back.GloveNo);
        Assert.Equal(2, back.Result);
    }

    [Fact]
    public void RealtimeDataDto_DoubleFieldsPreserved()
    {
        var dto = new RealtimeDataDto
        {
            Total = 100, NgCount = 5, RemoveTotal = 5, RemoveFail = 0,
            Efficiency = 95.5, TotalNgRate = 5.0, Occupancy = 30, OccupancyRate = 60.5,
            StartTime = "2026-07-20 14:55:00"
        };
        var json = JsonSerializer.Serialize(dto, Opt);
        Assert.Contains("\"efficiency\":95.5", json);
        Assert.Contains("\"totalNgRate\":5", json);
        Assert.Contains("\"occupancyRate\":60.5", json);

        var back = JsonSerializer.Deserialize<RealtimeDataDto>(json, Opt);
        Assert.Equal(95.5, back!.Efficiency);
        Assert.Equal(60.5, back.OccupancyRate);
    }
}
