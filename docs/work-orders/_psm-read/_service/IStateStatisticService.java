/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.service.IService
 *  com.hikrobotics.solution.module.line.model.StateStatisticPO
 *  com.hikrobotics.solution.module.line.service.IStateStatisticService
 */
package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.line.model.StateStatisticPO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface IStateStatisticService
extends IService<StateStatisticPO> {
    public void saveStatisticBatch(List<StateStatisticPO> var1);

    public List<StateStatisticPO> listDailyStatisticDataBetween(Set<Integer> var1, LocalDateTime var2, LocalDateTime var3);

    public void removeBefore(LocalDateTime var1);
}

