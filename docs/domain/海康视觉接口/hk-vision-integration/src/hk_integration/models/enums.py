"""领域枚举定义。"""

from __future__ import annotations

from enum import Enum


class WorkShopEnum(str, Enum):
    """车间代码枚举（淮北/江西/青州基地）。"""

    # 淮北丁腈
    HBN1 = "HBN1"
    HBN2 = "HBN2"
    HBN3 = "HBN3"
    HBN4 = "HBN4"
    HBN5 = "HBN5"
    HBN6 = "HBN6"
    # 淮北 PVC
    HBP1 = "HBP1"
    HBP2 = "HBP2"
    HBP3 = "HBP3"
    HBP4 = "HBP4"
    HBP5 = "HBP5"
    HBP6 = "HBP6"
    # 江西丁腈
    JXN1 = "JXN1"
    JXN2 = "JXN2"
    JXN3 = "JXN3"
    JXN4 = "JXN4"
    # 青州基地
    QZM1 = "QZM1"
    QZN1 = "QZN1"
    QZN2 = "QZN2"
    QZN3 = "QZN3"
    QZP1 = "QZP1"
    QZP2 = "QZP2"
    QZP3 = "QZP3"


class AlarmLevelEnum(str, Enum):
    """报警等级占位枚举，待与海康对齐枚举值。"""

    LOW = "Low"
    MEDIUM = "Medium"
    HIGH = "High"
    CRITICAL = "Critical"


class AlarmResultEnum(str, Enum):
    """报警处理结果占位枚举。

    TODO(业务待确认): 实际状态集需要与英科/海康对齐，当前仅占位：
    - OPEN: 未处理/待处理
    - CLOSED: 已处理
    - _FALSE_ALARM: 误报（前缀下划线表示非最终命名）
    """

    OPEN = "OPEN"
    CLOSED = "CLOSED"
    _FALSE_ALARM = "FALSE_ALARM"


class DataportalApiType(str, Enum):
    """英科 dataportal ApiType 常量。"""

    AUTH = "AuthenticationController"
    VISUAL_INSPECTION = "VisualInspectionController"


class DataportalMethod(str, Enum):
    """英科 dataportal Method 常量。"""

    LOGIN = "Login"
    HANDLE_VISUAL_ALARM = "HandleVisualInspectionAlarm"
