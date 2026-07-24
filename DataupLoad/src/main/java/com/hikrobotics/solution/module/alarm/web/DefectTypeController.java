package com.hikrobotics.solution.module.alarm.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.IdQuery;
import com.hikrobotics.solution.framework.common.validation.group.AddGroup;
import com.hikrobotics.solution.framework.common.validation.group.UpdateGroup;
import com.hikrobotics.solution.framework.component.log.operation.aspect.ApiLog;
import com.hikrobotics.solution.module.alarm.dto.DefectTypeDTO;
import com.hikrobotics.solution.module.alarm.dto.SearchDefectDTO;
import com.hikrobotics.solution.module.alarm.service.IDefectTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/web/defect"})
public class DefectTypeController {
    @Autowired
    private IDefectTypeService defectTypeService;

    @PostMapping
    @ApiLog(operation="故障管理", module="新增故障")
    public BaseResult addDefectType(@RequestBody @Validated(value={AddGroup.class}) DefectTypeDTO form) {
        return this.defectTypeService.handleDefectTypeAdd(form);
    }

    @DeleteMapping
    @ApiLog(operation="故障管理", module="删除故障")
    public BaseResult delDefectType(@Validated IdQuery id) {
        return this.defectTypeService.handleDefectTypeDel(id.getId());
    }

    @GetMapping
    BaseResult listDefect(@Validated SearchDefectDTO form) {
        return this.defectTypeService.listDefect(form);
    }

    @PutMapping
    @ApiLog(operation="故障管理", module="修改故障")
    BaseResult editDefect(@Validated(value={UpdateGroup.class}) @RequestBody DefectTypeDTO form) {
        return this.defectTypeService.editDefect(form);
    }
}
