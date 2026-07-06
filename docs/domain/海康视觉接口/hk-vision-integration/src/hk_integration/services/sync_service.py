"""定时同步服务骨架。

- 同步海康配置（产线/缺陷/面别字典）
- 拉取检测数据（按整点小时）
"""

from __future__ import annotations

from datetime import datetime
from typing import List, Optional

from loguru import logger


async def sync_config(workshop: Optional[str] = None) -> None:
    """同步海康基础字典。

    Args:
        workshop: 可选车间代码，默认使用配置中 DEFAULT_WORKSHOP。

    Returns:
        None
    """
    # TODO(M3): 调用 HkVisionClient.get_config() 并写入配置缓存
    logger.info("sync_config triggered workshop={} (skeleton)", workshop)


async def sync_detection_data(
    target_time: datetime,
    lines: Optional[List[str]] = None,
    defects: Optional[List[str]] = None,
    faces: Optional[List[str]] = None,
) -> None:
    """按整点拉取检测数据。

    Args:
        target_time: 查询整点时间（如 14:00:00）。
        lines: 可选产线过滤。
        defects: 可选缺陷类型过滤。
        faces: 可选面别过滤。

    Returns:
        None
    """
    # TODO(M3): 调用 HkVisionClient.query_detection_data(...) 并落库/转发
    logger.info(
        "sync_detection_data triggered time={} lines={} defects={} faces={} (skeleton)",
        target_time.isoformat(),
        lines,
        defects,
        faces,
    )
