"""health 路由测试。"""

from __future__ import annotations

from fastapi.testclient import TestClient

from hk_integration.main import app


client = TestClient(app)


def test_healthz_returns_ok() -> None:
    """GET /healthz 返回 status=ok。"""
    resp = client.get("/healthz")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}
