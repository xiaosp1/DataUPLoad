using System;
using System.IO;
using IntcoEdge.Common.Contracts;
using IntcoEdge.Common.Models;
using IntcoEdge.MesUpload.Clients;
using IntcoEdge.MesUpload.Services;
using IntcoEdge.Storage;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace IntcoEdge.MesUpload.DependencyInjection;

/// <summary>
/// MES 上传模块 DI 注册扩展。EdgeHost 调用 services.AddMesUpload(config) 一行完成注册。
/// </summary>
public static class ServiceCollectionExtensions
{
    public static IServiceCollection AddMesUpload(this IServiceCollection services, IConfiguration configuration)
    {
        if (services is null) throw new ArgumentNullException(nameof(services));
        if (configuration is null) throw new ArgumentNullException(nameof(configuration));

        services.Configure<EdgeOptions>(configuration.GetSection(EdgeOptions.SectionName));
        services.Configure<MesServerOptions>(configuration.GetSection(MesServerOptions.SectionName));
        services.Configure<UploadPolicy>(configuration.GetSection("UploadPolicy"));

        services.AddHttpClient("IntcoEdge.MesUpload");

        services.AddSingleton<MockMesClient>(sp =>
        {
            var opts = sp.GetRequiredService<IOptions<MesServerOptions>>();
            var logger = sp.GetService<ILogger<MockMesClient>>();
            return new MockMesClient(opts, logger);
        });

        services.AddSingleton<IMesUploadClient>(sp =>
        {
            var opts = sp.GetRequiredService<IOptions<MesServerOptions>>().Value;
            if (opts.UseMock) return sp.GetRequiredService<MockMesClient>();
            return new HttpMesClient(
                sp.GetRequiredService<IHttpClientFactory>(),
                sp.GetRequiredService<IOptions<MesServerOptions>>(),
                sp.GetRequiredService<IOptions<UploadPolicy>>(),
                sp.GetService<ILogger<HttpMesClient>>());
        });

        services.AddSingleton<IMesUploadQueue>(sp =>
        {
            var edge = sp.GetRequiredService<IOptions<EdgeOptions>>().Value;
            var dataDir = Path.Combine(edge.DataPath, "mes");
            Directory.CreateDirectory(dataDir);
            var dbPath = Path.Combine(dataDir, "mes_outbox.db");
            var schemaDir = Path.Combine(AppContext.BaseDirectory, "Schema");
            var logger = sp.GetService<ILogger<MesUploadQueue>>();
            var queue = new MesUploadQueue(dbPath, schemaDir, logger);
            try { queue.InitializeAsync().GetAwaiter().GetResult(); }
            catch (Exception ex) { logger?.LogError(ex, "MesUploadQueue 初始化失败"); }
            return queue;
        });

        services.AddHostedService<MesUploadWorker>();
        return services;
    }
}
