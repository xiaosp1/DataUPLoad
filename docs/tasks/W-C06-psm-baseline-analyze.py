#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
W-C06: Refined analysis — count pushes per hour, ignore ANSI codes.
"""

import os
import re
import json
from collections import defaultdict

LOG_ROOT = r"E:\DEMO\数据采集\docs\domain\海康大屏逆向\10-反编译产物-NEW\PSM\server\log\intco-screen"

# Strip ANSI escape codes (e.g., [7m...[0m)
ANSI = re.compile(r'\x1b\[[0-9;]*[mK]|\[[0-9]+m|\[[0-9]+;[0-9]+m')

# Patterns for push events (after stripping ANSI)
# log lines look like:
#   2026-07-17 03:14:34.165 - ERROR intco-screen [...] [YKServiceImpl.pushAlarm2YK:182] push alarm to yk error,ticket is null....
#   2026-07-04 00:09:02.861 - WARN  intco-screen [...] [YKServiceImpl.pushAlarm2YK:146] success receive alarm event....
#   2026-07-17 00:00:00.295 - WARN  intco-screen [...] [AlarmRecordServiceImpl.add:254] current alarm is not interesting defect...
#   2026-07-17 03:26:13.750 - ERROR intco-screen [...] [AlarmRecordServiceImpl.log:121] deal alram failed...

# Note: PSM may wrap long lines by inserting literal \n in middle of text.  We must join continuation lines first.
# Looking at samples: each "wrapped" continuation line starts with a date+time prefix, BUT the content has no prefix
# (it starts with the literal text wrapped). Actually looking at samples, the wrapper is `[7m...[0m` and the actual
# content is on the next line.  Looking again, the format appears to be:
#   "2026-07-17 03:26:13.750 -ERROR intco-screen [http-nio-80-exec-21] [AlarmRecordServiceImpl.log:121] [7mdeal alram failed[0m,alarm uuid. [par[0m\n[7m[0mam=509fc55d-7e27-4750-8207-a98051b2bb88][code=20102][0m"
# i.e., lines wrap visually but the time prefix is only at the start.  Lines are joined by "\n" then we see the [7m tags.
# So: simple line-by-line parsing with ANSI stripping should work — the time prefix marks the start.

RX_LINE = re.compile(r'^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d+)\s*-\s*(ERROR|WARN|INFO)\s+intco-screen\s+\[([^\]]+)\]\s+\[([^\]]+)\](.*)$')

def classify(stripped_line):
    """Returns (category, hour_str 'YYYY-MM-DD HH') or None."""
    m = RX_LINE.match(stripped_line)
    if not m:
        return None
    ts, level, thread, logger_method, rest = m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)
    hour = ts[:13]  # YYYY-MM-DD HH
    if logger_method.startswith("YKServiceImpl.pushAlarm2YK:"):
        if "success receive alarm event" in rest:
            return ("push_success", hour, ts)
        if "push alarm to yk error" in rest:
            return ("push_err_ticket", hour, ts)
        if "push alarm info to yk failed" in rest:
            return ("push_err_resp", hour, ts)
    if logger_method.startswith("AlarmRecordServiceImpl.add:") and "current alarm is not interesting defect" in rest:
        return ("alarm_not_interesting", hour, ts)
    if logger_method.startswith("AlarmRecordServiceImpl.log:") and "deal alram failed" in rest:
        return ("deal_failed", hour, ts)
    if logger_method.startswith("YKServiceImpl.updateTicket:"):
        if level == "INFO" and "success to get ticket" in rest:
            return ("ticket_success", hour, ts)
        if level == "ERROR" and "get ticket from yk system failed" in rest:
            return ("ticket_failed", hour, ts)
    return None

def list_log_files():
    backups = os.path.join(LOG_ROOT, "backup")
    found = []
    if os.path.isdir(backups):
        for d in sorted(os.listdir(backups)):
            full_d = os.path.join(backups, d)
            if not os.path.isdir(full_d):
                continue
            for f in sorted(os.listdir(full_d)):
                low = f.lower()
                if low.endswith(".log") and (low.startswith("error.") or low.startswith("warn.") or low.startswith("info.")):
                    found.append((d, os.path.join(full_d, f)))
    for f in sorted(os.listdir(LOG_ROOT)):
        low = f.lower()
        if low.endswith(".log") and (low.startswith("error.") or low.startswith("warn.") or low.startswith("info.")):
            found.append(("CURRENT", os.path.join(LOG_ROOT, f)))
    return found

def main():
    files = list_log_files()
    totals = defaultdict(int)
    hour_buckets = defaultdict(lambda: defaultdict(int))
    day_totals = defaultdict(lambda: defaultdict(int))
    per_file = []
    bytes_processed = 0
    for d, path in files:
        local = defaultdict(int)
        size = 0
        # Note: PSM writes log with ANSI; lines starting with timestamp may contain wrapped continuation in next line
        # but the timestamp prefix is unique to the start line. Read line by line and strip ANSI per line.
        try:
            with open(path, "r", encoding="utf-8", errors="replace") as fh:
                for line in fh:
                    size += len(line.encode("utf-8", errors="replace"))
                    # Strip ANSI on per-line basis
                    stripped = ANSI.sub('', line)
                    r = classify(stripped)
                    if r:
                        cat, hour, ts = r
                        totals[cat] += 1
                        local[cat] += 1
                        hour_buckets[(d, hour)][cat] += 1
                        day_totals[d][cat] += 1
            bytes_processed += size
            per_file.append({"day": d, "file": os.path.basename(path), "size_bytes": size, **dict(local)})
        except Exception as e:
            per_file.append({"day": d, "file": os.path.basename(path), "error": str(e)})
    
    push_total = totals["push_success"] + totals["push_err_ticket"] + totals["push_err_resp"]
    
    # Compute hour-bucket stats using push_success + push_err_ticket + push_err_resp
    push_hour_totals = []
    for (d, hour), cats in hour_buckets.items():
        pt = cats.get("push_success", 0) + cats.get("push_err_ticket", 0) + cats.get("push_err_resp", 0)
        if pt > 0:
            push_hour_totals.append({"day": d, "hour": hour, "total_pushes": pt,
                                      "success": cats.get("push_success", 0),
                                      "err_ticket": cats.get("push_err_ticket", 0),
                                      "err_resp": cats.get("push_err_resp", 0)})
    
    push_hour_totals.sort(key=lambda x: (x["day"], x["hour"]))
    
    # Stats over push_hour_totals
    if push_hour_totals:
        total_hours_with_push = len(push_hour_totals)
        max_h = max(x["total_pushes"] for x in push_hour_totals)
        min_h = min(x["total_pushes"] for x in push_hour_totals)
        avg_h = push_total / total_hours_with_push
        # Per-day avg
        from collections import defaultdict as dd
        per_day_sum = dd(int)
        per_day_count = dd(int)
        for x in push_hour_totals:
            per_day_sum[x["day"]] += x["total_pushes"]
            per_day_count[x["day"]] += 1
        per_day_avg = {d: round(per_day_sum[d]/per_day_count[d], 2) for d in sorted(per_day_sum)}
        # Quantiles
        sorted_pushes = sorted(x["total_pushes"] for x in push_hour_totals)
        n = len(sorted_pushes)
        p50 = sorted_pushes[n//2]
        p90 = sorted_pushes[int(n*0.9)]
        p99 = sorted_pushes[int(n*0.99)] if n > 100 else sorted_pushes[-1]
    else:
        total_hours_with_push = 0
        max_h = min_h = avg_h = 0
        per_day_avg = {}
        p50 = p90 = p99 = 0
    
    out = {
        "log_root": LOG_ROOT,
        "files_scanned": len(per_file),
        "bytes_processed_GB": round(bytes_processed/1024/1024/1024, 3),
        "totals": dict(totals),
        "push_total": push_total,
        "total_hours_with_push_activity": total_hours_with_push,
        "min_pushes_in_active_hour": min_h,
        "max_pushes_in_active_hour": max_h,
        "median_p50": p50,
        "p90": p90,
        "p99": p99,
        "avg_push_per_active_hour": round(avg_h, 2),
        "avg_per_day": per_day_avg,
        "day_totals": {d: dict(v) for d, v in day_totals.items()},
        "per_file_count": per_file,
        "push_hour_breakdown_sample_first_30": push_hour_totals[:30],
        "push_hour_breakdown_sample_last_30": push_hour_totals[-30:],
        "push_hour_breakdown_total_records": len(push_hour_totals),
    }
    print(json.dumps(out, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
