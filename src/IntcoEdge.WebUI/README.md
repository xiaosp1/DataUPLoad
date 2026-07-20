# IntcoEdge.WebUI

Vue 3 + Element Plus 静态资源目录。Task B1 之前此目录为占位文件。

预期目录结构（待 Task B1 填充）：

```
wwwroot/
├── index.html          # 主入口
├── assets/             # 编译产物（JS / CSS）
└── ...                 # 其它静态资源
```

构建产物会通过 `IntcoEdge.EdgeHost` 的 `UseStaticFiles()` 提供。
