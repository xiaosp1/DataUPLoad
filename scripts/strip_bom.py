#!/usr/bin/env python3
"""Strip UTF-8 BOM from all files in a directory (recursively)."""
import os
import sys

root = sys.argv[1] if len(sys.argv) > 1 else r'E:\DEMO\数据采集\src\IntcoEdge.Desktop'
exts = ('.cs', '.xaml', '.csproj', '.config', '.json', '.ps1', '.py', '.md')
n = 0
for r, _, fs in os.walk(root):
    for f in fs:
        if not f.endswith(exts):
            continue
        p = os.path.join(r, f)
        with open(p, 'rb') as fp:
            d = fp.read()
        if d.startswith(b'\xef\xbb\xbf'):
            with open(p, 'wb') as fp:
                fp.write(d[3:])
            print('STRIP:', p)
            n += 1
print(f'Stripped {n} files.')
