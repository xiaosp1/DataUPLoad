#!/usr/bin/env python3
"""Print first 16 bytes hex for all source files."""
import os
import sys

root = sys.argv[1] if len(sys.argv) > 1 else r'E:\DEMO\数据采集\src\IntcoEdge.Desktop'
skip_dirs = ('obj', 'bin', '.git')
for r, dirs, fs in os.walk(root):
    dirs[:] = [d for d in dirs if d not in skip_dirs]
    for f in sorted(fs):
        if not f.endswith(('.cs', '.xaml', '.csproj')):
            continue
        p = os.path.join(r, f)
        with open(p, 'rb') as fp:
            d = fp.read(16)
        hex_str = ' '.join(f'{b:02X}' for b in d)
        print(f'{hex_str}  {p}')
