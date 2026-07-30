/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.framework.common.base.BaseResult
 *  com.hikrobotics.solution.framework.common.query.PageQuery
 *  com.hikrobotics.solution.framework.common.validation.ValidateUtils
 *  com.hikrobotics.solution.framework.component.log.operation.aspect.ApiLog
 *  com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO
 *  com.hikrobotics.solution.module.line.dto.LineBodyDTO
 *  com.hikrobotics.solution.module.line.dto.LinePanelQueryDTO
 *  com.hikrobotics.solution.module.line.dto.LinePlanBindDTO
 *  com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO
 *  com.hikrobotics.solution.module.line.dto.LineUpdateDTO
 *  com.hikrobotics.solution.module.line.service.ILineService
 *  com.hikrobotics.solution.module.line.web.LineController
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.hikrobotics.solution.module.line.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.framework.common.validation.ValidateUtils;
import com.hikrobotics.solution.framework.component.log.operation.aspect.ApiLog;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.dto.LineBodyDTO;
import com.hikrobotics.solution.module.line.dto.LinePanelQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO;
import com.hikrobotics.solution.module.line.dto.LineUpdateDTO;
import com.hikrobotics.solution.module.line.service.ILineService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/web/line"})
public class LineController {
    @Autowired
    private ILineService lineService;

    @ApiLog(operation="\u4ea7\u7ebf\u7ba1\u7406", module="\u4ea7\u7ebf\u67e5\u8be2")
    @GetMapping
    public BaseResult getLineList(PageQuery pageQuery) {
        return this.lineService.listAll(pageQuery);
    }

    @ApiLog(operation="\u4ea7\u7ebf\u7ba1\u7406", module="\u4ea7\u7ebf\u65b0\u589e")
    @PostMapping
    public BaseResult add(@RequestBody LineBodyDTO lineDTO) {
        ValidateUtils.validateEntity((String)"LineController.add", (Object)lineDTO, (Class[])new Class[0]);
        return this.lineService.add(lineDTO);
    }

    @ApiLog(operation="\u4ea7\u7ebf\u7ba1\u7406", module="\u4ea7\u7ebf\u4fee\u6539")
    @PutMapping
    public BaseResult modify(@RequestBody LineUpdateDTO lineUpdateDTO) {
        ValidateUtils.validateEntity((String)"LineController.modify", (Object)lineUpdateDTO, (Class[])new Class[0]);
        return this.lineService.modify(lineUpdateDTO);
    }

    @ApiLog(operation="\u4ea7\u7ebf\u7ba1\u7406", module="\u4ea7\u7ebf\u5220\u9664")
    @DeleteMapping
    public BaseResult delete(@RequestParam Integer id) {
        return this.lineService.delete(id);
    }

    @ApiLog(operation="\u4ea7\u7ebf\u7ba1\u7406", module="\u4fee\u6539\u7ebf\u4f53\u987a\u5e8f")
    @PutMapping(value={"/order"})
    public BaseResult chgLineOrder(@RequestBody List<ChgLineOrderDTO> lineOrders) {
        ValidateUtils.validateEntity((String)"LineController.chgLineOrder", lineOrders, (Class[])new Class[0]);
        return this.lineService.chgLineOrder(lineOrders);
    }

    @ApiLog(operation="\u4ea7\u7ebf\u7ba1\u7406", module="\u67e5\u8be2\u4ea7\u7ebf\u6811")
    @GetMapping(value={"/tree"})
    public BaseResult searchLineTree() {
        return this.lineService.handleLineTreeSearch();
    }

    @ApiLog(operation="\u4ea7\u7ebf\u7ba1\u7406", module="\u4ea7\u7ebf\u914d\u65b9\u5206\u53d1")
    @PostMapping(value={"/plan/bind"})
    public BaseResult dispatchSolution(@RequestBody LinePlanBindDTO linePlanBindDTO) {
        ValidateUtils.validateEntity((String)"LineController.dispatchSolution", (Object)linePlanBindDTO, (Class[])new Class[0]);
        return this.lineService.bindPlan(linePlanBindDTO);
    }

    @ApiLog(operation="\u4ea7\u7ebf\u7ba1\u7406", module="\u4ea7\u7ebf\u914d\u65b9\u5207\u6362")
    @PostMapping(value={"/plan/switch"})
    public BaseResult switchSolution(@RequestBody LinePlanSwitchDTO linePlanSwitchDTO) {
        ValidateUtils.validateEntity((String)"LineController.switchSolution", (Object)linePlanSwitchDTO, (Class[])new Class[0]);
        return this.lineService.switchPlan(linePlanSwitchDTO);
    }

    @GetMapping(value={"/panel"})
    public BaseResult planPanel(LinePanelQueryDTO linePanelQueryDTO) {
        ValidateUtils.validateEntity((String)"PlanController.planPanel", (Object)linePanelQueryDTO, (Class[])new Class[0]);
        return this.lineService.planPanel(linePanelQueryDTO);
    }

    @GetMapping(value={"/status"})
    public BaseResult planStatus(LinePanelQueryDTO linePanelQueryDTO) {
        ValidateUtils.validateEntity((String)"PlanController.planStatus", (Object)linePanelQueryDTO, (Class[])new Class[0]);
        return this.lineService.planStatus(linePanelQueryDTO);
    }

    @GetMapping(value={"/group"})
    public BaseResult lineGroup() {
        return this.lineService.lineGroup();
    }
}

