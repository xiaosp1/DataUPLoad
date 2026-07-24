package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.entity.LineOrder;
import java.util.List;

public interface ILineOrderService extends IService<LineOrder> {
    void addLineOrder(List<Integer> var1);
    void removeByLineId(Integer var1);
    Boolean modLineOrder(List<ChgLineOrderDTO> var1);
}
