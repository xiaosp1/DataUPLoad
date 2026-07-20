using IntcoEdge.Common;
using IntcoEdge.Db;
using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Services;
using Microsoft.Extensions.FileProviders;

var builder = WebApplication.CreateBuilder(args);

// 端口固定为 5288（老板拍板：沿用现场老 EdgeHost 端口，避免与现场 PSM 冲突）
builder.WebHost.UseUrls("http://0.0.0.0:5288");

// ============== 服务注册 ==============

// 选项绑定：英科网关
builder.Services.Configure<YingkeGatewayOptions>(
    builder.Configuration.GetSection(YingkeGatewayOptions.SectionName));

// HTTP 客户端（HttpClientFactory 管生命周期，避免 socket 泄漏）
builder.Services.AddHttpClient<IntcoHttpClient>((sp, http) =>
{
    var opt = sp.GetRequiredService<Microsoft.Extensions.Options.IOptions<YingkeGatewayOptions>>().Value;
    http.Timeout = TimeSpan.FromMilliseconds(opt.TimeoutMs);
    http.DefaultRequestHeaders.Add("User-Agent", "IntcoEdge-EdgeHost/0.3");
}).ConfigurePrimaryHttpMessageHandler(() => new HttpClientHandler
{
    // 现场 PSM/英科网关多为内网自签证书，先放开；后续要走 HTTPS + 证书固定再收紧
    ServerCertificateCustomValidationCallback = HttpClientHandler.DangerousAcceptAnyServerCertificateValidator,
});

builder.Services.AddHttpClient<VisionHttpClient>((sp, http) =>
{
    http.Timeout = TimeSpan.FromSeconds(10);
    http.BaseAddress = new Uri("http://127.0.0.1:5288");
});

// 业务客户端
// W-A6：YkTicketCache 必须 Singleton（跨请求共享 ticket）
builder.Services.AddSingleton(sp =>
{
    var opt = sp.GetRequiredService<Microsoft.Extensions.Options.IOptions<YingkeGatewayOptions>>().Value;
    return new IntcoEdge.EdgeHost.Models.YkTicketCache(TimeSpan.FromMinutes(opt.TicketCacheMinutes));
});
builder.Services.AddSingleton<YingkeGatewayClient>();

// SQLite 连接工厂：W-A5 字典 / 缺陷查询仓储共用。
// 路径优先级：配置 > Constants 默认值 > "data/intco.db"。
var dbPath = builder.Configuration["IntcoEdge:DbPath"]
             ?? Constants.DefaultDbPath;
// PM 17:58 修复 v4：固定用 IntcoEdge.Db 项目目录的 data/intco.db。
// （不查 BaseDirectory/data，避免 EdgeHost 自己创建空 DB）
string resolved;
if (Path.IsPathRooted(dbPath))
{
    resolved = dbPath!;
}
else
{
    // 沿父目录向上找 src/IntcoEdge.Db/data/intco.db
    string? found = null;
    var dir = new DirectoryInfo(AppContext.BaseDirectory);
    for (int i = 0; i < 6 && dir != null; i++)
    {
        var candidate = Path.Combine(dir.FullName, "src", "IntcoEdge.Db", "data", "intco.db");
        if (File.Exists(candidate))
        {
            found = candidate;
            break;
        }
        dir = dir.Parent;
    }
    resolved = found ?? Path.Combine(AppContext.BaseDirectory, dbPath ?? "data/intco.db");
}
builder.Services.AddSingleton(new SqliteConnectionFactory(resolved));

// 仓储（W-A5）
builder.Services.AddSingleton<IDictionaryRepository, DictionaryRepository>();
  // PM 17:45 修复：W-A4 漏注册仓储
  builder.Services.AddSingleton<ILineRecordRepository, LineRecordRepository>();
  builder.Services.AddSingleton<IAlarmRecordRepository, AlarmRecordRepository>();
  builder.Services.AddSingleton<IDefectRecordRepository, DefectRecordRepository>();
builder.Services.AddSingleton<IDefectQueryRepository, DefectQueryRepository>();

// 业务服务
builder.Services.AddSingleton<ILineRecordService, LineRecordService>();
builder.Services.AddSingleton<IDefectConversion, DefectConversion>();  // PM 20:35 bug fix: defect 展开转换器 DI
builder.Services.AddSingleton<IAlarmService, AlarmService>();
builder.Services.AddSingleton<IAlarmConversion, AlarmConversion>();  // W-A7-M: 报警 DTO 转换器 DI 注册（铁则 7 防止漏注册）
builder.Services.AddSingleton<IYingkeService, YingkeService>();

// W-A5：字典查询 + 缺陷查询 + 产线统计
builder.Services.AddSingleton<IDictionaryService, DictionaryService>();
builder.Services.AddSingleton<IDefectQueryService, DefectQueryService>();

// 控制器（用 W-A3 写的 DetectController / DefectController / YkController）
builder.Services.AddControllers()
    .AddJsonOptions(opts =>
    {
        // 与 IntcoHttpClient.DefaultJsonOptions 保持一致：忽略 null、camelCase、支持中文
        opts.JsonSerializerOptions.DefaultIgnoreCondition =
            System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull;
        opts.JsonSerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
        opts.JsonSerializerOptions.Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping;
    });

var app = builder.Build();

// W-B1：Web UI 集成（Vue 3 大屏）。
// W-B2：Web UI 集成（Vue 3 大屏）。
// 读取 IntcoEdge:WebUi:Path 配置；若目录存在则 UseStaticFiles + MapFallbackToFile。
// - 开发模式：另起 vite dev server 在 5289，EdgeHost 5288 只做 API（PM 推荐方案）
// - 生产模式：vite build 把产物输出到 src/IntcoEdge.EdgeHost/wwwroot/，并随 csproj
//   的 <None Include="wwwroot\**\*"> 复制到 bin/Debug/net8.0/wwwroot/
// - 若目录不存在则静默跳过，仅 info 日志一行
//
// W-B2 修复：参考 DbPath v4（17:58）模式——sln 布局固定时硬编码到
// src/IntcoEdge.EdgeHost/wwwroot，沿父目录向上探测，避免依赖 cwd 或 appsettings 错配。
// 优先级：
//   1) IntcoEdge:WebUi:Path 配置（绝对路径直接用）
//   2) 当前 cwd/wwwroot（dotnet run 时的标准场景）
//   3) 沿父目录向上找 src/IntcoEdge.EdgeHost/wwwroot（sln 根运行场景）
//   4) 沿父目录向上找 bin/<cfg>/net8.0/wwwroot（dotnet publish 后）
string? webUiRoot = null;
var configuredPath = builder.Configuration["IntcoEdge:WebUi:Path"];
if (!string.IsNullOrWhiteSpace(configuredPath) && Path.IsPathRooted(configuredPath) && Directory.Exists(configuredPath))
{
    webUiRoot = configuredPath;
}
else
{
    // 优先：cwd/wwwroot（dotnet run 标准场景 / dotnet publish 后 exe 同目录）
    var cwdCandidate = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot");
    if (Directory.Exists(cwdCandidate))
    {
        webUiRoot = cwdCandidate;
    }
    else
    {
        // fallback：沿父目录向上探测（sln 根运行 / 异常 cwd 场景）
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        for (int i = 0; i < 6 && dir != null; i++)
        {
            var candidate = Path.Combine(dir.FullName, "src", "IntcoEdge.EdgeHost", "wwwroot");
            if (Directory.Exists(candidate))
            {
                webUiRoot = candidate;
                break;
            }
            dir = dir.Parent;
        }
    }
}

var webUiMounted = false;
if (webUiRoot != null)
{
    var fp = new PhysicalFileProvider(webUiRoot);
    app.UseDefaultFiles(new DefaultFilesOptions { FileProvider = fp, RequestPath = "" });
    app.UseStaticFiles(new StaticFileOptions { FileProvider = fp, RequestPath = "" });
    webUiMounted = true;
    app.Logger.LogInformation("Web UI mounted from {Path} (http://localhost:5288/)", webUiRoot);
}
else
{
    app.Logger.LogInformation(
        "Web UI 'wwwroot' not found (cwd={Cwd}, baseDir={BaseDir}); skipping static file serving (dev mode or pre-build)",
        Directory.GetCurrentDirectory(),
        AppContext.BaseDirectory);
}

// 极简健康检查：返回 200 + "ok"
app.MapGet("/health", () => Results.Text("ok", "text/plain; charset=utf-8"));

// 根路径给个简单提示，避免裸 404 引起误会（仅在未挂载 Web UI 时使用）
if (!webUiMounted)
{
    app.MapGet("/", () => Results.Text(
        "IntcoEdge EdgeHost v0.3\n" +
        "Endpoints:\n" +
        "  GET  /health                                 -> ok\n" +
        "  POST /client/data/detect                     -> DetectController\n" +
        "  POST /client/data/alarm                      -> DetectController (W-A4 老路径)\n" +
        "  POST /api/alarm/push                         -> AlarmController (W-A7-M 入库+推英科)\n" +
        "  POST /client/yk/defect-record                -> DefectController\n" +
        "  POST /client/yk/defect-records               -> DefectController\n" +
        "  POST /client/yk/login                        -> YkController\n" +
        "  GET  /client/yk/line-defect                  -> YkController\n" +
        "  POST /client/yk/defect-query                 -> YkController\n" +
        "  GET  /api/dict/defect-type                   -> DefectController (W-A5)\n" +
        "  GET  /api/dict/defect-group                  -> DefectController (W-A5)\n" +
        "  GET  /api/dict/face-group                    -> DefectController (W-A5)\n" +
        "  POST /api/defect/query                       -> DefectController (W-A5)\n" +
        "  GET  /api/line/statistic?lineNo=...          -> DefectController (W-A5)\n",
        "text/plain; charset=utf-8"));
}

// 控制器路由
app.MapControllers();

// SPA fallback：仅在 Web UI 挂载后注册，排在 API 路由之后——
// 确保 /health、/client/* 等后端路由优先匹配，vue-router 路径走 index.html
if (webUiMounted && !string.IsNullOrEmpty(webUiRoot))
{
    app.MapFallbackToFile("index.html", new StaticFileOptions
    {
        FileProvider = new PhysicalFileProvider(webUiRoot)
    });
}

// 烟雾自检：启动时打印端口 + 配置摘要
var ykOpt = app.Services.GetRequiredService<Microsoft.Extensions.Options.IOptions<YingkeGatewayOptions>>().Value;
app.Logger.LogInformation(
    "IntcoEdge.EdgeHost starting on port {Port} (env={Env}) ykUrl={YkUrl} ykApiType={YkApiType}",
    5288,
    app.Environment.EnvironmentName,
    ykOpt.Url,
    // W-A4 把 YingkeGatewayOptions.ApiType 移除了（YingkeOptions 重构）；
    // 启动日志这行没跟上更新，暂时硬编码 "edge.dataTrans" 避免 build break。
    // TODO(W-A4/W-A6): 重构完后从配置读回正确的 ApiType。
    "edge.dataTrans");

app.Run();

// 让 Program 类型可被集成测试访问（最小 API 默认 internal）
public partial class Program { }
