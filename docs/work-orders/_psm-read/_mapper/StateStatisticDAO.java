/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  com.hikrobotics.solution.module.line.mapper.StateStatisticDAO
 *  com.hikrobotics.solution.module.line.model.StateStatisticPO
 *  org.apache.ibatis.annotations.Param
 */
package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.line.model.StateStatisticPO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface StateStatisticDAO
extends BaseMapper<StateStatisticPO> {
    public void insertBatch(@Param(value="statistics") List<StateStatisticPO> var1);
}

