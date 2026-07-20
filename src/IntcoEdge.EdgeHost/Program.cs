using IntcoEdge.Common;
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
builder.Services.AddSingleton<YingkeGatewayClient>();

// 业务服务
builder.Services.AddSingleton<ILineRecordService, LineRecordService>();
builder.Services.AddSingleton<IAlarmService, AlarmService>();
builder.Services.AddSingleton<IYingkeService, YingkeService>();

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
// 读取 IntcoEdge:WebUi:Path 配置；若目录存在则 UseStaticFiles + MapFallbackToFile。
// - 开发模式：另起 vite dev server 在 5289，EdgeHost 5288 只做 API（PM 推荐方案）
// - 生产模式：npm run build 后 dist/ 出现，Path 指向它即可
// - 若目录不存在则静默跳过，仅 info 日志一行
var webUiPath = builder.Configuration["IntcoEdge:WebUi:Path"] ?? "wwwroot";
// 路径相对于当前工作目录（dotnet run 时 = EdgeHost 项目目录）
var webUiRoot = Path.IsPathRooted(webUiPath)
    ? webUiPath
    : Path.Combine(Directory.GetCurrentDirectory(), webUiPath);

var webUiMounted = false;
if (Directory.Exists(webUiRoot))
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
        "Web UI path '{Path}' not found, skipping static file serving (dev mode or pre-build)",
        webUiRoot);
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
        "  POST /client/data/alarm                      -> DetectController\n" +
        "  POST /client/yk/defect-record                -> DefectController\n" +
        "  POST /client/yk/defect-records               -> DefectController\n" +
        "  POST /client/yk/login                        -> YkController\n" +
        "  GET  /client/yk/line-defect                  -> YkController\n" +
        "  POST /client/yk/defect-query                 -> YkController\n",
        "text/plain; charset=utf-8"));
}

// 控制器路由
app.MapControllers();

// SPA fallback：仅在 Web UI 挂载后注册，排在 API 路由之后——
// 确保 /health、/client/* 等后端路由优先匹配，vue-router 路径走 index.html
if (webUiMounted)
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
    ykOpt.ApiType);

app.Run();

// 让 Program 类型可被集成测试访问（最小 API 默认 internal）
public partial class Program { }
