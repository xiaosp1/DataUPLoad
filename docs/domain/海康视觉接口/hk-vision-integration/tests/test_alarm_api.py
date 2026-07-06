"""报警 API 测试。"""

from __future__ import annotations

from fastapi.testclient import TestClient

from hk_integration.main import app
from hk_integration.services.auth import ticket_cache


client = TestClient(app)


def _login() -> str:
    resp = client.post(
        "/api/dataportal/invoke",
        json={
            "ApiType": "AuthenticationController",
            "Method": "Login",
            "Parameters": [{"Value": "HKSJSB"}, {"Value": "HKSJSB123"}],
            "Context": {},
        },
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["Result"]["code"] == 200
    return body["Context"]["Ticket"]


def test_upload_alarm_without_ticket_returns_401() -> None:
    """未携带 Ticket 上传报警应返回业务错误 code=401。"""
    ticket_cache.clear()
    resp = client.post(
        "/api/dataportal/invoke",
        json={
            "ApiType": "VisualInspectionController",
            "Method": "HandleVisualInspectionAlarm",
            "Parameters": [
                {
                    "Value": [
                        {
                            "WorkShop": "HBN1",
                            "Line": "line1A",
                            "Face": "A面",
                            "AlarmTime": "2024-08-30T14:30:00",
                            "AlarmType": "Temperature Alert",
                            "AlarmLevel": "High",
                            "AlarmDetails": "Temperature exceeded threshold",
                            "AlarmResult": "CLOSED",
                            "AlarmCount": 1,
                        }
                    ]
                }
            ],
            "Context": {},
        },
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["Success"] is True
    assert body["Result"]["code"] == 401


def test_upload_alarm_with_valid_ticket_returns_200() -> None:
    """合法 Ticket 上传 1 条报警应返回 code=200。"""
    ticket_cache.clear()
    ticket = _login()
    resp = client.post(
        "/api/dataportal/invoke",
        json={
            "ApiType": "VisualInspectionController",
            "Method": "HandleVisualInspectionAlarm",
            "Parameters": [
                {
                    "Value": [
                        {
                            "WorkShop": "HBN1",
                            "Line": "line1A",
                            "Face": "A面",
                            "AlarmTime": "2024-08-30T14:30:00",
                            "AlarmType": "Temperature Alert",
                            "AlarmLevel": "High",
                            "AlarmDetails": "Temperature exceeded threshold",
                            "AlarmResult": "CLOSED",
                            "AlarmCount": 1,
                        }
                    ]
                }
            ],
            "Context": {"Ticket": ticket, "InvOrgId": 1},
        },
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["Success"] is True
    assert body["Result"]["code"] == 200
    assert body["Result"]["accepted"] == 1
