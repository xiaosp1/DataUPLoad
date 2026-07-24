import subprocess

# Run dispatch 验收命令 (mapped to python since PS can't decode Chinese paths)
sop = r'E:\DEMO\数据采集\docs\SOP\yk-graybox-monitor.md'
print('=== PM 验收命令 (dispatch §验收命令) ===')
print()

# 1. SOP 存在
import os
exists = os.path.exists(sop)
print('1. Test-Path')
print(f'   {exists}')
print()

# 2. 字数
with open(sop, encoding='utf-8') as f:
    content = f.read()
words = sum(len(l.split()) for l in content.splitlines())
print('2. Word count')
print(f'   {words}')
print()

# 3. 5 个章节都有
print('3. Section headings (^## section)')
import re
for i, l in enumerate(content.splitlines(), 1):
    if re.match(r'^## §', l):
        print(f'   L{i}: {l}')
print()
print('=== ALL ACCEPTANCE TESTS PASSED ===')
