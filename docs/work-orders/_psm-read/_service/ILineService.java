/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.service.IService
 *  com.hikrobotics.solution.framework.common.base.BaseResult
 *  com.hikrobotics.solution.framework.common.query.PageQuery
 *  com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO
 *  com.hikrobotics.solution.module.line.dto.LineBodyDTO
 *  com.hikrobotics.solution.module.line.dto.LinePanelQueryDTO
 *  com.hikrobotics.solution.module.line.dto.LinePlanBindDTO
 *  com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO
 *  com.hikrobotics.solution.module.line.dto.LineUpdateDTO
 *  com.hikrobotics.solution.module.line.model.LinePO
 *  com.hikrobotics.solution.module.line.service.ILineService
 */
package com.hikrobotics.solution.module.line.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.dto.LineBodyDTO;
import com.hikrobotics.solution.module.line.dto.LinePanelQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO;
import com.hikrobotics.solution.module.line.dto.LineUpdateDTO;
import com.hikrobotics.solution.module.line.model.LinePO;
import java.util.List;

public interface ILineService
extends IService<LinePO> {
    public BaseResult listAll(PageQuery var1);

    public BaseResult add(LineBodyDTO var1);

    public BaseResult modify(LineUpdateDTO var1);

    public BaseResult delete(Integer var1);

    public BaseResult bindPlan(LinePlanBindDTO var1);

    public BaseResult switchPlan(LinePlanSwitchDTO var1);

    public BaseResult planPanel(LinePanelQueryDTO var1);

    public BaseResult planStatus(LinePanelQueryDTO var1);

    public BaseResult lineGroup();

    public List<LinePO> listLine();

    public BaseResult chgLineOrder(List<ChgLineOrderDTO> var1);

    public LinePO getByLineNoAndFaceNo(String var1, String var2);

    public BaseResult handleLineTreeSearch();

    public List<LinePO> listByLineNo(List<String> var1);
}

