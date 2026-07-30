/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.baomidou.mybatisplus.core.toolkit.CollectionUtils
 *  com.baomidou.mybatisplus.core.toolkit.Wrappers
 *  com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
 *  com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO
 *  com.hikrobotics.solution.module.line.mapper.LineOrderDAO
 *  com.hikrobotics.solution.module.line.model.LineOrderPO
 *  com.hikrobotics.solution.module.line.service.ILineOrderService
 *  com.hikrobotics.solution.module.line.service.imp.LineOrderServiceImpl
 *  org.assertj.core.util.Lists
 *  org.springframework.stereotype.Service
 */
package com.hikrobotics.solution.module.line.service.imp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.mapper.LineOrderDAO;
import com.hikrobotics.solution.module.line.model.LineOrderPO;
import com.hikrobotics.solution.module.line.service.ILineOrderService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Service;

@Service
public class LineOrderServiceImpl
extends ServiceImpl<LineOrderDAO, LineOrderPO>
implements ILineOrderService {
    public void addLineOrder(List<Integer> lineIds) {
        if (CollectionUtils.isNotEmpty(lineIds)) {
            ArrayList orders = Lists.newArrayList();
            LineOrderPO latest = (LineOrderPO)this.getOne((Wrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().orderByDesc(LineOrderPO::getOrderValue)).last("limit 1"));
            int current = latest == null ? 1 : latest.getOrderValue() + 1;
            for (int id : lineIds) {
                LineOrderPO lineOrder = new LineOrderPO();
                lineOrder.setLineId(Integer.valueOf(id)).setOrderValue(Integer.valueOf(current));
                ++current;
                orders.add(lineOrder);
            }
            this.saveBatch((Collection)orders);
        }
    }

    public void removeByLineId(Integer lineId) {
        this.remove((Wrapper)Wrappers.lambdaQuery().eq(LineOrderPO::getLineId, (Object)lineId));
    }

    public Boolean modLineOrder(List<ChgLineOrderDTO> lineOrders) {
        if (CollectionUtils.isNotEmpty(lineOrders)) {
            if (this.remove((Wrapper)Wrappers.lambdaQuery())) {
                ArrayList orders = Lists.newArrayList();
                List<ChgLineOrderDTO> temp = lineOrders.stream().sorted(Comparator.comparing(ChgLineOrderDTO::getOrder)).toList();
                for (int i = 1; i <= temp.size(); ++i) {
                    orders.add(LineOrderPO.builder().lineId(temp.get(i - 1).getLineId()).orderValue(Integer.valueOf(i)).build());
                }
                return this.saveBatch((Collection)orders);
            }
            return false;
        }
        return true;
    }
}

