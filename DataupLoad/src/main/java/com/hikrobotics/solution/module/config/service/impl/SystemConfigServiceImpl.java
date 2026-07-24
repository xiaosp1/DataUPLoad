package com.hikrobotics.solution.module.config.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.config.mapper.SystemConfigMapper;
import com.hikrobotics.solution.module.config.model.SystemConfigPO;
import com.hikrobotics.solution.module.config.service.ISystemConfigService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * DataupLoad 系统配置服务实现（SCFG-1 / W-E 工单）。
 *
 * <p>1:1 抄自 PSM 反编译
 * {@code com.hikrobotics.solution.module.config.service.imp.SystemConfigServiceImpl}，
 * 仅调整以下差异：</p>
 * <ul>
 *   <li>包名：{@code imp} → {@code impl}（Spring/Java 社区约定）</li>
 *   <li>DAO 引用：{@code SystemConfigDAO} → {@code SystemConfigMapper}（项目命名风格统一）</li>
 * </ul>
 *
 * <p>业务逻辑：</p>
 * <ol>
 *   <li>{@link #handleSystemConfigChg(List)} 校验行数 → {@code updateBatchById} → 返回 ok / 业务错误码。</li>
 *   <li>{@link #listByConfigKey(List)} 空集合短路；非空时用 {@code Wrappers.lambdaQuery().in(...)} 查询。</li>
 * </ol>
 */
@Service
public class SystemConfigServiceImpl
        extends ServiceImpl<SystemConfigMapper, SystemConfigPO>
        implements ISystemConfigService {

    @Override
    public BaseResult handleSystemConfigChg(List<SystemConfigPO> form) {
        List<SystemConfigPO> existConfig = this.list();
        if (form == null || existConfig.size() != form.size()) {
            // 行数不一致：业务侧错误码 20601
            return BaseResult.build().error("20601");
        }
        if (this.updateBatchById(form)) {
            return BaseResult.build().ok();
        }
        // 批量更新失败：通用错误码 20001
        return BaseResult.build().error("20001");
    }

    @Override
    public List<SystemConfigPO> listByConfigKey(List<String> configKeys) {
        if (CollectionUtils.isNotEmpty(configKeys)) {
            return this.list(Wrappers.<SystemConfigPO>lambdaQuery()
                    .in(SystemConfigPO::getConfigKey, configKeys));
        }
        return new ArrayList<>();
    }
}
