# IntcoEdge.Db

SQLite + Flyway-compatible migration runner for the new EdgeHost service.

This project replaces the in-memory file system + manual SQL of the old EdgeHost with a
proper versioned migration pipeline. The schema is a 1:1 translation of PSM's PostgreSQL
schema (V1.0–V1.19) into SQLite-compatible SQL.

## Layout

```
src/IntcoEdge.Db/
├── IntcoEdge.Db.csproj          # Migration runner console app
├── Program.cs                   # Embedded Flyway-equivalent (Microsoft.Data.Sqlite)
├── migrations/                  # 19 versioned scripts (V1.0 → V1.19)
│   ├── V1.0__init.sql           # defect_record, alarm_record, line, plan, role, user, ...
│   ├── ...
│   └── V1.19__state_change.sql  # state_change + state_statistic + status_record.line_id
├── SmokeTest/                   # DoD verification sub-project (excluded from main build)
│   ├── SmokeTest.csproj
│   └── Program.cs               # Read-only assertions on key tables + columns
└── data/                        # ← runtime DB (gitignored, never committed)
    └── intco.db
```

## Migration runner

`Program.cs` is a 350-line Flyway-equivalent implementation. It is **not** the Flyway CLI;
we wrote it ourselves because:

1. The Flyway CLI is a Java JAR, and bringing a JRE on the production box is overkill.
2. For 19 fixed-versioned scripts, the contract is small enough to be re-implemented locally.
3. It produces the same `flyway_schema_history` table layout as Flyway 9.x, so any tool that
   reads the history table sees a familiar schema.

Supported commands (default = `migrate`):

| Command   | What it does                                                                 |
|-----------|------------------------------------------------------------------------------|
| `migrate` | Apply pending scripts in version order, recording each in `flyway_schema_history`. |
| `info`    | Print the migration history (rank / version / description / success).         |
| `validate`| Re-check SHA-1 checksums of applied scripts against files on disk.            |
| `clean`   | Drop every table the runner created. Requires `INTCO_DB_ALLOW_CLEAN=1`.       |
| `schema`  | Print `.schema` (CREATE TABLE / CREATE INDEX for everything).                 |
| `tables`  | Print `.tables` (sorted whitespace-separated table list).                     |

The script version is the substring between `V` and `__` in the filename. Versions are sorted
**numerically segment-by-segment** (so V1.2 < V1.10 < V1.13 < V1.14), not lexicographically.
SHA-1 checksum is computed per script and stored as a signed int32 (Flyway's wire format).

## Schema decisions worth knowing

| Topic              | Decision                                                                                          |
|--------------------|---------------------------------------------------------------------------------------------------|
| Foreign keys       | **Off** (`PRAGMA foreign_keys = OFF`). Application layer validates FKs. PM directive.            |
| `serial`           | `INTEGER PRIMARY KEY AUTOINCREMENT`.                                                              |
| `varchar(N)`       | `TEXT` (SQLite does not enforce length; we keep `(N)` in comments for documentation).           |
| `bool`             | `INTEGER` (0/1).                                                                                  |
| `timestamp`        | `TEXT` (ISO 8601). `CURRENT_TIMESTAMP` is supported by SQLite and returns UTC text.              |
| `COMMENT ON ...`   | Dropped. SQLite has no comment syntax; column purposes live in our SQL comments and in DTOs.    |
| `CREATE TRIGGER`   | Dropped. `update_time` is set by application code, not by DB triggers.                           |
| `public.` schema   | Dropped. SQLite has no schema namespace.                                                          |
| `USING btree`      | Dropped. SQLite uses btree by default.                                                            |
| `truncate table`   | `DELETE FROM` (SQLite has no TRUNCATE).                                                           |

## Pre-Flyway PSM tables

PSM's role / user / white_ip tables have **no DDL in the V* scripts** (the schema doc notes
"Flyway 之前的脚本建的，SQL 没看到"). To preserve the 19-script contract, we reconstruct their
DDL inside **V1.0**'s "pre-Flyway" section, using field hints from V1.1 (`role.permission`)
and V1.7 (`white_ip.ip, create_time, update_time`). All inferred fields are flagged in the
file header comment.

## Verifying the DoD

```pwsh
cd src/IntcoEdge.Db
dotnet build                                                  # 0 errors
dotnet run --no-build migrate                                 # apply all 19
dotnet run --no-build tables                                  # .tables
dotnet run --no-build info                                    # Flyway history
dotnet run --no-build validate                                # SHA-1 checksum verify
dotnet run --no-build schema                                  # .schema dump

# SmokeTest (separate project, excluded from main build)
cd SmokeTest
dotnet run --no-build -- ..\data\intco.db
```

## Known caveats

1. **Schema doc says 22 tables, we ship 20 user-data tables.** The 22-table headline in
   `06-sql-schema.md` is inconsistent with its own tables — 17 核心业务 + 3 权限/系统 = 20.
   The 2 missing tables are either from pre-Flyway scripts we don't have DDL for, or from
   PSM 2.1.9+ additions (which the schema doc flags under "已知问题"). Confirm against
   `psql \dt` if you need to find them.
2. **No triggers.** `update_time` must be set by application code on every UPDATE.
   Application-layer convention: `UPDATE x SET ..., update_time = CURRENT_TIMESTAMP WHERE ...`.
3. **`role` / `user` field names use quoted identifiers.** `role` is a SQL keyword; we keep it
   quoted to match PSM semantics. C# DTOs should use `[Column("role")]` accordingly.
