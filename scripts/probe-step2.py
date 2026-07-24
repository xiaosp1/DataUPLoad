files = {
    'STATUS.md': r'E:\DEMO\数据采集\STATUS.md',
    'INDEX.md': r'E:\DEMO\数据采集\docs\delivered\INDEX.md',
    'TODO.md': r'E:\DEMO\数据采集\TODO.md',
}

print('=== Verify "灰盒跑法 SOP 引用" section added ===')
for name, p in files.items():
    with open(p, encoding='utf-8') as f:
        content = f.read()
    has_marker = '灰盒跑法 SOP 引用' in content
    has_doc_link = 'yk-graybox-monitor.md' in content
    line_count = content.count('\n')
    print(f'  {name}:')
    print(f'    size: {len(content)} chars, lines: {line_count}')
    print(f'    has "灰盒跑法 SOP 引用": {has_marker}')
    print(f'    has "yk-graybox-monitor.md" link: {has_doc_link}')
    print()
