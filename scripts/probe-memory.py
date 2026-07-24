import os
p = r'E:\DEMO\数据采集\MEMORY.md'
print('size:', os.path.getsize(p))
print('mtime:', os.path.getmtime(p))
with open(p, 'rb') as f:
    head = f.read(128)
print('first 128 bytes hex:')
print(' '.join(f'{b:02x}' for b in head[:64]))
print(' '.join(f'{b:02x}' for b in head[64:]))
# Try utf-8
try:
    txt = head.decode('utf-8')
    print('utf-8 ok:', repr(txt[:80]))
except UnicodeDecodeError as e:
    print('utf-8 fails:', e)
# Try gbk
try:
    txt = head.decode('gbk')
    print('gbk ok:', repr(txt[:80]))
except UnicodeDecodeError as e:
    print('gbk fails:', e)
# Try cp1252
try:
    txt = head.decode('cp1252')
    print('cp1252 ok:', repr(txt[:80]))
except UnicodeDecodeError as e:
    print('cp1252 fails:', e)
