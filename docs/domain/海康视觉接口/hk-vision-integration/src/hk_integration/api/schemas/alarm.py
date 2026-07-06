"""报警业务模型。"""

from __future__ import annotations

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field

from hk_integration.models.enums import AlarmLevelEnum, AlarmResultEnum, WorkShopEnum


class AlarmPayload(BaseModel):
    """海康侧上传的单条报警对象。"""

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    work_shop: WorkShopEnum = Field(alias="WorkShop", description="车间代码")
    line: str = Field(alias="Line", description="产线")
    face: str = Field(alias="Face", description="面别")
    alarm_time: datetime = Field(alias="AlarmTime", description="报警时间")
    alarm_type: str = Field(alias="AlarmType", description="报警类型")
    alarm_level: AlarmLevelEnum = Field(alias="AlarmLevel", description="报警等级，枚举值待对齐")
    alarm_details: str = Field(alias="AlarmDetails", description="报警内容")
    alarm_result: AlarmResultEnum = Field(alias="AlarmResult", description="处理结果，枚举待确认")
    alarm_count: float = Field(default=1, alias="AlarmCount", description="报警次数")
    ext: Optional[dict] = Field(default=None, description="预留扩展字段")
