using System.Windows;

namespace IntcoEdge.Desktop;

/// <summary>
/// App.xaml 的交互逻辑入口。
/// 全局异常捕获，确保桌面工具崩溃时有可见日志。
/// </summary>
public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        // 捕获 UI 线程未处理异常
        DispatcherUnhandledException += (s, args) =>
        {
            MessageBox.Show(
                $"界面线程异常：\n{args.Exception}",
                "IntcoEdge Desktop — 未处理异常",
                MessageBoxButton.OK,
                MessageBoxImage.Error);
            args.Handled = true;
        };
    }
}
