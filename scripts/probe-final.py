import os

deliverables = {
    'SOP main (Step 1)': r'E:\DEMO\数据采集\docs\SOP\yk-graybox-monitor.md',
    'STATUS.md (Step 2.1)': r'E:\DEMO\数据采集\STATUS.md',
    'INDEX.md (Step 2.2)': r'E:\DEMO\数据采集\docs\delivered\INDEX.md',
    'TODO.md (Step 2.3)': r'E:\DEMO\数据采集\TODO.md',
    'Result report (Step 3)': r'E:\DEMO\数据采集\docs\delivered\2026-07-23-W-X14-result.md',
}

print('=== Final deliverables ===')
for name, p in deliverables.items():
    if os.path.exists(p):
        size = os.path.getsize(p)
        with open(p, encoding='utf-8') as f:
            content = f.read()
        lines = content.count('\n')
        words = sum(len(l.split()) for l in content.splitlines())
        print(f'  [OK] {name}')
        print(f'        {p}')
        print(f'        size={size} bytes, lines={lines}, words={words}')
        print()
    else:
        print(f'  [MISSING] {name}: {p}')
        print()
