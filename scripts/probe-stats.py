import os
sop = r'E:\DEMO\数据采集\docs\SOP\yk-graybox-monitor.md'
with open(sop, encoding='utf-8') as f:
    content = f.read()
    lines = content.splitlines()

# Per-section statistics
sections = []
current = None
for i, l in enumerate(lines, 1):
    if l.startswith('## '):
        if current:
            sections.append(current)
        current = {'line': i, 'title': l.strip(), 'lines': 0, 'words': 0}
    if current:
        current['lines'] += 1
        current['words'] += len(l.split())
if current:
    sections.append(current)

print('=== Per-section stats ===')
for s in sections:
    print(f"  L{s['line']:>4}  {s['title']}  ({s['lines']} lines, {s['words']} words)")

print()
print('=== Iron rule sections only (5 rules) ===')
iron_rules = [s for s in sections if '铁则' in s['title']]
for s in iron_rules:
    print(f"  L{s['line']:>4}  {s['title']}  ({s['lines']} lines, {s['words']} words)")

print()
print('=== PowerShell code block count ===')
ps_count = sum(1 for l in lines if l.startswith('```powershell'))
print(f'  {ps_count} powershell blocks')

print()
print('=== Iron rule keyword coverage ===')
for n in (36, 37, 38, 39, 40):
    cnt = content.count(f'铁则 {n}')
    print(f'  铁则 {n}: {cnt} mentions')
