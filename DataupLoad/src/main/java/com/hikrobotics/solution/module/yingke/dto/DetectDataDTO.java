package com.hikrobotics.solution.module.yingke.dto;

import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import java.util.List;

/**
 * PSM 1:1 DetectDataDTO — 缺陷记录查询返回值。
 * 内含 DefectDataDTO（缺陷记录行）和 RemoveCountDTO（剔除数行）。
 */
public class DetectDataDTO {
    private List<DefectDataDTO> defects;
    private List<RemoveCountDTO> removeCounts;

    public List<DefectDataDTO> getDefects() { return defects; }
    public List<RemoveCountDTO> getRemoveCounts() { return removeCounts; }

    public DetectDataDTO setDefects(List<DefectDataDTO> defects) {
        this.defects = defects;
        return this;
    }

    public DetectDataDTO setRemoveCounts(List<RemoveCountDTO> removeCounts) {
        this.removeCounts = removeCounts;
        return this;
    }

    /** 缺陷记录行（从 DefectDayRecord 转换）。 */
    public static class DefectDataDTO {
        private Integer id;
        private String time;
        private String lineNo;
        private String faceNo;
        private String type;
        private Integer count;

        public static DefectDataDTO convert(DefectDayRecord record) {
            DefectDataDTO dto = new DefectDataDTO();
            dto.id = record.getId();
            dto.time = record.getTime();
            dto.lineNo = record.getLineNo();
            dto.faceNo = record.getFaceNo();
            dto.type = record.getType();
            dto.count = record.getCount();
            return dto;
        }

        public Integer getId() { return id; }
        public String getTime() { return time; }
        public String getLineNo() { return lineNo; }
        public String getFaceNo() { return faceNo; }
        public String getType() { return type; }
        public Integer getCount() { return count; }

        public DefectDataDTO setId(Integer id) { this.id = id; return this; }
        public DefectDataDTO setTime(String time) { this.time = time; return this; }
        public DefectDataDTO setLineNo(String lineNo) { this.lineNo = lineNo; return this; }
        public DefectDataDTO setFaceNo(String faceNo) { this.faceNo = faceNo; return this; }
        public DefectDataDTO setType(String type) { this.type = type; return this; }
        public DefectDataDTO setCount(Integer count) { this.count = count; return this; }
    }

    /** 剔除数行（从 LineDayRecord 转换）。 */
    public static class RemoveCountDTO {
        private Integer id;
        private String time;
        private String lineNo;
        private String faceNo;
        private Integer removeTotal;
        private Integer uploadRemoveTotal;

        public static RemoveCountDTO convert(LineDayRecord record) {
            RemoveCountDTO dto = new RemoveCountDTO();
            dto.id = record.getId();
            dto.time = record.getTime();
            dto.lineNo = record.getLineNo();
            dto.faceNo = record.getFaceNo();
            dto.removeTotal = record.getRemoveTotal();
            dto.uploadRemoveTotal = record.getUploadRemoveTotal();
            return dto;
        }

        public Integer getId() { return id; }
        public String getTime() { return time; }
        public String getLineNo() { return lineNo; }
        public String getFaceNo() { return faceNo; }
        public Integer getRemoveTotal() { return removeTotal; }
        public Integer getUploadRemoveTotal() { return uploadRemoveTotal; }

        public RemoveCountDTO setId(Integer id) { this.id = id; return this; }
        public RemoveCountDTO setTime(String time) { this.time = time; return this; }
        public RemoveCountDTO setLineNo(String lineNo) { this.lineNo = lineNo; return this; }
        public RemoveCountDTO setFaceNo(String faceNo) { this.faceNo = faceNo; return this; }
        public RemoveCountDTO setRemoveTotal(Integer removeTotal) { this.removeTotal = removeTotal; return this; }
        public RemoveCountDTO setUploadRemoveTotal(Integer uploadRemoveTotal) { this.uploadRemoveTotal = uploadRemoveTotal; return this; }
    }
}
