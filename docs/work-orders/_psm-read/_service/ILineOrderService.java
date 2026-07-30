/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.service.IService
 *  com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO
 *  com.hikrobotics.solution.module.line.model.LineOrderPO
 *  com.hikrobotics.solution.module.line.service.ILineOrderService
 */
package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.model.LineOrderPO;
import java.util.List;

public interface ILineOrderService
extends IService<LineOrderPO> {
    public void addLineOrder(List<Integer> var1);

    public void removeByLineId(Integer var1);

    public Boolean modLineOrder(List<ChgLineOrderDTO> var1);
}

