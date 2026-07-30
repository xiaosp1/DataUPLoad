package com.hikrobotics.solution.module.screen.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.google.common.collect.Maps;
import com.hikrobotics.solution.common.constants.WsTypeEnum;
import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.framework.component.ws.model.WsMessage;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.defect.service.ILineDefectTypeService;
import com.hikrobotics.solution.module.detect.enums.DeviceStatus;
import com.hikrobotics.solution.module.detect.enums.DeviceType;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import com.hikrobotics.solution.module.detect.service.IDefectDayRecordService;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.entity.LineDayRecord;
import com.hikrobotics.solution.module.line.service.ILineDayRecordService;
import com.hikrobotics.solution.module.line.service.ILineService;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import com.hikrobotics.solution.module.line.dto.RealTimeDetectData;
import com.hikrobotics.solution.module.line.entity.LineDefectType;
import com.hikrobotics.solution.module.screen.dto.ClientStatusDTO;
import com.hikrobotics.solution.module.screen.dto.DefectNumberDTO;
import com.hikrobotics.solution.module.screen.dto.ScreenDataDTO;
import com.hikrobotics.solution.module.screen.service.IScreenService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScreenServiceImpl
implements IScreenService {
    @Autowired
    private ILineDefectTypeService lineDefectTypeService;
    @Autowired
    private IDefectDayRecordService defectDayRecordService;
    @Autowired
    private ILineService lineService;
    @Autowired
    private ILineDayRecordService lineDayRecordService;
    @Autowired
    private IStatusRecordService statusRecordService;
    @Autowired
    private WebSocketHandler webSocketHandler;

    @Override
    public void sendScreenDataInfo() {
        ScreenDataDTO screenDataDTO = this.buildScreenData();
        WsMessage wsMessage = WsMessage.build().type(WsTypeEnum.SCREEN.getValue()).data((Object)screenDataDTO);
        // W-PERF-B：用 broadcastByType("screen") 仅投递给 type=screen 的客户端，
        // 避免原 broadcastByUid("web") 把大屏数据广播到 alarm / sound 客户端造成污染。
        this.webSocketHandler.broadcastByType(wsMessage.toJsonString(), WsTypeEnum.SCREEN.getValue());
    }

    private ScreenDataDTO buildScreenData() {
        ScreenDataDTO result = new ScreenDataDTO();
        Set<String> needShowDefectNames = new HashSet<>();
        Map<String, Map<String, LineDefectType>> sortDefectByPosAndName = Maps.newHashMap();
        this.lineDefectTypeService.listIfShowEnable(null, null).forEach(defect -> {
            needShowDefectNames.add(defect.getName());
            Map<String, LineDefectType> sortDefectByName = sortDefectByPosAndName.getOrDefault(defect.getPos(), Maps.newHashMap());
            sortDefectByName.put(defect.getName(), defect);
            sortDefectByPosAndName.put(defect.getPos(), sortDefectByName);
        });
        String currentHours = HikDateUtil.formatLocalDate((LocalDateTime)LocalDateTime.now(), (String)"yyyy-MM-dd HH") + ":00:00";
        Map<String, Map<String, List<DefectDayRecord>>> sortDefectByPosAndType = this.defectDayRecordService.listByStartTimeAndDefect(needShowDefectNames, currentHours).stream().collect(Collectors.groupingBy(DefectDayRecord::getType, Collectors.groupingBy(DefectDayRecord::getPos)));
        boolean isCalcTotalDefectCount = false;
        Map<String, LineDayRecord> sortDayRecordByFace = Maps.newHashMap();
        this.lineDayRecordService.listByTime(currentHours).forEach(data -> {
            sortDayRecordByFace.put(data.getKey(), data);
            result.setRemoveSum(result.getRemoveSum() + data.getRemoveTotal());
        });
        List<Line> lines = this.lineService.listLine().stream().sorted(Comparator.comparingInt(Line::getOrder).thenComparing(Line::getColor)).collect(Collectors.toList());
        for (Line line : lines) {
            ScreenDataDTO.DetectDataDTO detectDataOfLine = new ScreenDataDTO.DetectDataDTO().setLineNo(line.getLineNo()).setFaceNo(line.getFaceNo()).setOrder(line.getOrder()).setLineId(line.getId()).setColor(line.getColor());
            Map<String, LineDefectType> sortDefectByName = sortDefectByPosAndName.getOrDefault(line.getPos(), Maps.newHashMap());
            List<DefectNumberDTO> defectCounts = new ArrayList<DefectNumberDTO>();
            for (String defectName : needShowDefectNames) {
                Integer totalCountOfFace = sortDefectByName.containsKey(defectName) ? Integer.valueOf(0) : null;
                Map<String, List<DefectDayRecord>> sortDefectByPos = sortDefectByPosAndType.getOrDefault(defectName, new HashMap<String, List<DefectDayRecord>>(0));
                if (null != totalCountOfFace && sortDefectByPos.containsKey(line.getPos())) {
                    totalCountOfFace = sortDefectByPos.get(line.getPos()).stream().map(DefectDayRecord::getCount).reduce(0, Integer::sum);
                }
                DefectNumberDTO defectNumberDTO = new DefectNumberDTO().setDefectName(defectName).setDefectCount(totalCountOfFace);
                defectCounts.add(defectNumberDTO);
                if (isCalcTotalDefectCount) continue;
                Integer totalDefectCount = sortDefectByPos.values().stream().flatMap(counts -> counts.stream().map(DefectDayRecord::getCount)).reduce(0, Integer::sum);
                DefectNumberDTO defectTotalCount = new DefectNumberDTO();
                defectTotalCount.setDefectName(defectName);
                defectTotalCount.setDefectCount(totalDefectCount);
                result.getDefectSum().add(defectTotalCount);
            }
            if (sortDayRecordByFace.containsKey(line.getKey())) {
                detectDataOfLine.setRemoveTotal(sortDayRecordByFace.get(line.getKey()).getRemoveTotal().intValue());
            }
            detectDataOfLine.setHourDefectCount(defectCounts);
            if (StringUtils.isNotBlank((CharSequence)line.getRealtimeData())) {
                detectDataOfLine.setRealTimeDetectData((RealTimeDetectData)JSONUtil.toBean((String)line.getRealtimeData(), RealTimeDetectData.class));
            }
            result.getDetectData().add(detectDataOfLine);
            isCalcTotalDefectCount = true;
        }
        List<ClientStatusDTO> clientStatusDTOList = this.getCilentStatusList(lines);
        result.setClientStatusList(clientStatusDTOList);
        return result;
    }

    private List<ClientStatusDTO> getCilentStatusList(List<Line> lines) {
        List<StatusRecord> statusRecordPOList = this.statusRecordService.list();
        Map<String, List<StatusRecord>> lineStatusMap = new HashMap<String, List<StatusRecord>>();
        for (StatusRecord status : statusRecordPOList) {
            List<StatusRecord> lineStatusList = lineStatusMap.getOrDefault(status.getLine(), new ArrayList<StatusRecord>());
            lineStatusList.add(status);
            lineStatusMap.putIfAbsent(status.getLineNo() + ":" + status.getFaceNo(), lineStatusList);
        }
        List<ClientStatusDTO> clientStatusDTOList = new ArrayList<ClientStatusDTO>();
        for (Line line : lines) {
            Collection<StatusRecord> lineStatus = lineStatusMap.getOrDefault(line.getKey(), new ArrayList<StatusRecord>()).stream().collect(Collectors.toMap(StatusRecord::getDeviceNo, Function.identity(), (o, n) -> o.getId() > n.getId() ? o : n)).values();
            Boolean cameraStatus = null;
            Boolean eliminatorStatus = null;
            Boolean clientStatus = null;
            for (StatusRecord statusRecordPO : lineStatus) {
                Boolean status = DeviceStatus.ONLINE.getValue().equals(statusRecordPO.getStatus()) ? Boolean.TRUE : Boolean.FALSE;
                if (DeviceType.CAMERA.getValue().equals(statusRecordPO.getType())) {
                    cameraStatus = cameraStatus == null ? status : status & cameraStatus;
                    continue;
                }
                if (DeviceType.MACHINE.getValue().equals(statusRecordPO.getType())) {
                    eliminatorStatus = eliminatorStatus == null ? status : status & eliminatorStatus;
                    continue;
                }
                if (!DeviceType.CLIENT.getValue().equals(statusRecordPO.getType())) continue;
                clientStatus = clientStatus == null ? status : status & clientStatus;
            }
            ClientStatusDTO clientStatusDTO = new ClientStatusDTO();
            clientStatusDTO.setLineNo(line.getLineNo());
            clientStatusDTO.setFaceNo(line.getFaceNo());
            clientStatusDTO.setLineId(line.getId());
            clientStatusDTO.setOrder(line.getOrder());
            clientStatusDTO.setCameraStatus(Boolean.TRUE.equals(cameraStatus) ? DeviceStatus.ONLINE.getValue() : DeviceStatus.OUTLINE.getValue());
            clientStatusDTO.setEliminatorStatus(Boolean.TRUE.equals(eliminatorStatus) ? DeviceStatus.ONLINE.getValue() : DeviceStatus.OUTLINE.getValue());
            clientStatusDTO.setClientStatus(Boolean.TRUE.equals(clientStatus) ? DeviceStatus.ONLINE.getValue() : DeviceStatus.OUTLINE.getValue());
            clientStatusDTOList.add(clientStatusDTO);
        }
        return clientStatusDTOList;
    }
}
