using System;
using System.Diagnostics;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using IntcoEdge.Desktop.Services;
using Microsoft.Win32;

namespace IntcoEdge.Desktop.Views;

/// <summary>
/// WatchdogView.xaml 的交互逻辑。
/// </summary>
public partial class WatchdogView : UserControl
{
    private WatchdogService? _svc;
    private const int LogMaxLines = 2000;

    public WatchdogView()
    {
        InitializeComponent();
        Loaded += (_, _) => InitFromConfig();
    }

    private void InitFromConfig()
    {
        AppendLog($"[{DateTime.Now:HH:mm:ss}] 看门狗视图已加载，默认目标：{ExePathBox.Text}");
        UpdateState(WatchdogState.Unknown, "未启动");
    }

    private WatchdogService BuildService()
    {
        var path = ExePathBox.Text?.Trim() ?? "";
        return new WatchdogService(path);
    }

    private void OnApplyExe(object sender, RoutedEventArgs e)
    {
        // 重新挂接
        if (_svc != null)
        {
            _svc.Log -= OnWatchdogLog;
            _svc.StateChanged -= OnStateChanged;
            _svc.Stop();
            _svc = null;
        }
        _svc = BuildService();
        _svc.Log += OnWatchdogLog;
        _svc.StateChanged += OnStateChanged;
        FooterText.Text = $"已应用配置：{_svc.ExePath}（存在：{(_svc.IsDeployed ? "是" : "否 → 显示未部署")}）";
        UpdateState(_svc.IsDeployed ? WatchdogState.Stopped : WatchdogState.NotDeployed,
            _svc.IsDeployed ? "已就绪，点击「启动看门狗」开始监控" : "目标 exe 不存在 → 未部署");
    }

    private void OnBrowseExe(object sender, RoutedEventArgs e)
    {
        var dlg = new OpenFileDialog
        {
            Title = "选择 EdgeHost.exe",
            Filter = "可执行文件 (*.exe)|*.exe|所有文件 (*.*)|*.*",
            CheckFileExists = false,
            FileName = "IntcoEdge.EdgeHost.exe",
        };
        if (dlg.ShowDialog() == true)
        {
            ExePathBox.Text = dlg.FileName;
        }
    }

    private void OnStart(object sender, RoutedEventArgs e)
    {
        if (_svc == null)
        {
            _svc = BuildService();
            _svc.Log += OnWatchdogLog;
            _svc.StateChanged += OnStateChanged;
        }
        _svc.Start();
    }

    private void OnStop(object sender, RoutedEventArgs e)
    {
        _svc?.Stop();
        UpdateState(WatchdogState.Stopped, "看门狗已停止");
    }

    private void OnManualStart(object sender, RoutedEventArgs e)
    {
        if (_svc == null)
        {
            MessageBox.Show("请先点击「应用」配置目标 exe", "提示", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }
        if (!_svc.IsDeployed)
        {
            MessageBox.Show($"目标不存在：{_svc.ExePath}", "无法启动", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }
        var ok = _svc.TryStartProcess();
        AppendLog(ok
            ? $"[{DateTime.Now:HH:mm:ss}] 已发出手动启动指令"
            : $"[{DateTime.Now:HH:mm:ss}] 手动启动失败（请检查路径与权限）");
    }

    private void OnRefresh(object sender, RoutedEventArgs e)
    {
        if (_svc == null)
        {
            AppendLog($"[{DateTime.Now:HH:mm:ss}] 未配置目标");
            return;
        }
        var procs = _svc.FindEdgeHostProcesses();
        if (procs.Count == 0)
        {
            AppendLog($"[{DateTime.Now:HH:mm:ss}] 未发现 {_svc.ExeName} 进程");
        }
        else
        {
            foreach (var p in procs)
            {
                AppendLog($"[{DateTime.Now:HH:mm:ss}] 发现 PID={p.Id} Title={SafeTitle(p)} StartTime={SafeStartTime(p)}");
            }
        }
    }

    private void OnWatchdogLog(object? sender, string line)
    {
        Dispatcher.Invoke(() =>
        {
            AppendLog(line);
        });
    }

    private void OnStateChanged(object? sender, WatchdogState s)
    {
        Dispatcher.Invoke(() =>
        {
            UpdateState(s, $"状态变更：{s}");
            if (_svc != null)
            {
                RestartCountText.Text = _svc.TotalRestarts.ToString();
                LastCheckText.Text = (_svc.LastCheckUtc ?? DateTime.UtcNow).ToLocalTime().ToString("HH:mm:ss");
            }
        });
    }

    private void UpdateState(WatchdogState s, string msg)
    {
        StateText.Text = s switch
        {
            WatchdogState.Running => "运行中",
            WatchdogState.Stopped => "已停止",
            WatchdogState.Restarting => "正在重启",
            WatchdogState.Crashed => "崩溃",
            WatchdogState.NotDeployed => "未部署",
            _ => "未知",
        };
        StateBadge.Background = s switch
        {
            WatchdogState.Running => new SolidColorBrush(Color.FromRgb(0x4C, 0xAF, 0x50)),
            WatchdogState.Restarting => new SolidColorBrush(Color.FromRgb(0xFF, 0x98, 0x00)),
            WatchdogState.Crashed => new SolidColorBrush(Color.FromRgb(0xF4, 0x43, 0x36)),
            WatchdogState.NotDeployed => new SolidColorBrush(Color.FromRgb(0x9E, 0x9E, 0x9E)),
            _ => new SolidColorBrush(Color.FromRgb(0x60, 0x7D, 0x8B)),
        };
        FooterText.Text = $"[{DateTime.Now:HH:mm:ss}] {msg}";
    }

    private void AppendLog(string line)
    {
        if (LogBox.LineCount > LogMaxLines)
        {
            // 截断最前面 200 行
            var text = LogBox.Text;
            var idx = 0;
            for (int i = 0; i < 200 && idx < text.Length; i++)
            {
                idx = text.IndexOf('\n', idx) + 1;
            }
            if (idx > 0 && idx < text.Length)
            {
                LogBox.Text = text.Substring(idx);
            }
        }
        LogBox.AppendText(line + Environment.NewLine);
        LogBox.ScrollToEnd();
    }

    private static string SafeTitle(Process p)
    {
        try { return p.MainWindowTitle ?? "(无标题)"; } catch { return "(无法读取)"; }
    }

    private static string SafeStartTime(Process p)
    {
        try { return p.StartTime.ToString("yyyy-MM-dd HH:mm:ss"); } catch { return "(无法读取)"; }
    }
}
