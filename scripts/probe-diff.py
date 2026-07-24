import os

# Show the new "灰盒跑法 SOP 引用" sections from each file
files = {
    'STATUS.md': r'E:\DEMO\数据采集\STATUS.md',
    'INDEX.md (line 19-22)': r'E:\DEMO\数据采集\docs\delivered\INDEX.md',
    'TODO.md': r'E:\DEMO\数据采集\TODO.md',
}

for name, p in files.items():
    print(f'\n========== {name} ==========')
    with open(p, encoding='utf-8') as f:
        lines = f.readlines()
    # find marker
    for i, l in enumerate(lines):
        if 'yk-graybox' in l:
            # print 5 lines before and 20 lines after
            start = max(0, i - 5)
            end = min(len(lines), i + 20)
            for j in range(start, end):
                marker = '>>>' if j == i else '   '
                print(f'{marker} {j+1:4d}: {lines[j].rstrip()}')
            break
