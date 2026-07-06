"""报警接收路由。

对外暴露 POST /api/dataportal/invoke，按 ApiType/Method 分发：
- AuthenticationController / Login：返回模拟 Ticket（M2 对接真实登录逻辑，当前为骨架）
- VisualInspectionController / HandleVisualInspectionAlarm：接收报警并处理
- 其他：返回 400 业务错误
"""

from __future__ import annotations

from typing import List

from fastapi import APIRouter, Request
from loguru import logger

from hk_integration.api.schemas.alarm import AlarmPayload
from hk_integration.api.schemas.dataportal import DataportalRequest, DataportalResponse
from hk_integration.models.enums import DataportalApiType, DataportalMethod
from hk_integration.services import alarm_service
from hk_integration.services.auth import ticket_cache

router = APIRouter(tags=["dataportal"])


@router.post("/api/dataportal/invoke", response_model=DataportalResponse)
async def dataportal_invoke(payload: DataportalRequest, request: Request) -> DataportalResponse:
    """英科风格统一入口。

    Args:
        payload: dataportal 通用请求信封。
        request: 原始请求对象（用于日志/扩展）。

    Returns:
        DataportalResponse: 通用响应信封。
    """
    api_type = payload.api_type
    method = payload.method
    logger.info("dataportal invoke api_type={} method={} from={}", api_type, method, request.client.host if request.client else "-")

    if api_type == DataportalApiType.VISUAL_INSPECTION and method == DataportalMethod.HANDLE_VISUAL_ALARM:
        return await _handle_alarm(payload)
    if api_type == DataportalApiType.AUTH and method == DataportalMethod.LOGIN:
        return await _handle_login(payload)

    logger.warning("unknown ApiType/Method: {}/{}", api_type, method)
    return DataportalResponse.business_fail(code=400, message=f"unknown ApiType/Method: {api_type}/{method}")


async def _handle_login(payload: DataportalRequest) -> DataportalResponse:
    """登录接口骨架（M2 对接真实账号校验）。

    Args:
        payload: 登录请求。

    Returns:
        DataportalResponse: 含 Ticket 的响应。
    """
    # TODO(M2): 校验 Parameters[0]/[1] 是否等于配置的账号密码
    import secrets
    ticket = secrets.token_urlsafe(48)
    user_code = payload.parameters[0].value if payload.parameters else "UNKNOWN"
    ticket_cache.put(ticket, str(user_code))
    return DataportalResponse(
        Success=True,
        Message=None,
        Result={
            "code": 200,
            "UserId": 50001,
            "EmployeeId": 60002,
            "UserCode": user_code,
            "UserName": f"海康视觉设备[{user_code}]",
            "InvOrg": 1,
        },
        Context={"Ticket": ticket, "InvOrgId": 1},
    )


async def _handle_alarm(payload: DataportalRequest) -> DataportalResponse:
    """处理报警上传。

    Args:
        payload: 报警请求，Parameters[0].Value 为报警数组。

    Returns:
        DataportalResponse: 业务结果。
    """
    ticket = payload.context.ticket
    if not ticket or not ticket_cache.is_valid(ticket):
        logger.warning("alarm rejected: invalid or missing ticket")
        return DataportalResponse.business_fail(code=401, message="invalid or missing ticket")

    if not payload.parameters:
        return DataportalResponse.business_fail(code=400, message="parameters required")

    raw_list = payload.parameters[0].value or []
    if not isinstance(raw_list, list):
        return DataportalResponse.business_fail(code=400, message="Parameters[0].Value must be a list")

    alarms: List[AlarmPayload] = []
    for item in raw_list:
        try:
            alarms.append(AlarmPayload.model_validate(item))
        except Exception as exc:  # noqa: BLE001
            logger.warning("alarm item validation failed: {}", exc)
            return DataportalResponse.business_fail(code=400, message=f"invalid alarm item: {exc}")

    stat = await alarm_service.ingest_alarms(alarms)
    return DataportalResponse.ok(code=200, message="ok", **stat)
