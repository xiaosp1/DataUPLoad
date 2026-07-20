using System;
using System.Windows;
using System.Windows.Threading;

namespace IntcoEdge.Desktop;

/// <summary>
/// MainWindow.xaml 的交互逻辑。
/// 三个 Tab：看门狗 / PG 客户端 / 测试工具。
/// </summary>
public partial class MainWindow : Window
{
    private readonly DispatcherTimer _clockTimer;

    public MainWindow()
    {
        InitializeComponent();

        _clockTimer = new DispatcherTimer
        {
            Interval = TimeSpan.FromSeconds(1),
        };
        _clockTimer.Tick += (_, _) => TimeText.Text = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        _clockTimer.Start();
    }

    protected override void OnClosed(EventArgs e)
    {
        _clockTimer.Stop();
        base.OnClosed(e);
    }
}
