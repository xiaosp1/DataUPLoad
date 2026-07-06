"""应用配置模块。

基于 pydantic-settings 读取环境变量与 .env 文件。
"""

from __future__ import annotations

from functools import lru_cache
from typing import Optional

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """应用运行配置。"""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_env: str = Field(default="dev", description="运行环境：dev/test/prod")
    app_host: str = Field(default="0.0.0.0", description="服务监听地址")
    app_port: int = Field(default=8000, description="服务监听端口")
    log_level: str = Field(default="INFO", description="日志级别")
    log_file: str = Field(default="logs/app.log", description="日志落盘路径")

    inkey_base_url: str = Field(default="http://192.168.32.86:1025", description="英科 dataportal 基地址")
    inkey_username: str = Field(default="HKSJSB", description="英科登录账号")
    inkey_password: str = Field(default="HKSJSB123", description="英科登录密码")
    inkey_inv_org_id: int = Field(default=1, description="英科组织 ID")

    hk_base_url: str = Field(default="http://TODO-HIK-HOST:TODO-HIK-PORT", description="海康视觉中控基地址")
    hk_auth_token: Optional[str] = Field(default=None, description="海康接口鉴权 Token，待确认鉴权方式")
    hk_request_timeout: float = Field(default=10.0, description="海康接口请求超时秒数")

    default_workshop: str = Field(default="HBN1", description="默认车间代码")

    config_sync_cron: str = Field(default="0 */30 * * * *", description="配置同步 cron 表达式")
    detection_sync_cron: str = Field(default="5 0 * * * *", description="检测数据拉取 cron 表达式")

    ticket_ttl_seconds: int = Field(default=1800, description="Ticket 内存缓存 TTL（秒）")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """返回单例配置对象。"""
    return Settings()
