/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.line.dto.LineTreeItemDTO
 *  com.hikrobotics.solution.module.line.model.LinePO
 *  org.assertj.core.util.Lists
 */
package com.hikrobotics.solution.module.line.dto;

import com.hikrobotics.solution.module.line.model.LinePO;
import java.util.List;
import org.assertj.core.util.Lists;

public class LineTreeItemDTO {
    private Integer id;
    private String name;
    private String lineNo;
    private List<LineTreeItemDTO> childs;

    public LineTreeItemDTO(LinePO po) {
        this.setId(po.getId());
        this.setName(po.getName());
        this.setLineNo(po.getLineNo());
        this.setChilds((List)Lists.newArrayList());
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getLineNo() {
        return this.lineNo;
    }

    public List<LineTreeItemDTO> getChilds() {
        return this.childs;
    }

    public LineTreeItemDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public LineTreeItemDTO setName(String name) {
        this.name = name;
        return this;
    }

    public LineTreeItemDTO setLineNo(String lineNo) {
        this.lineNo = lineNo;
        return this;
    }

    public LineTreeItemDTO setChilds(List<LineTreeItemDTO> childs) {
        this.childs = childs;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LineTreeItemDTO)) {
            return false;
        }
        LineTreeItemDTO other = (LineTreeItemDTO)o;
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
        String this$lineNo = this.getLineNo();
        String other$lineNo = other.getLineNo();
        if (this$lineNo == null ? other$lineNo != null : !this$lineNo.equals(other$lineNo)) {
            return false;
        }
        List this$childs = this.getChilds();
        List other$childs = other.getChilds();
        return !(this$childs == null ? other$childs != null : !((Object)this$childs).equals(other$childs));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LineTreeItemDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $lineNo = this.getLineNo();
        result = result * 59 + ($lineNo == null ? 43 : $lineNo.hashCode());
        List $childs = this.getChilds();
        result = result * 59 + ($childs == null ? 43 : ((Object)$childs).hashCode());
        return result;
    }

    public String toString() {
        return "LineTreeItemDTO(id=" + this.getId() + ", name=" + this.getName() + ", lineNo=" + this.getLineNo() + ", childs=" + this.getChilds() + ")";
    }

    public LineTreeItemDTO() {
    }
}

