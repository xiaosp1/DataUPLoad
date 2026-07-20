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
    public void YkLoginRequest_WrapsStringInValueField()
    {
        var dto = YkLoginRequest.Wrap("HKSJSB");
        var json = JsonSerializer.Serialize(dto, Opt);
        // ★ PSM StringParamDTO 字段名 = Value
        Assert.Contains("\"Value\":\"HKSJSB\"", json);
    }

    [Fact]
    public void YkLoginResponse_RoundTrip()
    {
        var dto = new YkLoginResponse
        {
            UserId = 50001.0,
            EmployeeId = 60002.0,
            UserCode = "HKSJSB",
            UserName = "海康视觉设备[HKSJSB]",
            InvOrg = 1
        };
        var json = JsonSerializer.Serialize(dto, Opt);
        var back = JsonSerializer.Deserialize<YkLoginResponse>(json, Opt);
        Assert.NotNull(back);
        Assert.Equal(50001.0, back!.UserId);
        Assert.Equal(1, back.InvOrg);
    }

    [Fact]
    public void YkListParam_WrapsListInValueField()
    {
        var inner = new List<AlarmPushDto>
        {
            new() { WorkShop = "WS01", Line = "L01", Face = "A1" }
        };
        var wrapped = YkListParam<AlarmPushDto>.Wrap(inner);
        var json = JsonSerializer.Serialize(wrapped, Opt);
        // ★ PSM ListParamsDTO 字段名 = Value
        Assert.Contains("\"Value\":[", json);
        Assert.Contains("\"WorkShop\":\"WS01\"", json);
    }

    [Fact]
    public void YkRequestDto_LoginFormat_MatchesAuthoritativeProtocol()
    {
        // 权威协议 3.1 的请求体：
        // {
        //   "ApiType": "AuthenticationController",
        //   "Method": "Login",
        //   "Parameters": [
        //     { "Value": "HKSJSB" },
        //     { "Value": "HKSJSB123" }
        //   ],
        //   "Context": {}
        // }
        var req = new YkRequestDto
        {
            ApiType = "AuthenticationController",
            Method = "Login",
            Parameters = new List<object>
            {
                YkLoginRequest.Wrap("HKSJSB"),
                YkLoginRequest.Wrap("HKSJSB123"),
            },
            Context = null,
        };
        var json = JsonSerializer.Serialize(req, Opt);
        Assert.Contains("\"ApiType\":\"AuthenticationController\"", json);
        Assert.Contains("\"Method\":\"Login\"", json);
        Assert.Contains("\"Parameters\":[", json);
        Assert.Contains("{\"Value\":\"HKSJSB\"}", json);
        Assert.Contains("{\"Value\":\"HKSJSB123\"}", json);
        // Context=null 时不写入 JSON（避免英科网关拿到空对象而非缺失字段）
        Assert.DoesNotContain("\"Context\":", json);
    }

    [Fact]
    public void YkRequestDto_PushAlarmFormat_MatchesAuthoritativeProtocol()
    {
        // 权威协议 3.2 的请求体：
        // {
        //   "ApiType": "VisualInspectionController",
        //   "Method": "HandleVisualInspectionAlarm",
        //   "Parameters": [
        //     { "Value": [ { WorkShop, Line, Face, AlarmTime, ... } ] }
        //   ],
        //   "Context": { Ticket: "...", InvOrgId: 1 }
        // }
        var req = new YkRequestDto
        {
            ApiType = "VisualInspectionController",
            Method = "HandleVisualInspectionAlarm",
            Parameters = new List<object>
            {
                YkListParam<AlarmPushDto>.Wrap(new List<AlarmPushDto>
                {
                    new()
                    {
                        WorkShop = "QZN2",
                        Line = "L01",
                        Face = "A1",
                        AlarmTime = "2026-07-20T14:30:00",
                        AlarmType = "defect",
                        AlarmLevel = "严重",
                        AlarmDetails = "底面破损",
                        AlarmResult = "已处理",
                        AlarmCount = 1,
                    }
                }),
            },
            Context = new YkContextDto { Ticket = "test-ticket", InvOrgId = 1 },
        };
        var json = JsonSerializer.Serialize(req, Opt);
        Assert.Contains("\"ApiType\":\"VisualInspectionController\"", json);
        Assert.Contains("\"Method\":\"HandleVisualInspectionAlarm\"", json);
        Assert.Contains("\"Parameters\":[{", json);
        // ★ 关键：Parameters[0] = {Value: [...]}，列表里包含业务对象
        Assert.Contains("\"Value\":[{", json);
        Assert.Contains("\"WorkShop\":\"QZN2\"", json);
        Assert.Contains("\"Line\":\"L01\"", json);
        Assert.Contains("\"AlarmCount\":1", json);
        Assert.Contains("\"Context\":{\"Ticket\":\"test-ticket\",\"InvOrgId\":1}", json);
    }

    [Fact]
    public void YkContextDto_FieldNames_PascalCase()
    {
        var ctx = new YkContextDto { Ticket = "tk", InvOrgId = 1 };
        var json = JsonSerializer.Serialize(ctx, Opt);
        Assert.Contains("\"Ticket\":\"tk\"", json);
        Assert.Contains("\"InvOrgId\":1", json);
    }

    [Fact]
    public void YkResponseDto_DeserializesContextAndResult()
    {
        // 模拟英科登录响应：Result 是 LoginResult，Context.Ticket 才是凭证
        var json = """
        {
          "Success": true,
          "Message": null,
          "Result": {
            "UserId": "50001",
            "EmployeeId": "60002",
            "UserCode": "HKSJSB",
            "UserName": "海康视觉设备[HKSJSB]",
            "InvOrg": 1
          },
          "Context": {
            "Ticket": "abc123-ticket-xxx",
            "InvOrgId": 1
          }
        }
        """;
        var resp = JsonSerializer.Deserialize<YkResponseDto>(json, Opt);
        Assert.NotNull(resp);
        Assert.True(resp!.Success);
        Assert.NotNull(resp.Context);
        Assert.Equal("abc123-ticket-xxx", resp.Context!.Ticket);
        Assert.Equal(1, resp.Context.InvOrgId);
        var login = resp.DeserializeResult<YkLoginResponse>();
        Assert.NotNull(login);
        Assert.Equal("HKSJSB", login!.UserCode);
        Assert.Equal(1, login.InvOrg);
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
