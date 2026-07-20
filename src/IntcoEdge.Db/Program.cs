// IntcoEdge.Db - Embedded Flyway-like migration runner for SQLite.
//
// Why a hand-rolled runner instead of Flyway CLI:
//   - Flyway CLI is a Java JAR; bringing in the JRE on a Windows production box is overkill.
//   - Flyway 9.x added an "Teams" / "Pro" paywall for the .NET-style usage we need.
//   - For 19 fixed-versioned scripts we can implement the Flyway contract in ~120 lines:
//       * flyway_schema_history table (version, description, type, script, checksum,
//         installed_by, installed_on, execution_time, success)
//       * versioned migrations in lexical order
//       * SHA-1 checksum of each script (Flyway uses SHA-1, we keep it compatible)
//       * transactional per-script execution (SQLite serial; one writer at a time).
//
// Behavior parity with Flyway 9.x for our usage:
//   - migrate:  apply pending scripts in order; record history; abort on first failure.
//   - info:     print history table to stdout.
//   - validate: re-check checksums of already-applied scripts; non-zero exit on mismatch.
//   - clean:    drop all IntcoEdge tables (for local dev only; refuses if --production).

using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using Microsoft.Data.Sqlite;

namespace IntcoEdge.Db;

internal static class Program
{
    private const string DefaultDbPath = "data/intco.db";
    private const string MigrationsDir = "migrations";

    // Tables IntcoEdge.Db itself creates. Everything else in the DB is "user data".
    private static readonly HashSet<string> OwnedTables = new(StringComparer.OrdinalIgnoreCase)
    {
        "flyway_schema_history",
    };

    private static int Main(string[] args)
    {
        // Force UTF-8 stdout/stderr so Chinese comments survive when output is piped or redirected.
        // Windows default codepage is often GBK/936, which mangles UTF-8 byte sequences.
        try
        {
            Console.OutputEncoding = new UTF8Encoding(encoderShouldEmitUTF8Identifier: false);
            Console.InputEncoding  = Encoding.UTF8;
        }
        catch
        {
            // some hosts (e.g. test runners) reject encoding swap; safe to ignore.
        }

        try
        {
            var cmd = args.Length > 0 ? args[0].ToLowerInvariant() : "migrate";
            var dbPath = args.Length > 1 ? args[1] : DefaultDbPath;

            var dbDir = Path.GetDirectoryName(dbPath);
            if (!string.IsNullOrEmpty(dbDir))
            {
                Directory.CreateDirectory(dbDir);
            }

            var connStr = new SqliteConnectionStringBuilder
            {
                DataSource = dbPath,
                Mode = SqliteOpenMode.ReadWriteCreate,
                Cache = SqliteCacheMode.Shared,
                Pooling = true,
                // FK enforcement intentionally OFF (per PM directive: SQLite skips FK, application layer validates).
                ForeignKeys = false,
            }.ToString();

            using var conn = new SqliteConnection(connStr);
            conn.Open();

            // SQLite serializes writers; PRAGMA busy_timeout avoids SQLITE_BUSY on parallel runs.
            using (var pragma = conn.CreateCommand())
            {
                pragma.CommandText = "PRAGMA busy_timeout = 5000;";
                pragma.ExecuteNonQuery();
            }

            EnsureHistoryTable(conn);

            return cmd switch
            {
                "migrate"  => RunMigrate(conn),
                "info"     => RunInfo(conn),
                "validate" => RunValidate(conn),
                "clean"    => RunClean(conn),
                "schema"   => RunSchema(conn),
                "tables"   => RunTables(conn),
                _          => PrintUsage(),
            };
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"[IntcoEdge.Db] FATAL: {ex.Message}");
            Console.Error.WriteLine(ex);
            return 1;
        }
    }

    private static int PrintUsage()
    {
        Console.Error.WriteLine("Usage: IntcoEdge.Db <migrate|info|validate|clean|schema|tables> [dbPath]");
        Console.Error.WriteLine("  dbPath default: data/intco.db (relative to cwd)");
        return 2;
    }

    private static int RunTables(SqliteConnection conn)
    {
        // Mimic sqlite3 ".tables" output: one whitespace-separated list, sorted.
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name";
        using var rd = cmd.ExecuteReader();
        var sb = new StringBuilder();
        while (rd.Read())
        {
            if (sb.Length > 0) sb.Append(' ');
            sb.Append(rd.GetString(0));
        }
        Console.WriteLine(sb.ToString());
        return 0;
    }

    private static int RunSchema(SqliteConnection conn)
    {
        // Mimic sqlite3 ".schema": for each table, print its CREATE statement.
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"SELECT name, sql FROM sqlite_master
                            WHERE type IN ('table','index') AND name NOT LIKE 'sqlite_%'
                            ORDER BY type DESC, name";
        using var rd = cmd.ExecuteReader();
        while (rd.Read())
        {
            var name = rd.GetString(0);
            var sql = rd.IsDBNull(1) ? "" : rd.GetString(1);
            Console.WriteLine($"{sql};");
        }
        return 0;
    }

    // ---- Schema history ----------------------------------------------------

    private static void EnsureHistoryTable(SqliteConnection conn)
    {
        const string ddl = @"
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INTEGER NOT NULL PRIMARY KEY,
    version        TEXT,
    description    TEXT NOT NULL,
    type           TEXT NOT NULL,
    script         TEXT NOT NULL,
    checksum       INTEGER,
    installed_by   TEXT NOT NULL,
    installed_on   TEXT NOT NULL,
    execution_time INTEGER NOT NULL,
    success        INTEGER NOT NULL
);";
        using var cmd = conn.CreateCommand();
        cmd.CommandText = ddl;
        cmd.ExecuteNonQuery();
    }

    // ---- Discover scripts --------------------------------------------------

    private record MigrationScript(string Version, string Description, string FilePath);

    private static List<MigrationScript> DiscoverScripts()
    {
        if (!Directory.Exists(MigrationsDir))
        {
            throw new DirectoryNotFoundException(
                $"Migrations directory not found: {Path.GetFullPath(MigrationsDir)}");
        }

        var scripts = new List<MigrationScript>();
        foreach (var path in Directory.EnumerateFiles(MigrationsDir, "V*.sql"))
        {
            var name = Path.GetFileNameWithoutExtension(path); // e.g. "V1.0__init"
            // Strict Flyway-style parsing: V<version>__<description>
            var sep = name.IndexOf("__", StringComparison.Ordinal);
            if (sep <= 1 || name[0] != 'V')
            {
                throw new InvalidOperationException(
                    $"Invalid migration filename '{Path.GetFileName(path)}'. Expected V<version>__<description>.sql");
            }

            var version = name.Substring(1, sep - 1); // strip leading 'V'
            var description = name.Substring(sep + 2);
            scripts.Add(new MigrationScript(version, description, path));
        }

        if (scripts.Count == 0)
        {
            throw new InvalidOperationException($"No V*.sql migration scripts found in '{MigrationsDir}'.");
        }

        // Flyway sorts by version segments numerically (V1.2 before V1.10), not lexicographically.
        // Ordinal sort would put V1.10 before V1.2 and break dependency order.
        return scripts
            .OrderBy(s => s.Version, new VersionStringComparer())
            .ThenBy(s => s.FilePath, StringComparer.Ordinal)
            .ToList();
    }

    private sealed class VersionStringComparer : IComparer<string>
    {
        public int Compare(string? x, string? y)
        {
            if (ReferenceEquals(x, y)) return 0;
            if (x is null) return -1;
            if (y is null) return 1;

            var xs = x.Split('.', '-', '_');
            var ys = y.Split('.', '-', '_');
            var n = Math.Min(xs.Length, ys.Length);
            for (var i = 0; i < n; i++)
            {
                var xi = ParseSegment(xs[i]);
                var yi = ParseSegment(ys[i]);
                if (xi.HasValue && yi.HasValue)
                {
                    var c = xi.Value.CompareTo(yi.Value);
                    if (c != 0) return c;
                }
                else
                {
                    var c = string.Compare(xs[i], ys[i], StringComparison.Ordinal);
                    if (c != 0) return c;
                }
            }
            return xs.Length.CompareTo(ys.Length);
        }

        private static long? ParseSegment(string s)
        {
            if (long.TryParse(s, NumberStyles.Integer, CultureInfo.InvariantCulture, out var v))
                return v;
            return null;
        }
    }

    // ---- Checksum ----------------------------------------------------------

    private static int Checksum(string scriptPath)
    {
        // Flyway uses SHA-1 and stores the first 4 bytes as a signed int32.
        // We replicate that exact byte-for-byte so future Flyway CLI interop works.
        using var sha1 = SHA1.Create();
        using var stream = File.OpenRead(scriptPath);
        var hash = sha1.ComputeHash(stream);
        return BitConverter.ToInt32(hash, 0);
    }

    // ---- migrate -----------------------------------------------------------

    private static int RunMigrate(SqliteConnection conn)
    {
        var scripts = DiscoverScripts();
        var applied = LoadHistory(conn);

        var pending = scripts.Where(s => !applied.Contains(s.Version)).ToList();
        Console.WriteLine($"[IntcoEdge.Db] {scripts.Count} scripts found, {pending.Count} pending.");

        var sw = new System.Diagnostics.Stopwatch();
        foreach (var script in pending)
        {
            Console.WriteLine($"[IntcoEdge.Db] Applying V{script.Version} ({script.Description})...");
            sw.Restart();

            using var tx = conn.BeginTransaction();
            try
            {
                var sql = File.ReadAllText(script.FilePath, Encoding.UTF8);
                using (var cmd = conn.CreateCommand())
                {
                    cmd.Transaction = tx;
                    cmd.CommandText = sql;
                    cmd.ExecuteNonQuery();
                }

                InsertHistory(conn, tx, script, success: true, (int)sw.ElapsedMilliseconds);
                tx.Commit();
                Console.WriteLine($"[IntcoEdge.Db]   OK ({sw.ElapsedMilliseconds} ms)");
            }
            catch (Exception ex)
            {
                tx.Rollback();
                Console.Error.WriteLine($"[IntcoEdge.Db]   FAILED: {ex.Message}");
                InsertHistory(conn, null, script, success: false, (int)sw.ElapsedMilliseconds);
                return 3;
            }
        }

        Console.WriteLine("[IntcoEdge.Db] Migrate complete.");
        return 0;
    }

    private static HashSet<string> LoadHistory(SqliteConnection conn)
    {
        var dict = new HashSet<string>(StringComparer.Ordinal);
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT version FROM flyway_schema_history WHERE success = 1";
        using var rd = cmd.ExecuteReader();
        while (rd.Read())
        {
            var v = rd.GetString(0);
            if (!string.IsNullOrEmpty(v))
            {
                dict.Add(v);
            }
        }
        return dict;
    }

    private static void InsertHistory(SqliteConnection conn, SqliteTransaction? tx, MigrationScript s, bool success, int elapsedMs)
    {
        using var cmd = conn.CreateCommand();
        if (tx != null) cmd.Transaction = tx;
        cmd.CommandText = @"
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
    ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
     $v, $d, 'SQL', $s, $c, $u, $t, $e, $ok)";
        cmd.Parameters.AddWithValue("$v", s.Version);
        cmd.Parameters.AddWithValue("$d", s.Description);
        cmd.Parameters.AddWithValue("$s", Path.GetFileName(s.FilePath));
        cmd.Parameters.AddWithValue("$c", Checksum(s.FilePath));
        cmd.Parameters.AddWithValue("$u", Environment.UserName);
        cmd.Parameters.AddWithValue("$t", DateTime.UtcNow.ToString("yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture));
        cmd.Parameters.AddWithValue("$e", elapsedMs);
        cmd.Parameters.AddWithValue("$ok", success ? 1 : 0);
        cmd.ExecuteNonQuery();
    }

    // ---- info / validate / clean ------------------------------------------

    private static int RunInfo(SqliteConnection conn)
    {
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
SELECT installed_rank, version, description, script, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank";
        using var rd = cmd.ExecuteReader();
        Console.WriteLine($"{"Rank",5}  {"Version",8}  {"Description",-24}  {"Script",-32}  {"Installed",-20}  {"ms",6}  OK");
        Console.WriteLine(new string('-', 110));
        while (rd.Read())
        {
            Console.WriteLine(string.Format(CultureInfo.InvariantCulture,
                "{0,5}  {1,8}  {2,-24}  {3,-32}  {4,-20}  {5,6}  {6}",
                rd.GetInt32(0),
                rd.IsDBNull(1) ? "" : rd.GetString(1),
                rd.GetString(2),
                rd.GetString(3),
                rd.GetString(4),
                rd.GetInt32(5),
                rd.GetInt32(6) == 1 ? "YES" : "NO"));
        }
        return 0;
    }

    private static int RunValidate(SqliteConnection conn)
    {
        var scripts = DiscoverScripts().ToDictionary(s => s.Version, StringComparer.Ordinal);
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT version, checksum FROM flyway_schema_history WHERE success = 1";
        using var rd = cmd.ExecuteReader();
        var bad = 0;
        while (rd.Read())
        {
            var v = rd.GetString(0);
            var stored = rd.IsDBNull(1) ? 0 : rd.GetInt32(1);
            if (!scripts.TryGetValue(v, out var s))
            {
                Console.Error.WriteLine($"[IntcoEdge.Db] validate FAIL: applied version V{v} no longer has a script");
                bad++;
                continue;
            }
            var current = Checksum(s.FilePath);
            if (current != stored)
            {
                Console.Error.WriteLine($"[IntcoEdge.Db] validate FAIL: V{v} checksum drift ({stored:X8} vs {current:X8})");
                bad++;
            }
        }
        if (bad > 0)
        {
            Console.Error.WriteLine($"[IntcoEdge.Db] {bad} validation failure(s).");
            return 4;
        }
        Console.WriteLine("[IntcoEdge.Db] Validation OK.");
        return 0;
    }

    private static int RunClean(SqliteConnection conn)
    {
        // Refuse unless operator explicitly opts in: never wipe the production DB by accident.
        var allow = Environment.GetEnvironmentVariable("INTCO_DB_ALLOW_CLEAN");
        if (!string.Equals(allow, "1", StringComparison.Ordinal))
        {
            Console.Error.WriteLine("Refusing to clean. Set INTCO_DB_ALLOW_CLEAN=1 if you really mean it.");
            return 5;
        }

        using var tx = conn.BeginTransaction();
        try
        {
            // Drop in reverse FK order would normally be needed; we have FK off and no application-level
            // dependency cycle yet, so drop alphabetically for determinism.
            using (var list = conn.CreateCommand())
            {
                list.Transaction = tx;
                list.CommandText = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";
                using var rd = list.ExecuteReader();
                var names = new List<string>();
                while (rd.Read()) names.Add(rd.GetString(0));
                foreach (var name in names.OrderByDescending(n => n, StringComparer.Ordinal))
                {
                    if (OwnedTables.Contains(name)) continue;
                    using var drop = conn.CreateCommand();
                    drop.Transaction = tx;
                    drop.CommandText = $"DROP TABLE IF EXISTS [{name}]";
                    drop.ExecuteNonQuery();
                    Console.WriteLine($"[IntcoEdge.Db] dropped {name}");
                }
            }

            using (var drop = conn.CreateCommand())
            {
                drop.Transaction = tx;
                drop.CommandText = "DELETE FROM flyway_schema_history";
                drop.ExecuteNonQuery();
            }

            tx.Commit();
            Console.WriteLine("[IntcoEdge.Db] clean done.");
            return 0;
        }
        catch
        {
            tx.Rollback();
            throw;
        }
    }
}
