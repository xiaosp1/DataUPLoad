using Microsoft.Data.Sqlite;

namespace IntcoEdge.Db;

/// <summary>
/// SQLite 连接工厂：集中管理连接字符串、路径解析与连接生命周期。
///
/// 用途：
///   - W-A5 (DictionaryRepository / DefectQueryRepository) 通过此工厂获取连接，
///     避免每个 Repository 重复写连接串解析代码。
///   - W-A4 (LineRecordService / AlarmService) 也可以复用（按需）。
///
/// 路径解析规则：
///   - 相对路径相对于 <see cref="BaseDirectory"/>（EdgeHost 启动时的 CWD），
///     与 <c>IntcoEdge.Db</c> 自带 migration runner 行为一致。
///   - 绝对路径原样使用。
///
/// 线程模型：
///   - 每个调用 <see cref="Open"/> 都返回独立连接，使用方负责 using。
///   - SQLite 写串行；如需并发，请使用事务 + busy_timeout（已在连接串里设 5s）。
/// </summary>
public sealed class SqliteConnectionFactory
{
    private readonly string _connectionString;

    /// <summary>工厂基目录（用于解析相对路径）。默认 <see cref="AppContext.BaseDirectory"/>。</summary>
    public string BaseDirectory { get; }

    /// <summary>解析后的绝对路径（用于日志/诊断）。</summary>
    public string DbPath { get; }

    /// <summary>
    /// 构造工厂。
    /// </summary>
    /// <param name="dbPath">
    /// 数据库路径。null 或空时用 <c>Constants.DefaultDbPath</c>（"data/intco.db"）。
    /// 相对路径相对 <paramref name="baseDirectory"/> 解析；为 null 时用 <see cref="AppContext.BaseDirectory"/>。
    /// </param>
    /// <param name="baseDirectory">解析相对路径时的基目录（可选，默认 AppContext.BaseDirectory）。</param>
    public SqliteConnectionFactory(string? dbPath = null, string? baseDirectory = null)
    {
        BaseDirectory = string.IsNullOrEmpty(baseDirectory)
            ? AppContext.BaseDirectory
            : baseDirectory;

        var raw = string.IsNullOrWhiteSpace(dbPath) ? "data/intco.db" : dbPath;
        DbPath = Path.IsPathRooted(raw)
            ? raw
            : Path.GetFullPath(Path.Combine(BaseDirectory, raw));

        // 与 IntcoEdge.Db migration runner 行为对齐：
        //   - 读写创建模式
        //   - 共享缓存（多个 Repository 共享同一物理文件）
        //   - 外键关闭（按 PM 指令，应用层自行校验引用完整性）
        //   - busy_timeout 5s 兜底并发写入
        var csb = new SqliteConnectionStringBuilder
        {
            DataSource = DbPath,
            Mode = SqliteOpenMode.ReadWriteCreate,
            // Cache = SqliteCacheMode.Shared,  // PM 17:50 去掉 shared cache（status_record 写时报 readonly）
            Pooling = true,
            ForeignKeys = false,
        };
        _connectionString = csb.ToString();
    }

    /// <summary>打开一个新连接。调用方负责 using 释放。</summary>
    public SqliteConnection Open()
    {
        var conn = new SqliteConnection(_connectionString);
        conn.Open();
        // SQLite 客户端级别的 busy timeout（即使连接池已设 busy_timeout=5000，这里再设一次保险）。
        using (var pragma = conn.CreateCommand())
        {
            pragma.CommandText = "PRAGMA busy_timeout = 5000;";
            pragma.ExecuteNonQuery();
        }
        return conn;
    }

    /// <summary>
    /// 同步打开只读连接（用于查询 API 想要的 read-only 行为，避免误写）。
    /// 文件不存在时抛 <see cref="InvalidOperationException"/>，调用方需捕获并返回 200 + 空集。
    /// </summary>
    public SqliteConnection OpenReadOnly()
    {
        if (!File.Exists(DbPath))
        {
            throw new InvalidOperationException($"SQLite DB 不存在：{DbPath}");
        }
        var csb = new SqliteConnectionStringBuilder
        {
            DataSource = DbPath,
            Mode = SqliteOpenMode.ReadOnly,
            // Cache = SqliteCacheMode.Shared,  // PM 17:50 去掉 shared cache（status_record 写时报 readonly）
            ForeignKeys = false,
        };
        var conn = new SqliteConnection(csb.ToString());
        conn.Open();
        return conn;
    }
}
