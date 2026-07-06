"""日志配置模块。

使用 loguru 统一控制台与文件日志格式。
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Optional

from loguru import logger


def setup_logging(log_level: str = "INFO", log_file: Optional[str] = "logs/app.log") -> None:
    """初始化 loguru 日志配置。

    Args:
        log_level: 日志级别，如 INFO/DEBUG/WARNING。
        log_file: 日志文件路径，None 表示不写文件。

    Returns:
        None
    """
    logger.remove()
    logger.add(
        sys.stdout,
        level=log_level,
        enqueue=True,
        backtrace=False,
        diagnose=False,
        format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>",
    )
    if log_file:
        log_path = Path(log_file)
        log_path.parent.mkdir(parents=True, exist_ok=True)
        logger.add(
            str(log_path),
            level=log_level,
            rotation="10 MB",
            retention="14 days",
            encoding="utf-8",
            enqueue=True,
            backtrace=False,
            diagnose=False,
            format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8} | {name}:{function}:{line} - {message}",
        )


__all__ = ["logger", "setup_logging"]
