package com.hikrobotics.solution.module.screen.dto;

import com.hikrobotics.solution.module.alarm.entity.AlarmRecord;
import com.hikrobotics.solution.module.line.dto.RealTimeDetectData;
import com.hikrobotics.solution.module.screen.dto.ClientStatusDTO;
import com.hikrobotics.solution.module.screen.dto.DefectNumberDTO;
import com.hikrobotics.solution.module.screen.dto.ScreenDataDTO;
import java.util.ArrayList;
import java.util.List;

public class ScreenDataDTO {
    private List<DetectDataDTO> detectData = new ArrayList<>();
    private List<DefectNumberDTO> defectSum = new ArrayList<>();
    private int removeSum;
    private List<ClientStatusDTO> clientStatusList = new ArrayList<>();
    private List<AlarmRecord> alarms = new ArrayList<>();

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
     * 鍐呭祵 DetectDataDTO 鈥斺€?PSM 鍙嶇紪璇戜骇鐗╂湭鍗曠嫭杈撳嚭璇ョ被锛圕FR 娉ㄩ噴閲屽垪涓?     * "Could not load the following classes"锛夛紝杩欓噷鎸?{@code ScreenServiceImpl}
     * 璋冪敤鏂瑰紡锛坈hain setter锛夊弽鍚戣ˉ榻愬瓧娈碉細
     * <ul>
     *   <li>lineNo / faceNo / order / lineId / color 鈥斺€?绾夸綋鍩虹灞炴€?/li>
     *   <li>removeTotal 鈥斺€?褰撳墠灏忔椂鍓旈櫎鏁?/li>
     *   <li>hourDefectCount 鈥斺€?褰撳墠灏忔椂鍚?defect 鍚嶇粏鏁?/li>
     *   <li>realTimeDetectData 鈥斺€?瀹炴椂妫€娴嬫暟鎹紙line.realtimeData 鍙嶅簭鍒楀寲寰楀埌锛?/li>
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
