import re
p = r'E:\DEMO\数据采集\docs\delivered\INDEX.md'
with open(p, encoding='utf-8') as f:
    lines = f.readlines()

print('=== yk-graybox-monitor.md references (line numbers + content) ===')
for i, l in enumerate(lines, 1):
    if 'yk-graybox' in l or '2026-07-23-next-plan' in l or '2026-07-23-W-X11c' in l:
        print(f'{i:4d}: {l.rstrip()}')

print()
print('=== Section 9 marker ===')
for i, l in enumerate(lines, 1):
    if '9.' in l and ('今晚' in l or 'DataupLoad' in l):
        print(f'{i:4d}: {l.rstrip()}')
        break
