/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.hikrobotics.solution.module.alarm.entity.AlarmRecord
 *  com.hikrobotics.solution.module.screen.dto.ClientStatusDTO
 *  com.hikrobotics.solution.module.screen.dto.DefectNumberDTO
 *  com.hikrobotics.solution.module.screen.dto.ScreenDataDTO
 *  com.hikrobotics.solution.module.screen.dto.ScreenDataDTO$DetectDataDTO
 *  org.assertj.core.util.Lists
 */
package com.hikrobotics.solution.module.screen.dto;

import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import com.hikrobotics.solution.module.line.dto.RealTimeDetectData;
import com.hikrobotics.solution.module.screen.dto.ClientStatusDTO;
import com.hikrobotics.solution.module.screen.dto.DefectNumberDTO;
import com.hikrobotics.solution.module.screen.dto.ScreenDataDTO;
import java.util.List;
import org.assertj.core.util.Lists;

public class ScreenDataDTO {
    private List<DetectDataDTO> detectData = Lists.newArrayList();
    private List<DefectNumberDTO> defectSum = Lists.newArrayList();
    private int removeSum;
    private List<ClientStatusDTO> clientStatusList = Lists.newArrayList();
    private List<AlarmRecord> alarms = Lists.newArrayList();

    public List<DetectDataDTO> getDetectData() {
        return this.detectData;
    }

    public List<DefectNumberDTO> getDefectSum() {
        return this.defectSum;
    }

    public int getRemoveSum() {
        return this.removeSum;
    }

    public List<ClientStatusDTO> getClientStatusList() {
        return this.clientStatusList;
    }

    public List<AlarmRecord> getAlarms() {
        return this.alarms;
    }

    public void setDetectData(List<DetectDataDTO> detectData) {
        this.detectData = detectData;
    }

    public void setDefectSum(List<DefectNumberDTO> defectSum) {
        this.defectSum = defectSum;
    }

    public void setRemoveSum(int removeSum) {
        this.removeSum = removeSum;
    }

    public void setClientStatusList(List<ClientStatusDTO> clientStatusList) {
        this.clientStatusList = clientStatusList;
    }

    public void setAlarms(List<AlarmRecord> alarms) {
        this.alarms = alarms;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ScreenDataDTO)) {
            return false;
        }
        ScreenDataDTO other = (ScreenDataDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getRemoveSum() != other.getRemoveSum()) {
            return false;
        }
        List this$detectData = this.getDetectData();
        List other$detectData = other.getDetectData();
        if (this$detectData == null ? other$detectData != null : !((Object)this$detectData).equals(other$detectData)) {
            return false;
        }
        List this$defectSum = this.getDefectSum();
        List other$defectSum = other.getDefectSum();
        if (this$defectSum == null ? other$defectSum != null : !((Object)this$defectSum).equals(other$defectSum)) {
            return false;
        }
        List this$clientStatusList = this.getClientStatusList();
        List other$clientStatusList = other.getClientStatusList();
        if (this$clientStatusList == null ? other$clientStatusList != null : !((Object)this$clientStatusList).equals(other$clientStatusList)) {
            return false;
        }
        List this$alarms = this.getAlarms();
        List other$alarms = other.getAlarms();
        return !(this$alarms == null ? other$alarms != null : !((Object)this$alarms).equals(other$alarms));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ScreenDataDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getRemoveSum();
        List $detectData = this.getDetectData();
        result = result * 59 + ($detectData == null ? 43 : ((Object)$detectData).hashCode());
        List $defectSum = this.getDefectSum();
        result = result * 59 + ($defectSum == null ? 43 : ((Object)$defectSum).hashCode());
        List $clientStatusList = this.getClientStatusList();
        result = result * 59 + ($clientStatusList == null ? 43 : ((Object)$clientStatusList).hashCode());
        List $alarms = this.getAlarms();
        result = result * 59 + ($alarms == null ? 43 : ((Object)$alarms).hashCode());
        return result;
    }

    public String toString() {
        return "ScreenDataDTO(detectData=" + this.getDetectData() + ", defectSum=" + this.getDefectSum() + ", removeSum=" + this.getRemoveSum() + ", clientStatusList=" + this.getClientStatusList() + ", alarms=" + this.getAlarms() + ")";
    }

    /**
     * 内嵌 DetectDataDTO —— PSM 反编译产物未单独输出该类（CFR 注释里列为
     * "Could not load the following classes"），这里按 {@code ScreenServiceImpl}
     * 调用方式（chain setter）反向补齐字段：
     * <ul>
     *   <li>lineNo / faceNo / order / lineId / color —— 线体基础属性</li>
     *   <li>removeTotal —— 当前小时剔除数</li>
     *   <li>hourDefectCount —— 当前小时各 defect 名细数</li>
     *   <li>realTimeDetectData —— 实时检测数据（line.realtimeData 反序列化得到）</li>
     * </ul>
     */
    public static class DetectDataDTO {
        private String lineNo;
        private String faceNo;
        private Integer order;
        private Integer lineId;
        private String color;
        private int removeTotal;
        private List<DefectNumberDTO> hourDefectCount;
        private RealTimeDetectData realTimeDetectData;

        public String getLineNo() {
            return this.lineNo;
        }

        public String getFaceNo() {
            return this.faceNo;
        }

        public Integer getOrder() {
            return this.order;
        }

        public Integer getLineId() {
            return this.lineId;
        }

        public String getColor() {
            return this.color;
        }

        public int getRemoveTotal() {
            return this.removeTotal;
        }

        public List<DefectNumberDTO> getHourDefectCount() {
            return this.hourDefectCount;
        }

        public RealTimeDetectData getRealTimeDetectData() {
            return this.realTimeDetectData;
        }

        public DetectDataDTO setLineNo(String lineNo) {
            this.lineNo = lineNo;
            return this;
        }

        public DetectDataDTO setFaceNo(String faceNo) {
            this.faceNo = faceNo;
            return this;
        }

        public DetectDataDTO setOrder(Integer order) {
            this.order = order;
            return this;
        }

        public DetectDataDTO setLineId(Integer lineId) {
            this.lineId = lineId;
            return this;
        }

        public DetectDataDTO setColor(String color) {
            this.color = color;
            return this;
        }

        public DetectDataDTO setRemoveTotal(int removeTotal) {
            this.removeTotal = removeTotal;
            return this;
        }

        public DetectDataDTO setHourDefectCount(List<DefectNumberDTO> hourDefectCount) {
            this.hourDefectCount = hourDefectCount;
            return this;
        }

        public DetectDataDTO setRealTimeDetectData(RealTimeDetectData realTimeDetectData) {
            this.realTimeDetectData = realTimeDetectData;
            return this;
        }
    }
}
