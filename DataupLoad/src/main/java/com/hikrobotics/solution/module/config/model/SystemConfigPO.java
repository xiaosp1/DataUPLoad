package com.hikrobotics.solution.module.config.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DataupLoad system_config 表 PO（SCFG-1 / W-E 工单）。
 *
 * <p>1:1 抄自 PSM 反编译 {@code com.hikrobotics.solution.module.config.model.SystemConfigPO}。</p>
 *
 * <p>字段对照：</p>
 * <ul>
 *   <li>{@code id} — 主键，自增</li>
 *   <li>{@code configName} — 配置中文名（用于界面展示）</li>
 *   <li>{@code configKey} — 配置键（业务侧使用）</li>
 *   <li>{@code configValue} — 配置值</li>
 *   <li>{@code updateTime / createTime} — 维护时间戳</li>
 * </ul>
 *
 * <p>反编译产物中的 {@code equals / hashCode / toString} 已基于 Java 17 模式匹配（{@code instanceof Pattern}）
 * 重写，方法语义不变，便于人工阅读。</p>
 */
@TableName("system_config")
public class SystemConfigPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String configName;

    private String configKey;

    private String configValue;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;

    public Integer getId() {
        return this.id;
    }

    public String getConfigName() {
        return this.configName;
    }

    public String getConfigKey() {
        return this.configKey;
    }

    public String getConfigValue() {
        return this.configValue;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public SystemConfigPO setId(Integer id) {
        this.id = id;
        return this;
    }

    public SystemConfigPO setConfigName(String configName) {
        this.configName = configName;
        return this;
    }

    public SystemConfigPO setConfigKey(String configKey) {
        this.configKey = configKey;
        return this;
    }

    public SystemConfigPO setConfigValue(String configValue) {
        this.configValue = configValue;
        return this;
    }

    public SystemConfigPO setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public SystemConfigPO setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SystemConfigPO other)) {
            return false;
        }
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$id = this.getId();
        Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        Object this$configName = this.getConfigName();
        Object other$configName = other.getConfigName();
        if (this$configName == null ? other$configName != null : !this$configName.equals(other$configName)) {
            return false;
        }
        Object this$configKey = this.getConfigKey();
        Object other$configKey = other.getConfigKey();
        if (this$configKey == null ? other$configKey != null : !this$configKey.equals(other$configKey)) {
            return false;
        }
        Object this$configValue = this.getConfigValue();
        Object other$configValue = other.getConfigValue();
        if (this$configValue == null ? other$configValue != null : !this$configValue.equals(other$configValue)) {
            return false;
        }
        Object this$updateTime = this.getUpdateTime();
        Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) {
            return false;
        }
        Object this$createTime = this.getCreateTime();
        Object other$createTime = other.getCreateTime();
        return this$createTime == null ? other$createTime == null : this$createTime.equals(other$createTime);
    }

    protected boolean canEqual(Object other) {
        return other instanceof SystemConfigPO;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $configName = this.getConfigName();
        result = result * 59 + ($configName == null ? 43 : $configName.hashCode());
        Object $configKey = this.getConfigKey();
        result = result * 59 + ($configKey == null ? 43 : $configKey.hashCode());
        Object $configValue = this.getConfigValue();
        result = result * 59 + ($configValue == null ? 43 : $configValue.hashCode());
        Object $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : $updateTime.hashCode());
        Object $createTime = this.getCreateTime();
        return result * 59 + ($createTime == null ? 43 : $createTime.hashCode());
    }

    @Override
    public String toString() {
        return "SystemConfigPO(id=" + this.getId()
            + ", configName=" + this.getConfigName()
            + ", configKey=" + this.getConfigKey()
            + ", configValue=" + this.getConfigValue()
            + ", updateTime=" + this.getUpdateTime()
            + ", createTime=" + this.getCreateTime() + ")";
    }
}
