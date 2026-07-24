package com.hikrobotics.solution.module.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hikrobotics.solution.module.config.model.SystemConfigPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * DataupLoad system_config 表 MyBatis-Plus Mapper（SCFG-1 / W-E 工单）。
 *
 * <p>1:1 抄自 PSM 反编译 {@code com.hikrobotics.solution.module.config.mapper.SystemConfigDAO}。
 * 文件名按 DataupLoad 现有约定从 {@code DAO} 改为 {@code Mapper}（参考
 * {@code AlarmRecordMapper} / {@code DefectTypeMapper} 等）。</p>
 *
 * <p>PSM {@code SystemConfigDAO} 只继承了 {@code BaseMapper<SystemConfigPO>}，无自定义方法；
 * 本类同样保持单一继承，{@code @Mapper} 注解让 Spring Boot 启动时自动扫描注册。</p>
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigPO> {
}
