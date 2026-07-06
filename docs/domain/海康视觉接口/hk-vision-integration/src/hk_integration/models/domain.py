"""海康视觉领域模型。

仅包含数据结构定义，暂不包含持久化逻辑。
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional


LineGroup = str
DefectType = str
Face = str


@dataclass
class DefectRecord:
    """单条缺陷统计记录。"""

    time: datetime
    line: LineGroup
    defect: DefectType
    face: Face
    detection_count: int = 0


@dataclass
class RemoveCount:
    """剔除数量记录。

    TODO(业务待确认): removeCount 是次品剔除数还是数据删除次数。
    """

    line: LineGroup
    face: Face
    remove_count: int = 0


@dataclass
class ConfigSnapshot:
    """海康配置快照（产线/缺陷/面别字典）。"""

    workshop: str
    line_groups: List[LineGroup] = field(default_factory=list)
    defect_groups: List[DefectType] = field(default_factory=list)
    face_groups: List[Face] = field(default_factory=list)
    fetched_at: Optional[datetime] = None


@dataclass
class DetectionDataResult:
    """一次检测数据查询结果。"""

    query_time: datetime
    defects: List[DefectRecord] = field(default_factory=list)
    remove_counts: List[RemoveCount] = field(default_factory=list)
