using IntcoEdge.Common;
using Xunit;

namespace IntcoEdge.Tests;

/// <summary>
/// 公共常量单元测试。
/// 确保老板拍板的端口 / 路径常量不被无意改动。
/// </summary>
public class ConstantsTests
{
    [Fact]
    public void EdgeHostPort_ShouldBe5288()
    {
        // 老板 16:12 拍板：沿用现场老 EdgeHost 端口 5288
        Assert.Equal(5288, Constants.EdgeHostPort);
    }

    [Fact]
    public void HealthPath_ShouldBeSlashHealth()
    {
        Assert.Equal("/health", Constants.HealthPath);
    }

    [Fact]
    public void DefaultDbPath_ShouldPointToDataIntcoDb()
    {
        Assert.Equal("data/intco.db", Constants.DefaultDbPath);
    }
}
