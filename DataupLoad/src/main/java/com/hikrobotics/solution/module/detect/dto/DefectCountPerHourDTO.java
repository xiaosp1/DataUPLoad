package com.hikrobotics.solution.module.detect.dto;

import com.hikrobotics.solution.module.screen.dto.DefectNumberDTO;
import java.util.List;

public class DefectCountPerHourDTO {
    private String time;
    private int removeTotal;
    private List<DefectNumberDTO> defects;

    public static DefectCountPerHourDTOBuilder builder() { return new DefectCountPerHourDTOBuilder(); }

    public String getTime() { return time; }
    public int getRemoveTotal() { return removeTotal; }
    public List<DefectNumberDTO> getDefects() { return defects; }

    public void setTime(String time) { this.time = time; }
    public void setRemoveTotal(int removeTotal) { this.removeTotal = removeTotal; }
    public void setDefects(List<DefectNumberDTO> defects) { this.defects = defects; }

    public DefectCountPerHourDTO() {}

    public DefectCountPerHourDTO(String time, int removeTotal, List<DefectNumberDTO> defects) {
        this.time = time;
        this.removeTotal = removeTotal;
        this.defects = defects;
    }

    public static class DefectCountPerHourDTOBuilder {
        private String time;
        private int removeTotal;
        private List<DefectNumberDTO> defects;
        DefectCountPerHourDTOBuilder() {}
        public DefectCountPerHourDTOBuilder time(String time) { this.time = time; return this; }
        public DefectCountPerHourDTOBuilder removeTotal(int removeTotal) { this.removeTotal = removeTotal; return this; }
        public DefectCountPerHourDTOBuilder defects(List<DefectNumberDTO> defects) { this.defects = defects; return this; }
        public DefectCountPerHourDTO build() { return new DefectCountPerHourDTO(time, removeTotal, defects); }
    }
}
