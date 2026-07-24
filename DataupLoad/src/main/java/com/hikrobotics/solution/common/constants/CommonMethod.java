package com.hikrobotics.solution.common.constants;

import com.hikrobotics.solution.common.constants.WsTypeEnum;
import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.framework.component.ws.model.WsMessage;

/**
 * PSM 1:1 静态工具方法（不依赖 framework-starter 注入）。
 * <p>
 * 整理自 PSM 反编译 {@code CommonMethod}，仅包含 plan change 推送方法。
 */
public class CommonMethod {

    public static void sendPlanChange(WebSocketHandler webSocketHandler, String clientNo) {
        WsMessage wsData = WsMessage.build()
            .type(WsTypeEnum.PLAN_CHANGE.getValue())
            .data("changePlan");
        webSocketHandler.broadcastByUid(wsData.toJsonString(), clientNo);
    }
}
