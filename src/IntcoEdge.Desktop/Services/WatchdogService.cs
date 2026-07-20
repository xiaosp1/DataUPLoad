using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace IntcoEdge.Desktop.Services;

/// <summary>
/// 看门狗状态。
/// </summary>
public enum WatchdogState
{
    Unknown,
    NotDeployed,    // 目标 exe 不存在
    Stopped,        // 进程没在跑
    Running,        // 正常跑
    Restarting,     // 正在拉起
    Crashed         // 拉起后立刻又掉
}

/// <summary>
/// 看门狗：定时检查 EdgeHost 主进程是否在跑，挂了自动拉起。
///
/// 设计要点（看 PSM 桌面端 / Windows 服务常见做法）：
/// - 每 5 秒轮询一次；
/// - 用 Process.GetProcessesByName(name) 找进程（按 exe 文件名匹配，稳）；
/// - 检测不到时尝试启动 EdgeHost.exe；
/// - 启动 5 秒后仍未存活 → 标记 Crashed 等待下一次重试；
/// - 启动次数累计到上限后停止（避免无限循环耗资源）。
/// </summary>
public class WatchdogService
{
    private const int PollIntervalMs = 5000;
    private const int StartupGraceMs = 5000;
    private const int MaxConsecutiveRestarts = 5;

    private readonly string _exePath;
    private readonly string _exeName;        // 不含扩展名
    private readonly string _workingDir;
    private readonly string _arguments;

    private CancellationTokenSource? _cts;
    private Task? _loopTask;

    public event EventHandler<WatchdogState>? StateChanged;
    public event EventHandler<string>? Log;

    public WatchdogState State { get; private set; } = WatchdogState.Unknown;
    public DateTime? LastCheckUtc { get; private set; }
    public int TotalRestarts { get; private set; }
    public int ConsecutiveRestarts { get; private set; }
    public Process? CurrentProcess { get; private set; }

    public string ExePath => _exePath;
    public string ExeName => _exeName;
    public bool IsDeployed => File.Exists(_exePath);

    public WatchdogService(string exePath, string arguments = "")
    {
        _exePath = exePath;
        _exeName = Path.GetFileNameWithoutExtension(exePath);
        _workingDir = Path.GetDirectoryName(exePath) ?? ".";
        _arguments = arguments;
    }

    public void Start()
    {
        if (_loopTask != null) return;

        if (!IsDeployed)
        {
            SetState(WatchdogState.NotDeployed, $"目标可执行文件不存在：{_exePath}");
            return;
        }

        _cts = new CancellationTokenSource();
        _loopTask = Task.Run(() => RunLoopAsync(_cts.Token));
        EmitLog($"看门狗已启动 → 监控：{_exePath}");
    }

    public void Stop()
    {
        try
        {
            _cts?.Cancel();
            _loopTask?.Wait(2000);
        }
        catch { /* 关闭时忽略 */ }
        finally
        {
            _cts?.Dispose();
            _cts = null;
            _loopTask = null;
        }
        EmitLog("看门狗已停止");
    }

    public bool TryStartProcess()
    {
        try
        {
            var psi = new ProcessStartInfo
            {
                FileName = _exePath,
                WorkingDirectory = _workingDir,
                Arguments = _arguments,
                UseShellExecute = true,
                CreateNoWindow = false,
                WindowStyle = ProcessWindowStyle.Normal,
            };
            var p = Process.Start(psi);
            if (p == null)
            {
                EmitLog("Process.Start 返回 null");
                return false;
            }
            CurrentProcess = p;
            return true;
        }
        catch (Exception ex)
        {
            EmitLog($"启动失败：{ex.Message}");
            return false;
        }
    }

    public List<Process> FindEdgeHostProcesses()
    {
        try
        {
            return Process.GetProcessesByName(_exeName).ToList();
        }
        catch (Exception ex)
        {
            EmitLog($"枚举进程失败：{ex.Message}");
            return new List<Process>();
        }
    }

    private async Task RunLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            try
            {
                Tick();
            }
            catch (Exception ex)
            {
                EmitLog($"Tick 异常：{ex.Message}");
            }

            try
            {
                await Task.Delay(PollIntervalMs, ct);
            }
            catch (OperationCanceledException)
            {
                break;
            }
        }
    }

    private void Tick()
    {
        LastCheckUtc = DateTime.UtcNow;

        if (!IsDeployed)
        {
            SetState(WatchdogState.NotDeployed, "目标可执行文件不存在（未部署）");
            return;
        }

        var procs = FindEdgeHostProcesses();
        var alive = procs.Count > 0;

        if (alive)
        {
            ConsecutiveRestarts = 0;
            CurrentProcess = procs[0];
            SetState(WatchdogState.Running, $"{_exeName} PID={procs[0].Id} 存活（{procs.Count} 个进程）");
            return;
        }

        // 没了 → 重启
        if (ConsecutiveRestarts >= MaxConsecutiveRestarts)
        {
            SetState(WatchdogState.Crashed,
                $"已连续重启 {ConsecutiveRestarts} 次仍无法存活，停止拉起。请人工介入。");
            return;
        }

        SetState(WatchdogState.Restarting, $"检测到 {_exeName} 掉线，尝试拉起...");
        var ok = TryStartProcess();
        if (!ok)
        {
            SetState(WatchdogState.Crashed, "启动失败（请检查 exe 是否损坏或权限）");
            ConsecutiveRestarts++;
            return;
        }

        TotalRestarts++;
        ConsecutiveRestarts++;
        EmitLog($"已发出启动指令（第 {TotalRestarts} 次）");

        // 给 5 秒宽限时间
        Thread.Sleep(StartupGraceMs);
        var after = FindEdgeHostProcesses();
        if (after.Count == 0)
        {
            EmitLog("⚠️ 启动后 5 秒仍未存活，标记为 Crashed");
            SetState(WatchdogState.Crashed, "启动后 5 秒内进程消失");
        }
        else
        {
            CurrentProcess = after[0];
            SetState(WatchdogState.Running, $"已恢复，PID={after[0].Id}");
            ConsecutiveRestarts = 0;
        }
    }

    private void SetState(WatchdogState s, string message)
    {
        State = s;
        EmitLog(message);
        StateChanged?.Invoke(this, s);
    }

    private void EmitLog(string msg)
    {
        var line = $"[{DateTime.Now:HH:mm:ss}] {msg}";
        Log?.Invoke(this, line);
    }
}
