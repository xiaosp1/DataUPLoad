/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.service.IService
 *  com.hikrobotics.solution.framework.common.base.BaseResult
 *  com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm
 *  com.hikrobotics.solution.module.line.model.StateChangePO
 *  com.hikrobotics.solution.module.line.model.StateStatisticPO
 *  com.hikrobotics.solution.module.line.service.IStateChangeService
 */
package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.line.dto.SearchStateStatisticForm;
import com.hikrobotics.solution.module.line.model.StateChangePO;
import com.hikrobotics.solution.module.line.model.StateStatisticPO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface IStateChangeService
extends IService<StateChangePO> {
    public BaseResult handleStateStatisticSearch(SearchStateStatisticForm var1);

    public List<StateStatisticPO> getStateStatistics(Set<Integer> var1, LocalDateTime var2, LocalDateTime var3);

    public void removeBefore(LocalDateTime var1);
}

