package com.hikrobotics.solution.module.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.config.model.SystemConfigPO;
import java.util.List;

/**
 * DataupLoad 系统配置服务接口（SCFG-1 / W-E 工单）。
 *
 * <p>1:1 抄自 PSM 反编译
 * {@code com.hikrobotics.solution.module.config.service.ISystemConfigService}。</p>
 *
 * <p>继承自 MyBatis-Plus {@link IService}，获得 {@code list / getOne / saveBatch / updateBatchById} 等
 * 通用 CRUD；自定义业务方法保留原签名：</p>
 * <ul>
 *   <li>{@link #handleSystemConfigChg(List)} — 大屏编辑保存配置项（按整批 id 全量更新）</li>
 *   <li>{@link #listByConfigKey(List)} — 按 configKey 集合拉取配置项</li>
 * </ul>
 */
public interface ISystemConfigService extends IService<SystemConfigPO> {

    /**
     * 大屏端保存系统配置（按 id 全量更新传入列表）。
     *
     * <p>业务校验：当前数据库行数必须等于入参数量，否则返回业务错误码
     * {@code 20601}（行数不一致）。更新失败返回 {@code 20001}。</p>
     *
     * @param form 前端提交的配置项集合（含 id）
     * @return 统一 BaseResult
     */
    BaseResult handleSystemConfigChg(List<SystemConfigPO> form);

    /**
     * 按 configKey 集合查询配置项。
     *
     * <p>空集合直接返回空列表，避免拼出 {@code WHERE config_key IN ()} 这种非法 SQL。</p>
     *
     * @param configKeys 配置键集合
     * @return 命中的配置项列表
     */
    List<SystemConfigPO> listByConfigKey(List<String> configKeys);
}
