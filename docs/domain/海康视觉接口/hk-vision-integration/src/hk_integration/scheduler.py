"""定时调度器模块。

使用 APScheduler 挂载配置同步、检测数据拉取等周期性任务。
当前仅提供启动/关闭骨架，任务注册在 M3 阶段补充。
"""

from __future__ import annotations

from typing import Optional

from apscheduler.schedulers.background import BackgroundScheduler

_scheduler: Optional[BackgroundScheduler] = None


def start_scheduler() -> BackgroundScheduler:
    """启动后台调度器，幂等。

    Returns:
        BackgroundScheduler: 当前运行的调度器实例。
    """
    global _scheduler
    if _scheduler is not None and _scheduler.running:
        return _scheduler

    _scheduler = BackgroundScheduler(timezone="Asia/Shanghai")
    # TODO(M3): 注册配置同步与检测数据拉取任务
    # _scheduler.add_job(sync_config, trigger=CronTrigger.from_crontab(...), id="sync_config")
    # _scheduler.add_job(sync_detection, trigger=CronTrigger.from_crontab(...), id="sync_detection")
    _scheduler.start()
    return _scheduler


def stop_scheduler() -> None:
    """关闭调度器并释放资源。"""
    global _scheduler
    if _scheduler is not None and _scheduler.running:
        _scheduler.shutdown(wait=False)
    _scheduler = None


def get_scheduler() -> Optional[BackgroundScheduler]:
    """返回当前调度器实例（未启动时返回 None）。"""
    return _scheduler
