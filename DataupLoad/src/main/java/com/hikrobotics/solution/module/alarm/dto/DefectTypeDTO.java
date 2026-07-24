/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.hikrobotics.solution.framework.common.validation.group.AddGroup
 *  com.hikrobotics.solution.framework.common.validation.group.UpdateGroup
 *  com.hikrobotics.solution.module.alarm.dto.DefectTypeDTO
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 */
package com.hikrobotics.solution.module.alarm.dto;

import com.hikrobotics.solution.framework.common.validation.group.AddGroup;
import com.hikrobotics.solution.framework.common.validation.group.UpdateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DefectTypeDTO {
    @NotNull(groups={UpdateGroup.class})
    private Integer id;
    @NotBlank(groups={AddGroup.class, UpdateGroup.class})
    private String name;
    @NotNull(groups={AddGroup.class, UpdateGroup.class})
    private Integer category;
    @NotNull(groups={AddGroup.class, UpdateGroup.class})
    private Integer alarmEnable;
    @NotNull(groups={AddGroup.class, UpdateGroup.class})
    private Integer sendYkEnable;
    @NotNull(groups={AddGroup.class, UpdateGroup.class})
    private Integer soundEnable;

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Integer getCategory() {
        return this.category;
    }

    public Integer getAlarmEnable() {
        return this.alarmEnable;
    }

    public Integer getSendYkEnable() {
        return this.sendYkEnable;
    }

    public Integer getSoundEnable() {
        return this.soundEnable;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public void setAlarmEnable(Integer alarmEnable) {
        this.alarmEnable = alarmEnable;
    }

    public void setSendYkEnable(Integer sendYkEnable) {
        this.sendYkEnable = sendYkEnable;
    }

    public void setSoundEnable(Integer soundEnable) {
        this.soundEnable = soundEnable;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DefectTypeDTO)) {
            return false;
        }
        DefectTypeDTO other = (DefectTypeDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$id = this.getId();
        Integer other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$category = this.getCategory();
        Integer other$category = other.getCategory();
        if (this$category == null ? other$category != null : !((Object)this$category).equals(other$category)) {
            return false;
        }
        Integer this$alarmEnable = this.getAlarmEnable();
        Integer other$alarmEnable = other.getAlarmEnable();
        if (this$alarmEnable == null ? other$alarmEnable != null : !((Object)this$alarmEnable).equals(other$alarmEnable)) {
            return false;
        }
        Integer this$sendYkEnable = this.getSendYkEnable();
        Integer other$sendYkEnable = other.getSendYkEnable();
        if (this$sendYkEnable == null ? other$sendYkEnable != null : !((Object)this$sendYkEnable).equals(other$sendYkEnable)) {
            return false;
        }
        Integer this$soundEnable = this.getSoundEnable();
        Integer other$soundEnable = other.getSoundEnable();
        if (this$soundEnable == null ? other$soundEnable != null : !((Object)this$soundEnable).equals(other$soundEnable)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        return !(this$name == null ? other$name != null : !this$name.equals(other$name));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DefectTypeDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : ((Object)$category).hashCode());
        Integer $alarmEnable = this.getAlarmEnable();
        result = result * 59 + ($alarmEnable == null ? 43 : ((Object)$alarmEnable).hashCode());
        Integer $sendYkEnable = this.getSendYkEnable();
        result = result * 59 + ($sendYkEnable == null ? 43 : ((Object)$sendYkEnable).hashCode());
        Integer $soundEnable = this.getSoundEnable();
        result = result * 59 + ($soundEnable == null ? 43 : ((Object)$soundEnable).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    public String toString() {
        return "DefectTypeDTO(id=" + this.getId() + ", name=" + this.getName() + ", category=" + this.getCategory() + ", alarmEnable=" + this.getAlarmEnable() + ", sendYkEnable=" + this.getSendYkEnable() + ", soundEnable=" + this.getSoundEnable() + ")";
    }
}
