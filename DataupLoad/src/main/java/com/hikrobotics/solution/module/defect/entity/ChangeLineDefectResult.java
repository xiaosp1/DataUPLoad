package com.hikrobotics.solution.module.defect.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * DataupLoad 切换产线缺陷结果实体。
 *
 * <p>1:1 抄自 PSM 反编译产物
 * {@code docs\domain\海康大屏逆向\PSM\server\decompiled\com\hikrobotics\solution\module\defect\dto\ChangeLineDefectResult.java}
 * （亦见于 {@code psm-decompiled\BOOT-INF\classes\...} 同名类）。
 *
 * <p><b>重要说明（与工单 W-DFT-01a 描述的差异）：</b>
 * <ul>
 *   <li>PSM 中实际类名是 {@code ChangeLineDefectResult}（位于 {@code defect/dto} 包），<b>不存在</b>
 *       {@code ChangeLineDefectResultPO}；本类即为 PSM 原类的直接 1:1 复制。</li>
 *   <li>PSM 原类只有 2 个字段（{@code needDelDefects} / {@code needAddDefect}，均为
 *       {@code Collection<String>}），<b>不包含</b> 工单描述中的 {@code id / lineId /
 *       beforeResult / afterResult / changeType / changeTime / operatorId / note} 等字段。
 *       本类严格按 1:1 原则未做虚构字段。</li>
 *   <li>所有 V0.x / V1.x 数据库迁移脚本中<b>均不存在</b> {@code change_line_defect_result}
 *       表，因此本类<b>未添加</b> {@code @TableName} / {@code @TableId} / {@code @JsonFormat}
 *       等持久化/序列化注解——它们在原 PSM 类中亦不存在。该类在 PSM 中是返回前端的运行时
 *       DTO，无持久化层。</li>
 *   <li>集合字段使用 {@link ArrayList} 默认初始化，移除 PSM 中对 {@code org.assertj.core.util.Lists}
 *       的依赖（assertj 是测试依赖，不应出现在 entity 中）。</li>
 * </ul>
 *
 * <p>若后续确实需要把"切换产线缺陷变更"持久化（建对应表 + 新增审计字段），应作为单独的
 * 工单处理，不要在本类上叠加臆造字段。
 */
public class ChangeLineDefectResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private Collection<String> needDelDefects = new ArrayList<>();

    private Collection<String> needAddDefect = new ArrayList<>();

    public Collection<String> getNeedDelDefects() {
        return this.needDelDefects;
    }

    public Collection<String> getNeedAddDefect() {
        return this.needAddDefect;
    }

    public void setNeedDelDefects(Collection<String> needDelDefects) {
        this.needDelDefects = needDelDefects;
    }

    public void setNeedAddDefect(Collection<String> needAddDefect) {
        this.needAddDefect = needAddDefect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChangeLineDefectResult)) {
            return false;
        }
        ChangeLineDefectResult other = (ChangeLineDefectResult) o;
        Collection<String> thisNeedDel = this.getNeedDelDefects();
        Collection<String> otherNeedDel = other.getNeedDelDefects();
        if (thisNeedDel == null ? otherNeedDel != null : !thisNeedDel.equals(otherNeedDel)) {
            return false;
        }
        Collection<String> thisNeedAdd = this.getNeedAddDefect();
        Collection<String> otherNeedAdd = other.getNeedAddDefect();
        return thisNeedAdd == null ? otherNeedAdd == null : thisNeedAdd.equals(otherNeedAdd);
    }

    @Override
    public int hashCode() {
        final int prime = 59;
        int result = 1;
        Collection<String> needDel = this.getNeedDelDefects();
        result = result * prime + (needDel == null ? 43 : needDel.hashCode());
        Collection<String> needAdd = this.getNeedAddDefect();
        result = result * prime + (needAdd == null ? 43 : needAdd.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ChangeLineDefectResult(needDelDefects=" + this.getNeedDelDefects()
                + ", needAddDefect=" + this.getNeedAddDefect() + ")";
    }
}
