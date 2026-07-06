"""海康视觉中控平台 HTTP 客户端骨架。

M3 阶段补齐真实 httpx 调用，当前仅定义方法签名与 docstring。
"""

from __future__ import annotations

from datetime import datetime
from typing import List, Optional, Protocol

from hk_integration.models.domain import ConfigSnapshot, DetectionDataResult


class HkVisionClient(Protocol):
    """海康视觉中控平台客户端协议。"""

    async def get_config(self) -> ConfigSnapshot:
        """查询产线组、缺陷类型组、面别组配置。

        Returns:
            ConfigSnapshot: 当前车间基础字典快照。

        Raises:
            HkApiError: 接口返回非 200 或网络异常。
        """
        ...

    async def query_detection_data(
        self,
        time: datetime,
        line_group: Optional[List[str]] = None,
        defect_group: Optional[List[str]] = None,
        face_group: Optional[List[str]] = None,
    ) -> DetectionDataResult:
        """按时间/产线/缺陷/面别查询次品统计数据。

        Args:
            time: 整点时间（YYYY-MM-dd HH:00:00）。
            line_group: 产线过滤，None 表示全部。
            defect_group: 缺陷过滤，None 表示全部。
            face_group: 面别过滤，None 表示全部。

        Returns:
            DetectionDataResult: 缺陷列表 + 删除数量列表。

        Raises:
            HkApiError: 接口返回非 0 或网络异常。
        """
        ...


class HkApiError(RuntimeError):
    """海康接口调用异常。"""


class HttpxHkVisionClient:
    """httpx 实现的海康客户端（M3 补充真实实现）。"""

    def __init__(self, base_url: str, timeout: float = 10.0, auth_token: Optional[str] = None) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.auth_token = auth_token
        # TODO(M3): self._client = httpx.AsyncClient(base_url=..., timeout=..., headers=...)

    async def get_config(self) -> ConfigSnapshot:
        raise NotImplementedError("M3: implement GET /config")

    async def query_detection_data(
        self,
        time: datetime,
        line_group: Optional[List[str]] = None,
        defect_group: Optional[List[str]] = None,
        face_group: Optional[List[str]] = None,
    ) -> DetectionDataResult:
        raise NotImplementedError("M3: implement POST /detection/query")
