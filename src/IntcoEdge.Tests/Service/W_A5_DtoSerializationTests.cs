using System.Text.Json;
using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using Xunit;

namespace IntcoEdge.Tests.Service;

/// <summary>
/// W-A5 新增 DTO 的序列化测试。
///
/// 重点：
///   - DefectQueryRequest / DefectQueryResponse 字段名都走 camelCase
///   - DefectQueryStatistics.ngRate 是 double（前端要 0.0123 这种小数）
///   - LineStatisticResponse.timeline / defectTypeTop5 数组不为 null
/// </summary>
public class W_A5_DtoSerializationTests
{
    private static readonly JsonSerializerOptions Opt = IntcoHttpClient.DefaultJsonOptions;

    [Fact]
    public void DefectQueryRequest_FieldsAreCamelCase()
    {
        var dto = new DefectQueryRequest
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
            LineNo = "L01",
            FaceNo = "A",
            DefectType = "黑点",
            Page = 2,
            PageSize = 50,
        };

        var json = JsonSerializer.Serialize(dto, Opt);

        Assert.Contains("\"startTime\":", json);
        Assert.Contains("\"endTime\":", json);
        Assert.Contains("\"lineNo\":", json);
        Assert.Contains("\"faceNo\":", json);
        Assert.Contains("\"defectType\":", json);
        Assert.Contains("\"page\":", json);
        Assert.Contains("\"pageSize\":", json);
        // ⚠️ 不应出现 PSM 兼容字段
        Assert.DoesNotContain("\"lindGroup\":", json);
    }

    [Fact]
    public void DefectQueryRequest_Defaults()
    {
        var dto = new DefectQueryRequest();
        Assert.Equal(1, dto.Page);
        Assert.Equal(20, dto.PageSize);
    }

    [Fact]
    public void DefectQueryResponse_RoundTrip_PreservesStructure()
    {
        var dto = new DefectQueryResponse
        {
            Total = 100,
            Rows = new List<DefectRecordRowDto>
            {
                new()
                {
                    Id = 1, LineNo = "L01", FaceNo = "A", GloveNo = "G1",
                    Result = 2, DefectType = "黑点", ImgList = "[]",
                    Time = "2026-07-20 14:00:00", ExceptFlag = 1,
                }
            },
            Statistics = new DefectQueryStatistics
            {
                TotalCount = 100,
                NgCount = 3,
                NgRate = 0.03,
                DefectTypeDistribution = new List<DefectTypeStatDto>
                {
                    new() { Type = "黑点", Count = 2 },
                    new() { Type = "破洞", Count = 1 },
                }
            }
        };

        var json = JsonSerializer.Serialize(dto, Opt);
        var back = JsonSerializer.Deserialize<DefectQueryResponse>(json, Opt);

        Assert.NotNull(back);
        Assert.Equal(100, back!.Total);
        Assert.Single(back.Rows);
        Assert.Equal("L01", back.Rows[0].LineNo);
        Assert.Equal(0.03, back.Statistics.NgRate);
        Assert.Equal(2, back.Statistics.DefectTypeDistribution.Count);
    }

    [Fact]
    public void DefectQueryResponse_EmptyList_NullSafeSerialization()
    {
        var dto = new DefectQueryResponse();
        var json = JsonSerializer.Serialize(dto, Opt);

        // rows / distribution 始终序列化为数组（不为 null），前端 v-for 才不会崩
        Assert.Contains("\"rows\":[]", json);
        Assert.Contains("\"defectTypeDistribution\":[]", json);
        // statistics 不能丢
        Assert.Contains("\"statistics\":", json);
    }

    [Fact]
    public void LineStatisticResponse_FieldsAreCamelCase()
    {
        var dto = new LineStatisticResponse
        {
            LineNo = "L01",
            Today = "2026-07-20",
            Total = 100, Right = 98, Ng = 2, NgRate = 0.02,
            DefectTypeTop5 = new List<DefectTopDto> { new() { Type = "黑点", Count = 2 } },
            Timeline = new List<LineTimelinePointDto> { new() { Time = "2026-07-20 14:00:00", Total = 50, Ng = 1 } }
        };

        var json = JsonSerializer.Serialize(dto, Opt);

        Assert.Contains("\"lineNo\":", json);
        Assert.Contains("\"today\":", json);
        Assert.Contains("\"ngRate\":0.02", json);
        Assert.Contains("\"defectTypeTop5\":", json);
        Assert.Contains("\"timeline\":", json);
    }

    [Fact]
    public void LineStatisticResponse_EmptyList_NullSafeSerialization()
    {
        var dto = new LineStatisticResponse();
        var json = JsonSerializer.Serialize(dto, Opt);
        Assert.Contains("\"defectTypeTop5\":[]", json);
        Assert.Contains("\"timeline\":[]", json);
    }

    [Fact]
    public void DictionaryDto_DefectTypeDict_CategoryNameMapped()
    {
        // categoryName 必须是中文，前端不用再做 i18n
        var dto = new DefectTypeDictDto
        {
            Id = 1,
            Name = "破洞",
            Category = 1,
            CategoryName = "破损",
            CountEnable = 1,
            AlarmEnable = 1,
        };
        var json = JsonSerializer.Serialize(dto, Opt);
        Assert.Contains("\"categoryName\":\"破损\"", json);
        Assert.Contains("\"countEnable\":1", json);
        Assert.Contains("\"alarmEnable\":1", json);
    }
}
