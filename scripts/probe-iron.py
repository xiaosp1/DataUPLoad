sop = r'E:\DEMO\数据采集\docs\SOP\yk-graybox-monitor.md'
with open(sop, encoding='utf-8') as f:
    lines = f.readlines()

print('=== Word count (whitespace-split) ===')
print(sum(len(l.split()) for l in lines))

print()
print('=== Section headings (^## ) ===')
for i, l in enumerate(lines, 1):
    if l.startswith('## '):
        print(f'{i:4d}: {l.rstrip()}')

print()
print('=== Sub-headings (^### ) - including iron rules ===')
for i, l in enumerate(lines, 1):
    if l.startswith('### '):
        print(f'{i:4d}: {l.rstrip()}')

print()
print('=== Iron rules 36-40 keyword presence ===')
content = ''.join(lines)
for n in (36, 37, 38, 39, 40):
    cnt = content.count(f'铁则 {n}')
    print(f'  铁则 {n}: {cnt} occurrences')

print()
print('=== Code blocks (powershell) count ===')
ps = sum(1 for l in lines if l.startswith('```powershell'))
print(f'  powershell blocks: {ps}')
print(f'  total lines: {len(lines)}')

print()
print('=== Iron rule original-text presence (sampling unique phrases) ===')
markers = {
    36: 'yk.enable 永久 false',
    37: '任务跨 6 步',
    38: 'Get-Process -Name java',
    39: 'PM 每小时必须体检',
    40: 'Worker Step 0~3 完成后',
}
for n, m in markers.items():
    found = m in content
    print(f'  铁则 {n}: [{"OK" if found else "MISS"}] -> "{m}"')
