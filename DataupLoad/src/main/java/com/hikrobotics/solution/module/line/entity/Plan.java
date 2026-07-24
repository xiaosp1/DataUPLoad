package com.hikrobotics.solution.module.line.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("plan")
public class Plan implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String name;
    private String uri;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

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

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public Plan setId(Integer id) {
        this.id = id;
        return this;
    }

    public Plan setName(String name) {
        this.name = name;
        return this;
    }

    public Plan setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public Plan setDescription(String description) {
        this.description = description;
        return this;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Plan setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Plan setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Plan other)) {
            return false;
        } else {
            if (!other.canEqual(this)) {
                return false;
            }
            Object this$id = this.getId();
            Object other$id = other.getId();
            if (this$id == null ? other$id == null : this$id.equals(other$id)) {
                Object this$name = this.getName();
                Object other$name = other.getName();
                if (this$name == null ? other$name == null : this$name.equals(other$name)) {
                    Object this$uri = this.getUri();
                    Object other$uri = other.getUri();
                    if (this$uri == null ? other$uri == null : this$uri.equals(other$uri)) {
                        Object this$description = this.getDescription();
                        Object other$description = other.getDescription();
                        if (this$description == null ? other$description == null : this$description.equals(other$description)) {
                            Object this$updateTime = this.getUpdateTime();
                            Object other$updateTime = other.getUpdateTime();
                            if (this$updateTime == null ? other$updateTime == null : this$updateTime.equals(other$updateTime)) {
                                Object this$createTime = this.getCreateTime();
                                Object other$createTime = other.getCreateTime();
                                return this$createTime == null ? other$createTime == null : this$createTime.equals(other$createTime);
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Plan;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        Object $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        Object $uri = this.getUri();
        result = result * 59 + ($uri == null ? 43 : $uri.hashCode());
        Object $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        Object $updateTime = this.getUpdateTime();
        result = result * 59 + ($updateTime == null ? 43 : $updateTime.hashCode());
        Object $createTime = this.getCreateTime();
        return result * 59 + ($createTime == null ? 43 : $createTime.hashCode());
    }

    @Override
    public String toString() {
        return "Plan(id="
            + this.getId()
            + ", name="
            + this.getName()
            + ", uri="
            + this.getUri()
            + ", description="
            + this.getDescription()
            + ", updateTime="
            + this.getUpdateTime()
            + ", createTime="
            + this.getCreateTime()
            + ")";
    }
}
