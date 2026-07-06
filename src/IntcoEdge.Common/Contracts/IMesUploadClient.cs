using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using IntcoEdge.Common.Models;

namespace IntcoEdge.Common.Contracts;

/// <summary>
/// MES 上传客户端（Mock/HTTP 适配器统一接口）。
/// </summary>
public interface IMesUploadClient
{
    /// <summary>登录并获取 Token/Ticket。None 模式可直接返回成功。</summary>
    Task<MesLoginResult> LoginAsync(CancellationToken ct);

    /// <summary>单条发送。</summary>
    Task<UploadResult> SendAsync(MesEvent evt, CancellationToken ct);

    /// <summary>批量发送。批内单条结果与 evts 顺序一一对应。</summary>
    Task<IReadOnlyList<UploadResult>> SendBatchAsync(IEnumerable<MesEvent> evts, CancellationToken ct);
}
