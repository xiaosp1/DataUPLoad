# tests/

本目录用于单元测试与集成测试项目（xUnit / NUnit），当前为空占位。

后续规划为以下模块补测试：
- `IntcoEdge.MesUpload`：HttpMesClient/MockMesClient/MesUploadWorker 的错误处理、幂等、退避、离线降级
- `IntcoEdge.Storage`：MesUploadQueue 的入队/出队/重试/死信/幂等/审计
- `IntcoEdge.EdgeHost`：启动与健康检查集成测试