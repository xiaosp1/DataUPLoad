using System;
using System.Data;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using IntcoEdge.Desktop.Services;
using Microsoft.Win32;

namespace IntcoEdge.Desktop.Views;

/// <summary>
/// PgClientView.xaml 的交互逻辑。
///
/// 注：任务原文叫"PG 客户端"，但本项目数据库是 SQLite（沿用现场）。
/// 这里实现的是 SQLite 浏览器 + 编辑器，与 PG 客户端定位一致。
/// </summary>
public partial class PgClientView : UserControl
{
    private readonly SqliteBrowserService _svc = new();
    private string _currentTable = "";

    public PgClientView()
    {
        InitializeComponent();
        _svc.Log += OnServiceLog;
        Loaded += (_, _) =>
        {
            FooterText.Text = "就绪。数据库文件不存在时，「连接」会失败提示。";
        };
    }

    private void OnBrowse(object sender, RoutedEventArgs e)
    {
        var dlg = new OpenFileDialog
        {
            Title = "选择 SQLite 数据库文件",
            Filter = "SQLite (*.db;*.sqlite;*.sqlite3)|*.db;*.sqlite;*.sqlite3|所有文件 (*.*)|*.*",
            CheckFileExists = false,
        };
        if (dlg.ShowDialog() == true)
        {
            DbPathBox.Text = dlg.FileName;
        }
    }

    private void OnConnect(object sender, RoutedEventArgs e)
    {
        var path = DbPathBox.Text?.Trim() ?? "";
        if (string.IsNullOrEmpty(path))
        {
            MessageBox.Show("请先填数据库路径", "提示", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }
        if (!File.Exists(path))
        {
            AppendLog($"⚠️ 文件不存在：{path}");
            ConnStatusText.Text = "文件不存在";
            ConnStatusText.Foreground = System.Windows.Media.Brushes.Red;
            return;
        }

        var ok = _svc.Open(path);
        ConnStatusText.Text = ok ? "已连接" : "连接失败";
        ConnStatusText.Foreground = ok ? System.Windows.Media.Brushes.Green : System.Windows.Media.Brushes.Red;
        if (ok)
        {
            RefreshTableList();
            FooterText.Text = $"已连接 → {path}";
        }
    }

    private void OnDisconnect(object sender, RoutedEventArgs e)
    {
        _svc.Close();
        TableList.Items.Clear();
        DataGridView.ItemsSource = null;
        ConnStatusText.Text = "未连接";
        ConnStatusText.Foreground = System.Windows.Media.Brushes.Gray;
        FooterText.Text = "已断开";
    }

    private void RefreshTableList()
    {
        TableList.Items.Clear();
        var tables = _svc.ListTables();
        foreach (var t in tables)
        {
            TableList.Items.Add(t);
        }
        AppendLog($"📋 共 {tables.Count} 张表/视图");
        FooterText.Text = $"已加载 {tables.Count} 张表";
    }

    private void OnRefreshTables(object sender, RoutedEventArgs e)
    {
        if (!_svc.IsOpen)
        {
            MessageBox.Show("请先连接数据库", "提示", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }
        RefreshTableList();
    }

    private void OnTableChanged(object sender, SelectionChangedEventArgs e)
    {
        if (TableList.SelectedItem is string s)
        {
            _currentTable = s;
            FooterText.Text = $"已选中：{s}（双击或点「加载」查看数据）";
        }
    }

    private void OnTableDblClick(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        if (TableList.SelectedItem is string s)
        {
            LoadTable(s);
        }
    }

    private void OnLoadCurrentTable(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrEmpty(_currentTable))
        {
            MessageBox.Show("请先在表列表里选一张表", "提示", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }
        LoadTable(_currentTable);
    }

    private void LoadTable(string tableName)
    {
        if (!_svc.IsOpen) return;
        try
        {
            var dt = _svc.QueryTable(tableName);
            dt.TableName = tableName;
            DataGridView.ItemsSource = dt.DefaultView;
            FooterText.Text = $"已加载 {tableName}（{dt.Rows.Count} 行）";
        }
        catch (Exception ex)
        {
            MessageBox.Show($"加载失败：{ex.Message}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private void OnSaveChanges(object sender, RoutedEventArgs e)
    {
        if (DataGridView.ItemsSource is DataView dv && dv.Table != null)
        {
            var n = _svc.SaveChanges(dv.Table);
            if (n < 0)
            {
                MessageBox.Show("保存失败（看日志）", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            else
            {
                FooterText.Text = $"✅ 保存完成，影响 {n} 行";
            }
        }
        else
        {
            MessageBox.Show("当前数据不是表数据视图，无法保存", "提示", MessageBoxButton.OK, MessageBoxImage.Information);
        }
    }

    private void OnExecSql(object sender, RoutedEventArgs e)
    {
        if (!_svc.IsOpen)
        {
            MessageBox.Show("请先连接数据库", "提示", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }
        var sql = SqlBox.Text?.Trim() ?? "";
        if (string.IsNullOrEmpty(sql)) return;

        var (isSelect, dt, affected) = _svc.ExecuteSql(sql);
        if (isSelect)
        {
            DataGridView.ItemsSource = dt.DefaultView;
            FooterText.Text = $"✅ SELECT → {dt.Rows.Count} 行（已加载到数据表）";
        }
        else
        {
            FooterText.Text = affected >= 0 ? $"✅ 非查询 → {affected} 行受影响" : "❌ 执行失败（看日志）";
        }
    }

    private void OnClearSql(object sender, RoutedEventArgs e)
    {
        SqlBox.Clear();
    }

    private void OnLoadSqlToGrid(object sender, RoutedEventArgs e)
    {
        OnExecSql(sender, e);
    }

    private void OnServiceLog(object? sender, string line)
    {
        Dispatcher.Invoke(() => AppendLog(line));
    }

    private void AppendLog(string line)
    {
        SqlLogBox.AppendText(line + Environment.NewLine);
        SqlLogBox.ScrollToEnd();
    }
}
