package com.hikrobotics.solution.module.alarm.dto;

import com.hikrobotics.solution.framework.common.query.TimePageQuery;

/**
 * Web 后台报警查询 DTO（PSM AlarmQueryDTO 1:1 对齐）。
 *
 * <p>W-ALM-02 修复：补齐 PSM 端 5 个字段（{@code type/level/solve/faceId/sortType}），
 * 让 {@code AlarmRecordServiceImpl.listAll(...)} 可以正常调用 PSM 同款 lambda 查询条件。
 *
 * <p>继承 {@link TimePageQuery}（PSM 同款），复用 {@code startTime/endTime/pageNum/pageSize}
 * + {@code localStartTime()/localEndTime()/isPaged()/getPage()} 公共方法。</p>
 */
public class AlarmQueryDTO extends TimePageQuery {
    private Integer type;
    private Integer level;
    private Integer solve;
    /** PSM 端为 Integer；前端传 line.id（Integer）查询当前产线工位下的报警 */
    private Integer faceId;
    /** PSM 默认值 1；{@code 0} = 升序，{@code 1} = 降序（按 time 排序） */
    private Integer sortType = 1;

    public Integer getType() { return this.type; }
    public Integer getLevel() { return this.level; }
    public Integer getSolve() { return this.solve; }
    public Integer getFaceId() { return this.faceId; }
    public Integer getSortType() { return this.sortType; }

    public void setType(Integer type) { this.type = type; }
    public void setLevel(Integer level) { this.level = level; }
    public void setSolve(Integer solve) { this.solve = solve; }
    public void setFaceId(Integer faceId) { this.faceId = faceId; }
    public void setSortType(Integer sortType) { this.sortType = sortType; }
}
