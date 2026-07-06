"""健康检查路由。"""

from __future__ import annotations

from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/healthz")
def healthz() -> dict:
    """健康检查接口。

    Returns:
        dict: 固定返回 status=ok。
    """
    return {"status": "ok"}
