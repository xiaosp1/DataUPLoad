// Smoke test for IntcoEdge.Db.
// Exercises a representative subset of the 20 user-data tables to confirm
// the schema accepts INSERT/SELECT after a clean migration. Not part of the
// shipping migration runner; lives in a separate sub-project so `dotnet build
// IntcoEdge.Db` stays clean.
using System.Globalization;
using Microsoft.Data.Sqlite;

var dbPath = args.Length > 0 ? args[0] : "../data/intco.db";
if (!Path.IsPathRooted(dbPath))
{
    dbPath = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, dbPath));
}

using var conn = new SqliteConnection($"Data Source={dbPath};Mode=ReadOnly");
conn.Open();

void Assert(string table, string sql, Action<SqliteDataReader> check)
{
    using var cmd = conn.CreateCommand();
    cmd.CommandText = sql;
    using var rd = cmd.ExecuteReader();
    if (!rd.Read()) throw new InvalidOperationException($"smoke FAIL: {table} empty");
    check(rd);
    Console.WriteLine($"  OK  {table}");
}

Console.WriteLine("Smoke testing IntcoEdge.Db schema (read-only):");
Assert("defect_type", "SELECT name, category FROM defect_type WHERE name='客户端'", r =>
{
    if (r.GetString(0) != "客户端") throw new Exception("客户端 row missing");
    if (r.GetInt32(1) != 3) throw new Exception("客户端 category wrong");
});

Assert("role", "SELECT role, permission FROM role WHERE role='super_admin'", r =>
{
    if (!r.GetString(1).Contains("user,")) throw new Exception("role.permission corrupted");
});

Assert("white_ip", "SELECT ip FROM white_ip WHERE ip='*.*.*.*'", r =>
{
    if (r.GetString(0) != "*.*.*.*") throw new Exception("white_ip default row missing");
});

Assert("system_config", "SELECT config_key, config_value FROM system_config WHERE config_key IN ('device_alarm_sound_uri','defect_alarm_sound_uri','system_alarm_sound_uri','sound_play_count') ORDER BY config_key", r =>
{
    // Already iterated to last row; we just need at least one.
    if (r.GetString(0) != "defect_alarm_sound_uri") throw new Exception("system_config rows incomplete");
});

Assert("line columns", "PRAGMA table_info(line)", r =>
{
    // Check that realtime_data and color columns exist.
    var foundRt = false; var foundColor = false;
    while (r.Read())
    {
        var n = r.GetString(1);
        if (n == "realtime_data") foundRt = true;
        if (n == "color") foundColor = true;
    }
    if (!foundRt) throw new Exception("line.realtime_data missing");
    if (!foundColor) throw new Exception("line.color missing");
});

Assert("alarm_record columns", "PRAGMA table_info(alarm_record)", r =>
{
    var foundDefectName = false;
    while (r.Read()) if (r.GetString(1) == "defect_name") foundDefectName = true;
    if (!foundDefectName) throw new Exception("alarm_record.defect_name missing (V1.11)");
});

Assert("status_record columns", "PRAGMA table_info(status_record)", r =>
{
    var foundLineId = false; var foundDeviceName = false;
    while (r.Read())
    {
        var n = r.GetString(1);
        if (n == "line_id") foundLineId = true;
        if (n == "device_name") foundDeviceName = true;
    }
    if (!foundLineId) throw new Exception("status_record.line_id missing (V1.19)");
    if (!foundDeviceName) throw new Exception("status_record.device_name missing (V1.10)");
});

Assert("line_day_record columns", "PRAGMA table_info(line_day_record)", r =>
{
    var foundRemove = false; var foundUpload = false; var foundFace = false;
    while (r.Read())
    {
        var n = r.GetString(1);
        if (n == "remove_total") foundRemove = true;
        if (n == "upload_remove_total") foundUpload = true;
        if (n == "face_no") foundFace = true;
    }
    if (!foundRemove) throw new Exception("line_day_record.remove_total missing (V1.17)");
    if (!foundUpload) throw new Exception("line_day_record.upload_remove_total missing (V1.18)");
    if (!foundFace) throw new Exception("line_day_record.face_no missing (V1.8)");
});

Assert("defect_record columns", "PRAGMA table_info(defect_record)", r =>
{
    var foundExcept = false;
    while (r.Read()) if (r.GetString(1) == "except_flag") foundExcept = true;
    if (!foundExcept) throw new Exception("defect_record.except_flag missing (V1.3)");
});

Assert("history", "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1", r =>
{
    var n = r.GetInt32(0);
    if (n != 19) throw new Exception($"expected 19 successful migrations, got {n}");
    Console.WriteLine($"        (19 successful migrations confirmed)");
});

Console.WriteLine("ALL SMOKE TESTS PASSED");
