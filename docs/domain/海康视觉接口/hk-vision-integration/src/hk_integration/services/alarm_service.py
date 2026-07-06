"""报警业务处理服务。

职责：
- 幂等去重（按 WorkShop+Line+Face+AlarmTime+AlarmType 生成幂等键）
- 落日志/落库（当前仅落 loguru 日志，M4 接入持久化）
- 调用英科 dataportal 转发（M2 对接 InkeyClient）
"""

from __future__ import annotations

import hashlib
from typing import Iterable, List

from loguru import logger

from hk_integration.api.schemas.alarm import AlarmPayload


def make_idempotent_key(alarm: AlarmPayload) -> str:
    """生成报警幂等键。

    Args:
        alarm: 单条报警对象。

    Returns:
        str: 16 进制 SHA1 字符串。
    """
    raw = "|".join(
        [
            alarm.work_shop.value,
            alarm.line,
            alarm.face,
            alarm.alarm_time.isoformat(),
            alarm.alarm_type,
        ]
    )
    return hashlib.sha1(raw.encode("utf-8")).hexdigest()


async def ingest_alarms(alarms: Iterable[AlarmPayload]) -> dict:
    """接收并处理一批报警。

    Args:
        alarms: 报警对象列表。

    Returns:
        dict: {"accepted": int, "duplicated": int} 统计信息。
    """
    accepted = 0
    duplicated = 0
    seen: set[str] = set()
    for alarm in alarms:
        key = make_idempotent_key(alarm)
        if key in seen:
            duplicated += 1
            continue
        seen.add(key)
        accepted += 1
        logger.info(
            "alarm ingested key={} workshop={} line={} face={} time={} type={} result={}",
            key[:12],
            alarm.work_shop.value,
            alarm.line,
            alarm.face,
            alarm.alarm_time.isoformat(),
            alarm.alarm_type,
            alarm.alarm_result.value,
        )
        # TODO(M2): 调用 InkeyClient.upload_alarms 转发到英科
    logger.info("alarm batch done accepted={} duplicated={}", accepted, duplicated)
    return {"accepted": accepted, "duplicated": duplicated}
