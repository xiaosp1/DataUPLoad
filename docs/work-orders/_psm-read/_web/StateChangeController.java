/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.framework.common.base.BaseResult
 *  com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm
 *  com.hikrobotics.solution.module.line.service.IStateChangeService
 *  com.hikrobotics.solution.module.line.web.StateChangeController
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.validation.annotation.Validated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.hikrobotics.solution.module.line.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm;
import com.hikrobotics.solution.module.line.service.IStateChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/web/line/state"})
public class StateChangeController {
    @Autowired
    private IStateChangeService stateChangeService;

    @GetMapping(value={"/statistic"})
    public BaseResult searchStateChangeRecord(@Validated SearchStateStatisticForm form) {
        return this.stateChangeService.handleStateStatisticSearch(form);
    }
}

