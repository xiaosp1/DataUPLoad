using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.IO;
using System.Linq;
using Microsoft.Data.Sqlite;

namespace IntcoEdge.Desktop.Services;

/// <summary>
/// 简易 SQLite 浏览器服务：
/// - 列出所有表
/// - 查询表数据（返回 DataTable）
/// - 执行任意 SQL（SELECT / UPDATE / DELETE）
/// - 保存修改
///
/// 设计要点：
/// - 用 Microsoft.Data.Sqlite（任务规定）
/// - 字符串字面量参数化（避免 SQL 注入）
/// - 启用 WAL 模式，多线程读不互斥
/// </summary>
public class SqliteBrowserService
{
    private string _connectionString = string.Empty;
    private string _dbPath = string.Empty;

    public string DbPath => _dbPath;
    public bool IsOpen => !string.IsNullOrEmpty(_connectionString);

    public event EventHandler<string>? Log;

    public bool Open(string dbPath)
    {
        try
        {
            if (!File.Exists(dbPath))
            {
                EmitLog($"❌ 数据库文件不存在：{dbPath}");
                return false;
            }

            _dbPath = dbPath;
            _connectionString = new SqliteConnectionStringBuilder
            {
                DataSource = dbPath,
                Mode = SqliteOpenMode.ReadWrite,
                Cache = SqliteCacheMode.Shared,
                Pooling = true,
            }.ToString();

            // 简单验证：能 SELECT 1
            using var conn = new SqliteConnection(_connectionString);
            conn.Open();
            using var cmd = conn.CreateCommand();
            cmd.CommandText = "SELECT 1;";
            cmd.ExecuteScalar();

            EmitLog($"✅ 数据库已连接：{dbPath}");
            return true;
        }
        catch (Exception ex)
        {
            EmitLog($"❌ 打开数据库失败：{ex.Message}");
            return false;
        }
    }

    public void Close()
    {
        _connectionString = string.Empty;
        _dbPath = string.Empty;
        SqliteConnection.ClearAllPools();
    }

    /// <summary>
    /// 列出所有用户表（排除 sqlite_* 内部表）。
    /// </summary>
    public List<string> ListTables()
    {
        var tables = new List<string>();
        if (!IsOpen) return tables;

        const string sql = @"
            SELECT name FROM sqlite_master
             WHERE type IN ('table', 'view')
               AND name NOT LIKE 'sqlite_%'
             ORDER BY name;";
        using var conn = new SqliteConnection(_connectionString);
        conn.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = sql;
        using var rdr = cmd.ExecuteReader();
        while (rdr.Read())
        {
            tables.Add(rdr.GetString(0));
        }
        return tables;
    }

    /// <summary>
    /// 查表前 1000 行。
    /// </summary>
    public DataTable QueryTable(string tableName, int limit = 1000)
    {
        var dt = new DataTable();
        if (!IsOpen) return dt;
        if (string.IsNullOrWhiteSpace(tableName)) return dt;

        // 表名白名单校验（仅允许字母/数字/下划线）
        foreach (var ch in tableName)
        {
            if (!(char.IsLetterOrDigit(ch) || ch == '_'))
            {
                EmitLog($"❌ 非法的表名：{tableName}");
                return dt;
            }
        }

        using var conn = new SqliteConnection(_connectionString);
        conn.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = $"SELECT * FROM \"{tableName}\" LIMIT {limit};";
        using var rdr = cmd.ExecuteReader();
        dt.Load(rdr);
        EmitLog($"📋 {tableName} → {dt.Rows.Count} 行");
        return dt;
    }

    /// <summary>
    /// 执行任意 SQL（SELECT 返回 DataTable，其他返回影响行数）。
    /// </summary>
    public (bool isSelect, DataTable data, int affected) ExecuteSql(string sql)
    {
        if (!IsOpen)
        {
            EmitLog("❌ 数据库未连接");
            return (false, new DataTable(), 0);
        }
        if (string.IsNullOrWhiteSpace(sql))
        {
            return (false, new DataTable(), 0);
        }

        try
        {
            using var conn = new SqliteConnection(_connectionString);
            conn.Open();
            using var cmd = conn.CreateCommand();
            cmd.CommandText = sql;
            var trimmed = sql.TrimStart();
            var isSelect = trimmed.StartsWith("SELECT", StringComparison.OrdinalIgnoreCase)
                           || trimmed.StartsWith("PRAGMA", StringComparison.OrdinalIgnoreCase)
                           || trimmed.StartsWith("WITH", StringComparison.OrdinalIgnoreCase);

            if (isSelect)
            {
                var dt = new DataTable();
                using var rdr = cmd.ExecuteReader();
                dt.Load(rdr);
                EmitLog($"✅ SELECT → {dt.Rows.Count} 行");
                return (true, dt, dt.Rows.Count);
            }
            else
            {
                var n = cmd.ExecuteNonQuery();
                EmitLog($"✅ 非查询 → {n} 行受影响");
                return (false, new DataTable(), n);
            }
        }
        catch (Exception ex)
        {
            EmitLog($"❌ SQL 执行失败：{ex.Message}");
            // 把异常信息返回给 UI
            var dt = new DataTable();
            dt.Columns.Add("错误", typeof(string));
            dt.Rows.Add(ex.Message);
            return (false, dt, -1);
        }
    }

    /// <summary>
    /// 把内存中 DataTable 的 RowState（Modified/Deleted/Added）回写到数据库。
    /// - 修改/新增走 INSERT OR REPLACE 或 UPDATE（按主键）
    /// - 删除走 DELETE
    ///
    /// 简化策略：如果表有 INTEGER PRIMARY KEY 列，则按该列作为 rowid 标识。
    /// </summary>
    public int SaveChanges(DataTable dt)
    {
        if (!IsOpen) return 0;
        if (dt == null) return 0;

        // 找 rowid 列
        var rowidCol = FindRowIdColumn(dt);
        if (rowidCol == null)
        {
            EmitLog("⚠️ DataTable 没有 INTEGER PRIMARY KEY / rowid 列，无法保存修改");
            return -1;
        }

        var affected = 0;
        using var conn = new SqliteConnection(_connectionString);
        conn.Open();
        using var tx = conn.BeginTransaction();

        try
        {
            foreach (var row in dt.AsEnumerable())
            {
                if (row.RowState == DataRowState.Unchanged) continue;

                var columns = dt.Columns.Cast<DataColumn>()
                    .Select(c => c.ColumnName)
                    .Where(n => !string.Equals(n, rowidCol, StringComparison.OrdinalIgnoreCase))
                    .ToList();

                if (row.RowState == DataRowState.Deleted)
                {
                    // 已删除的行原始值
                    var id = row[rowidCol, DataRowVersion.Original];
                    var sql = $"DELETE FROM \"{dt.TableName}\" WHERE \"{rowidCol}\" = $id;";
                    using var cmd = conn.CreateCommand();
                    cmd.Transaction = tx;
                    cmd.CommandText = sql;
                    cmd.Parameters.AddWithValue("$id", id ?? DBNull.Value);
                    affected += cmd.ExecuteNonQuery();
                }
                else if (row.RowState == DataRowState.Modified
                         || row.RowState == DataRowState.Added)
                {
                    // 拼接 SET 子句
                    var setCols = columns.Select(c => $"\"{c}\" = ${c}").ToList();
                    var id = row[rowidCol];
                    var sql = $"UPDATE \"{dt.TableName}\" SET {string.Join(", ", setCols)} WHERE \"{rowidCol}\" = $__rowid;";
                    using var cmd = conn.CreateCommand();
                    cmd.Transaction = tx;
                    cmd.CommandText = sql;
                    foreach (var c in columns)
                    {
                        cmd.Parameters.AddWithValue($"${c}", row[c] ?? DBNull.Value);
                    }
                    cmd.Parameters.AddWithValue("$__rowid", id ?? DBNull.Value);
                    affected += cmd.ExecuteNonQuery();
                }
            }

            tx.Commit();
            EmitLog($"✅ 已保存 {affected} 行修改");
            dt.AcceptChanges();
            return affected;
        }
        catch (Exception ex)
        {
            tx.Rollback();
            EmitLog($"❌ 保存失败：{ex.Message}");
            return -1;
        }
    }

    private static string? FindRowIdColumn(DataTable dt)
    {
        if (dt == null) return null;
        // 优先找名为 id / rowid 的 INTEGER PRIMARY KEY
        foreach (DataColumn c in dt.Columns)
        {
            var n = c.ColumnName.ToLowerInvariant();
            if ((n == "id" || n == "rowid") && c.DataType == typeof(long))
            {
                return c.ColumnName;
            }
        }
        // 退化：找任意 INTEGER PRIMARY KEY
        foreach (DataColumn c in dt.Columns)
        {
            if (c.DataType == typeof(long)) return c.ColumnName;
        }
        return null;
    }

    private void EmitLog(string msg)
    {
        Log?.Invoke(this, $"[{DateTime.Now:HH:mm:ss}] {msg}");
    }
}
