using IntcoEdge.Db;
using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Services;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace IntcoEdge.Tests.Service;

/// <summary>
/// 字典服务单元测试（W-A5 / 1）。
///
/// 测试策略：
///   - 缺陷分组 / 面别：纯常量返回，断言内容稳定性（PM 拍板）。
///   - 缺陷类型：DB 不存在时返空集（不是异常），不依赖 fixture。
/// </summary>
public class DictionaryServiceTests
{
    [Fact]
    public void GetDefectGroups_ReturnsFourFixedGroups()
    {
        var svc = NewService(dbExists: false);
        var groups = svc.GetDefectGroups();

        Assert.Equal(4, groups.Count);
        // 顺序固定：shape / spot / stain / hole
        Assert.Equal("shape", groups[0].Code);
        Assert.Equal("spot",  groups[1].Code);
        Assert.Equal("stain", groups[2].Code);
        Assert.Equal("hole",  groups[3].Code);

        // 中文名稳定（前端依赖这些文案）
        Assert.Equal("外形", groups[0].Name);
        Assert.Equal("黑点", groups[1].Name);
        Assert.Equal("污渍", groups[2].Name);
        Assert.Equal("破洞", groups[3].Name);
    }

    [Fact]
    public void GetFaceGroups_ReturnsABFace()
    {
        var svc = NewService(dbExists: false);
        var faces = svc.GetFaceGroups();

        Assert.Equal(2, faces.Count);
        Assert.Equal("A", faces[0].Code);
        Assert.Equal("A面", faces[0].Name);
        Assert.Equal("B", faces[1].Code);
        Assert.Equal("B面", faces[1].Name);
    }

    [Fact]
    public void GetDefectTypes_DbNotExists_ReturnsEmpty()
    {
        // DB 文件不存在时（首次启动 / migration runner 还没跑），不能抛异常，应返回空列表。
        var svc = NewService(dbExists: false);
        var types = svc.GetDefectTypes();

        Assert.NotNull(types);
        Assert.Empty(types);
    }

    [Fact]
    public void GetDefectGroups_HasExpectedShapeStainSpotHole()
    {
        // 业务校验：每个分组中文名都不是空，且互不重复
        var svc = NewService(dbExists: false);
        var groups = svc.GetDefectGroups();

        var names = groups.Select(g => g.Name).ToList();
        Assert.Equal(names.Count, names.Distinct().Count());
        Assert.All(names, n => Assert.False(string.IsNullOrWhiteSpace(n)));
    }

    // ---------- 辅助 ----------

    private static DictionaryService NewService(bool dbExists)
    {
        // 用一个不存在的目录作为 DB 路径，触发"DB 不存在"分支
        var fakeDb = dbExists
            ? (string?)null
            : Path.Combine(Path.GetTempPath(), $"intco-fake-{Guid.NewGuid():N}.db");
        var factory = new SqliteConnectionFactory(fakeDb);
        var repo = new DictionaryRepository(factory);
        return new DictionaryService(repo, NullLogger<DictionaryService>.Instance);
    }
}
