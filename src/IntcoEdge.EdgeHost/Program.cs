using IntcoEdge.Common;

var builder = WebApplication.CreateBuilder(args);

// 端口固定为 5288（老板拍板：沿用现场老 EdgeHost 端口，避免与现场 PSM 冲突）
builder.WebHost.UseUrls("http://0.0.0.0:5288");

var app = builder.Build();

// 极简健康检查：返回 200 + "ok"
app.MapGet("/health", () => Results.Text("ok", "text/plain; charset=utf-8"));

// 根路径给个简单提示，避免裸 404 引起误会
app.MapGet("/", () => Results.Text(
    "IntcoEdge EdgeHost v0.3\n" +
    "Endpoints:\n" +
    "  GET /health  -> ok\n",
    "text/plain; charset=utf-8"));

// 烟雾自检：启动时打印端口 + 配置摘要
app.Logger.LogInformation(
    "IntcoEdge.EdgeHost starting on port {Port} (env={Env})",
    5288,
    app.Environment.EnvironmentName);

app.Run();

// 让 Program 类型可被集成测试访问（最小 API 默认 internal）
public partial class Program { }
