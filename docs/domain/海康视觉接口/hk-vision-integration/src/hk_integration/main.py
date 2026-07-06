"""海康视觉与英科 dataportal 对接服务 FastAPI 入口。

职责：
- 加载配置与日志
- 挂载路由（health、alarm）
- 启动/关闭 APScheduler 定时任务
"""

from __future__ import annotations

from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI

from hk_integration.api.routes import alarm, health
from hk_integration.config import get_settings
from hk_integration.logging import setup_logging
from hk_integration.scheduler import start_scheduler, stop_scheduler


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """FastAPI 生命周期：启动定时任务、关闭时清理。"""
    settings = get_settings()
    setup_logging(log_level=settings.log_level, log_file=settings.log_file)
    start_scheduler()
    try:
        yield
    finally:
        stop_scheduler()


def create_app() -> FastAPI:
    """创建 FastAPI 应用实例。"""
    app = FastAPI(
        title="HK Vision Integration",
        description="海康视觉中控平台 <-> 英科 dataportal 对接服务",
        version="0.1.0",
        lifespan=lifespan,
    )
    app.include_router(health.router)
    app.include_router(alarm.router)
    return app


app = create_app()


if __name__ == "__main__":  # pragma: no cover
    import uvicorn

    settings = get_settings()
    uvicorn.run(
        "hk_integration.main:app",
        host=settings.app_host,
        port=settings.app_port,
        reload=settings.app_env == "dev",
    )
