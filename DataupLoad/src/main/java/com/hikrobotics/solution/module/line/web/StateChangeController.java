package com.hikrobotics.solution.module.line.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm;
import com.hikrobotics.solution.module.line.service.IStateChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 状态变更 Controller（W-B05 StateChange）
 *
 * <p>1:1 抄自反编译 PSM StateChangeController，仅把实现类指向 DataupLoad 的
 * {@link IStateChangeService}，与 PSM 同名服务接口签名一致。</p>
 *
 * <ul>
 *   <li>{@code GET /web/line/state/statistic} — 状态变更统计查询（按线体 + 时间范围）</li>
 * </ul>
 */
@RestController
@RequestMapping("/web/line/state")
public class StateChangeController {

    @Autowired
    private IStateChangeService stateChangeService;

    @GetMapping("/statistic")
    public BaseResult searchStateChangeRecord(@Validated SearchStateStatisticForm form) {
        return this.stateChangeService.handleStateStatisticSearch(form);
    }
}
