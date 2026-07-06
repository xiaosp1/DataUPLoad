"""Ticket 内存缓存与校验。

当前实现仅为骨架：内存存储 + TTL 过期判断。
后续可替换为 Redis/数据库持久化。
"""

from __future__ import annotations

import threading
import time
from dataclasses import dataclass
from typing import Optional


@dataclass
class _TicketEntry:
    ticket: str
    user_code: str
    created_at: float
    ttl_seconds: int


class TicketCache:
    """进程内 Ticket 缓存。"""

    def __init__(self, ttl_seconds: int = 1800) -> None:
        self._ttl = ttl_seconds
        self._store: dict[str, _TicketEntry] = {}
        self._lock = threading.Lock()

    def put(self, ticket: str, user_code: str, ttl_seconds: Optional[int] = None) -> None:
        """写入 Ticket。

        Args:
            ticket: Ticket 字符串。
            user_code: 关联的用户编码。
            ttl_seconds: 可选 TTL，默认使用初始化值。

        Raises:
            ValueError: ticket 为空。
        """
        if not ticket:
            raise ValueError("ticket must not be empty")
        with self._lock:
            self._store[ticket] = _TicketEntry(
                ticket=ticket,
                user_code=user_code,
                created_at=time.time(),
                ttl_seconds=ttl_seconds or self._ttl,
            )

    def is_valid(self, ticket: Optional[str]) -> bool:
        """校验 Ticket 是否存在且未过期。

        Args:
            ticket: 待校验 Ticket。

        Returns:
            bool: True 表示合法。
        """
        if not ticket:
            return False
        with self._lock:
            entry = self._store.get(ticket)
            if entry is None:
                return False
            if time.time() - entry.created_at > entry.ttl_seconds:
                self._store.pop(ticket, None)
                return False
            return True

    def clear(self) -> None:
        """清空缓存（测试用）。"""
        with self._lock:
            self._store.clear()


# 全局单例，M2 中会被登录流程填充
ticket_cache = TicketCache()
