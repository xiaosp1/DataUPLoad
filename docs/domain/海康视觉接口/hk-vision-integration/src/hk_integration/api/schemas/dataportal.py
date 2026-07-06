"""英科 dataportal 通用请求/响应模型。

英科接口统一使用 PascalCase 字段名，模型通过 alias 适配。
"""

from __future__ import annotations

from typing import Any, Dict, List, Literal, Optional

from pydantic import BaseModel, ConfigDict, Field


class RequestContext(BaseModel):
    """dataportal 请求上下文。"""

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    ticket: Optional[str] = Field(default=None, alias="Ticket", description="登录后获取的 Ticket")
    inv_org_id: Optional[int] = Field(default=1, alias="InvOrgId", description="组织 ID")


class Parameter(BaseModel):
    """通用参数键值包装。"""

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    value: Any = Field(default=None, alias="Value")


class DataportalRequest(BaseModel):
    """dataportal 通用请求信封。"""

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    api_type: str = Field(alias="ApiType")
    method: str = Field(alias="Method")
    parameters: List[Parameter] = Field(default_factory=list, alias="Parameters")
    context: RequestContext = Field(default_factory=RequestContext, alias="Context")


class ResponseContext(BaseModel):
    """dataportal 响应上下文。"""

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    ticket: Optional[str] = Field(default=None, alias="Ticket")
    inv_org_id: Optional[int] = Field(default=None, alias="InvOrgId")


class DataportalResponse(BaseModel):
    """dataportal 通用响应信封。"""

    model_config = ConfigDict(populate_by_name=True, extra="allow")

    success: bool = Field(default=True, alias="Success")
    message: Optional[str] = Field(default=None, alias="Message")
    result: Dict[str, Any] = Field(default_factory=dict, alias="Result")
    context: ResponseContext = Field(default_factory=ResponseContext, alias="Context")

    @classmethod
    def ok(cls, code: int = 200, message: Optional[str] = None, **extra: Any) -> "DataportalResponse":
        """快速构造成功响应（Result.code=200）。"""
        result: Dict[str, Any] = {"code": code}
        if message is not None:
            result["message"] = message
        result.update(extra)
        return cls(Success=True, Message=message, Result=result, Context=ResponseContext())

    @classmethod
    def business_fail(cls, code: int = 400, message: str = "business error") -> "DataportalResponse":
        """快速构造业务失败响应（HTTP 200，但 Result.code!=200）。"""
        return cls(Success=True, Message=message, Result={"code": code, "message": message}, Context=ResponseContext())


AuthenticationApiType = Literal["AuthenticationController"]
VisualInspectionApiType = Literal["VisualInspectionController"]
LoginMethod = Literal["Login"]
HandleAlarmMethod = Literal["HandleVisualInspectionAlarm"]
