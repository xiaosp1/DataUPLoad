package com.hikrobotics.solution.module.detect.service;

import com.hikrobotics.solution.framework.util.HikDateUtil;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * PSM 1:1 SearchDefectRecordDTO — 缺陷记录查询条件（detect 模块引用版本）。
 */
public class SearchDefectRecordDTO {
    private String startTime;
    private String endTime;
    private Set<String> lindGroup = new HashSet<>();
    private Set<String> defectGroup = new HashSet<>();
    private Set<String> faceGroup = new HashSet<>();

    public LocalDateTime getStartTime() { return HikDateUtil.transformTime(this.startTime); }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return HikDateUtil.transformTime(this.endTime); }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Set<String> getLindGroup() { return lindGroup; }
    public void setLindGroup(Set<String> lindGroup) { this.lindGroup = lindGroup; }

    public Set<String> getDefectGroup() { return defectGroup; }
    public void setDefectGroup(Set<String> defectGroup) { this.defectGroup = defectGroup; }

    public Set<String> getFaceGroup() { return faceGroup; }
    public void setFaceGroup(Set<String> faceGroup) { this.faceGroup = faceGroup; }
}
