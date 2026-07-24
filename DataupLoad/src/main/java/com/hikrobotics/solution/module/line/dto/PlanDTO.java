package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.framework.common.validation.group.GroupA;
import com.hikrobotics.solution.framework.common.validation.group.GroupB;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class PlanDTO {
    @NotNull(groups={GroupB.class})
    private Integer id;
    @NotEmpty(groups={GroupA.class})
    private String name;
    @NotEmpty(groups={GroupA.class})
    private String uri;
    private String description;

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getUri() {
        return this.uri;
    }

    public String getDescription() {
        return this.description;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PlanDTO)) {
            return false;
        }
        PlanDTO other = (PlanDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$id = this.getId();
        Integer other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$uri = this.getUri();
        String other$uri = other.getUri();
        if (this$uri == null ? other$uri != null : !this$uri.equals(other$uri)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        return !(this$description == null ? other$description != null : !this$description.equals(other$description));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PlanDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $uri = this.getUri();
        result = result * 59 + ($uri == null ? 43 : $uri.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        return result;
    }

    public String toString() {
        return "PlanDTO(id=" + this.getId() + ", name=" + this.getName() + ", uri=" + this.getUri() + ", description=" + this.getDescription() + ")";
    }
}

