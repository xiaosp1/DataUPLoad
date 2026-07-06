"""英科 dataportal HTTP 客户端骨架。

M2 阶段补齐登录、报警上传的真实调用。
"""

from __future__ import annotations

from typing import List, Optional, Protocol

from hk_integration.api.schemas.alarm import AlarmPayload


class InkeyClient(Protocol):
    """英科 dataportal 客户端协议。"""

    async def login(self, username: str, password: str) -> str:
        """登录英科 dataportal 并获取 Ticket。

        Args:
            username: 账号。
            password: 密码。

        Returns:
            str: 可用的 Ticket 字符串。

        Raises:
            InkeyApiError: 登录失败。
        """
        ...

    async def upload_alarms(self, ticket: str, alarms: List[AlarmPayload]) -> dict:
        """批量上传报警数据到英科。

        Args:
            ticket: 登录返回的 Ticket。
            alarms: 报警对象列表。

        Returns:
            dict: 英科返回的 Result 部分（含 code/message 等）。

        Raises:
            InkeyApiError: 接口调用失败。
        """
        ...


class InkeyApiError(RuntimeError):
    """英科接口调用异常。"""


class HttpxInkeyClient:
    """httpx 实现的英科客户端（M2 补充真实实现）。"""

    def __init__(self, base_url: str, inv_org_id: int = 1, timeout: float = 10.0) -> None:
        self.base_url = base_url.rstrip("/")
        self.inv_org_id = inv_org_id
        self.timeout = timeout
        # TODO(M2): self._client = httpx.AsyncClient(...)

    async def login(self, username: str, password: str) -> str:
        raise NotImplementedError("M2: implement AuthenticationController.Login")

    async def upload_alarms(self, ticket: str, alarms: List[AlarmPayload]) -> dict:
        raise NotImplementedError("M2: implement VisualInspectionController.HandleVisualInspectionAlarm")
