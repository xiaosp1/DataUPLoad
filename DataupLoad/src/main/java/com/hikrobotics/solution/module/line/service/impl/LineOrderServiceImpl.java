package com.hikrobotics.solution.module.line.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.entity.LineOrder;
import com.hikrobotics.solution.module.line.mapper.LineOrderMapper;
import com.hikrobotics.solution.module.line.service.ILineOrderService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Service;

@Service
public class LineOrderServiceImpl extends ServiceImpl<LineOrderMapper, LineOrder> implements ILineOrderService {

    @Override
    public void addLineOrder(List<Integer> lineIds) {
        if (CollectionUtils.isNotEmpty(lineIds)) {
            ArrayList<LineOrder> orders = Lists.newArrayList();
            LineOrder latest = this.getOne(((LambdaQueryWrapper<LineOrder>) Wrappers.<LineOrder>lambdaQuery()
                .orderByDesc(LineOrder::getOrderValue)).last("limit 1"));
            int current = latest == null ? 1 : latest.getOrderValue() + 1;
            for (int id : lineIds) {
                LineOrder lineOrder = new LineOrder();
                lineOrder.setLineId(id).setOrderValue(current);
                current++;
                orders.add(lineOrder);
            }
            this.saveBatch(orders);
        }
    }

    @Override
    public void removeByLineId(Integer lineId) {
        this.remove(Wrappers.<LineOrder>lambdaQuery().eq(LineOrder::getLineId, lineId));
    }

    @Override
    public Boolean modLineOrder(List<ChgLineOrderDTO> lineOrders) {
        if (CollectionUtils.isNotEmpty(lineOrders)) {
            if (this.remove(Wrappers.lambdaQuery())) {
                ArrayList<LineOrder> orders = Lists.newArrayList();
                List<ChgLineOrderDTO> temp = lineOrders.stream()
                    .sorted(Comparator.comparing(ChgLineOrderDTO::getOrder))
                    .toList();
                for (int i = 1; i <= temp.size(); i++) {
                    orders.add(LineOrder.builder()
                        .lineId(temp.get(i - 1).getLineId())
                        .orderValue(i)
                        .build());
                }
                return this.saveBatch(orders);
            }
            return false;
        }
        return true;
    }
}
