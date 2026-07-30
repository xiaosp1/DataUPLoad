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

/**
 * DataupLoad 缺陷类型 Controller（W-DEFECT-CFG 子单 A）。
 *
 * <h2>路由策略（双路径兼容）</h2>
 * <ul>
 *   <li>主路径：{@code /web/defect}（前端 E2 已按这个调）</li>
 *   <li>兼容路径：{@code /web/defect-api}（PSM 老 SPA {@code defectManage.js} 调，1:1 保留）</li>
 * </ul>
 * 通过 {@link RequestMapping#value()} 数组声明两个 prefix，4 个 HTTP method 在两个 prefix 下都生效。
 *
 * <h2>端点清单（与 PSM 1:1 对齐）</h2>
 * <ol>
 *   <li>{@code POST   /web/defect | /web/defect-api}        → {@code handleDefectTypeAdd} (PSM addDefectType 1:1)</li>
 *   <li>{@code DELETE /web/defect | /web/defect-api}        → {@code handleDefectTypeDel} (PSM delDefectType 1:1)</li>
 *   <li>{@code GET    /web/defect | /web/defect-api}        → {@code listDefect}         (PSM listDefect 1:1)</li>
 *   <li>{@code PUT    /web/defect | /web/defect-api}        → {@code editDefect}         (PSM editDefect 1:1)</li>
 * </ol>
 *
 * <h2>API 日志</h2>
 * {@code @ApiLog(operation="缺陷配置", module="新增/编辑/删除")} 与既有 AlarmRecordController 风格一致，
 * 写到 {@code api_log} 表（铁则 30：增删改必记日志）。
 */
@RestController
@RequestMapping(value = {"/web/defect", "/web/defect-api"})
public class DefectTypeController {
    @Autowired
    private IDefectTypeService defectTypeService;

    @PostMapping
    @ApiLog(operation = "缺陷配置", module = "新增缺陷")
    public BaseResult addDefectType(@RequestBody @Validated(AddGroup.class) DefectTypeDTO form) {
        return this.defectTypeService.handleDefectTypeAdd(form);
    }

    @DeleteMapping
    @ApiLog(operation = "缺陷配置", module = "删除缺陷")
    public BaseResult delDefectType(@Validated IdQuery id) {
        return this.defectTypeService.handleDefectTypeDel(id.getId());
    }

    @GetMapping
    public BaseResult listDefect(@Validated SearchDefectDTO form) {
        return this.defectTypeService.listDefect(form);
    }

    @PutMapping
    @ApiLog(operation = "缺陷配置", module = "编辑缺陷")
    public BaseResult editDefect(@Validated(UpdateGroup.class) @RequestBody DefectTypeDTO form) {
        return this.defectTypeService.editDefect(form);
    }
}
