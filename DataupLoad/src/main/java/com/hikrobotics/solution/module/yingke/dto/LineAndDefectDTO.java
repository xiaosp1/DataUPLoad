package com.hikrobotics.solution.module.yingke.dto;

import java.util.HashSet;
import java.util.Set;

/**
 * PSM 1:1 LineAndDefectDTO — 产线+缺陷类型下拉集合。
 */
public class LineAndDefectDTO {
    private Set<String> lineGroup = new HashSet<>();
    private Set<String> faceGroup = new HashSet<>();
    private Set<String> defectGroup = new HashSet<>();

    public Set<String> getLineGroup() { return lineGroup; }
    public void setLineGroup(Set<String> lineGroup) { this.lineGroup = lineGroup; }

    public Set<String> getFaceGroup() { return faceGroup; }
    public void setFaceGroup(Set<String> faceGroup) { this.faceGroup = faceGroup; }

    public Set<String> getDefectGroup() { return defectGroup; }
    public void setDefectGroup(Set<String> defectGroup) { this.defectGroup = defectGroup; }
}
