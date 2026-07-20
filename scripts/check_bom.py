#!/usr/bin/env python3
"""Check UTF-8 BOM in source files (skip obj/bin)."""
import os
import sys

root = sys.argv[1] if len(sys.argv) > 1 else r'E:\DEMO\数据采集\src\IntcoEdge.Desktop'
exts = ('.cs', '.xaml', '.csproj', '.config', '.json', '.ps1', '.py', '.md')
skip_dirs = ('obj', 'bin', '.git', 'node_modules')
bom = 0
ok = 0
for r, dirs, fs in os.walk(root):
    dirs[:] = [d for d in dirs if d not in skip_dirs]
    for f in fs:
        if not f.endswith(exts):
            continue
        p = os.path.join(r, f)
        with open(p, 'rb') as fp:
            d = fp.read(3)
        if len(d) >= 3 and d[:3] == b'\xef\xbb\xbf':
            print('BOM:', p)
            bom += 1
        else:
            ok += 1
print(f'Summary: BOM={bom} OK={ok}')
sys.exit(1 if bom > 0 else 0)
