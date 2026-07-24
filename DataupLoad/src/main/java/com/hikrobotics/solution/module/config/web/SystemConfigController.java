package com.hikrobotics.solution.module.config.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.config.model.SystemConfigPO;
import com.hikrobotics.solution.module.config.service.ISystemConfigService;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DataupLoad 系统配置 Controller（SCFG-1 / W-E 工单）。
 *
 * <p>1:1 抄自 PSM 反编译
 * {@code com.hikrobotics.solution.module.config.web.SystemConfigController}。</p>
 *
 * <p>路由（{@code /web/system-config}）：</p>
 * <ul>
 *   <li>{@code GET /web/system-config} — 大屏读取全部配置项</li>
 *   <li>{@code PUT /web/system-config} — 大屏编辑保存（整批更新，行数校验由 Service 内部完成）</li>
 * </ul>
 */
@RestController
@RequestMapping("/web/system-config")
public class SystemConfigController {

    @Autowired
    private ISystemConfigService systemConfigService;

    /**
     * 大屏查询系统配置列表（无分页，整张表回传）。
     */
    @GetMapping
    public BaseResult searchSystemConfig() {
        return BaseResult.build().data(this.systemConfigService.list());
    }

    /**
     * 大屏保存系统配置。
     *
     * <p>入参为非空列表（{@code @NotEmpty}），内部由
     * {@link ISystemConfigService#handleSystemConfigChg(ArrayList)} 校验行数一致性。</p>
     */
    @PutMapping
    public BaseResult chgSystemConfig(@RequestBody @Validated @NotEmpty ArrayList<SystemConfigPO> form) {
        return this.systemConfigService.handleSystemConfigChg(form);
    }
}
