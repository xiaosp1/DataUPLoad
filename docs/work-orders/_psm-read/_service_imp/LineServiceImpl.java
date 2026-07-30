/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.hutool.core.bean.BeanUtil
 *  cn.hutool.core.collection.CollectionUtil
 *  cn.hutool.core.date.LocalDateTimeUtil
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
 *  com.baomidou.mybatisplus.core.metadata.IPage
 *  com.baomidou.mybatisplus.core.toolkit.CollectionUtils
 *  com.baomidou.mybatisplus.core.toolkit.Wrappers
 *  com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
 *  com.google.common.collect.Maps
 *  com.hikrobotics.solution.common.constants.CommonMethod
 *  com.hikrobotics.solution.framework.common.base.BaseResult
 *  com.hikrobotics.solution.framework.common.query.PageQuery
 *  com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler
 *  com.hikrobotics.solution.framework.util.HikDateUtil
 *  com.hikrobotics.solution.module.alarm.constant.AlarmReasonEnum
 *  com.hikrobotics.solution.module.alarm.dto.AlarmCountDTO
 *  com.hikrobotics.solution.module.alarm.mapper.AlarmRecordDAO
 *  com.hikrobotics.solution.module.alarm.service.imp.AlarmRecordServiceImpl
 *  com.hikrobotics.solution.module.defect.model.LineDefectTypePO
 *  com.hikrobotics.solution.module.defect.service.ILineDefectTypeService
 *  com.hikrobotics.solution.module.detect.enums.DeviceStatus
 *  com.hikrobotics.solution.module.detect.mapper.DefectDayRecordDAO
 *  com.hikrobotics.solution.module.detect.mapper.LineDayRecordDAO
 *  com.hikrobotics.solution.module.detect.mapper.StatusRecordDAO
 *  com.hikrobotics.solution.module.detect.model.StatusRecordPO
 *  com.hikrobotics.solution.module.detect.service.IDefectDayRecordService
 *  com.hikrobotics.solution.module.detect.service.IStatusRecordService
 *  com.hikrobotics.solution.module.detect.util.TimeRange
 *  com.hikrobotics.solution.module.detect.util.TimeRange$TimePattern
 *  com.hikrobotics.solution.module.line.constant.PlanStatusEnum
 *  com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO
 *  com.hikrobotics.solution.module.line.dto.DefectCountDTO
 *  com.hikrobotics.solution.module.line.dto.DefectCountDisPlayDTO
 *  com.hikrobotics.solution.module.line.dto.LineBodyDTO
 *  com.hikrobotics.solution.module.line.dto.LineCountDTO
 *  com.hikrobotics.solution.module.line.dto.LinePanelDTO
 *  com.hikrobotics.solution.module.line.dto.LinePanelQueryDTO
 *  com.hikrobotics.solution.module.line.dto.LinePlanBindDTO
 *  com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO
 *  com.hikrobotics.solution.module.line.dto.LineTreeItemDTO
 *  com.hikrobotics.solution.module.line.dto.LineUpdateDTO
 *  com.hikrobotics.solution.module.line.dto.ToDayCountDTO
 *  com.hikrobotics.solution.module.line.mapper.LineDAO
 *  com.hikrobotics.solution.module.line.mapper.PlanToLineDAO
 *  com.hikrobotics.solution.module.line.model.LinePO
 *  com.hikrobotics.solution.module.line.model.PlanToLinePO
 *  com.hikrobotics.solution.module.line.service.ILineOrderService
 *  com.hikrobotics.solution.module.line.service.ILineService
 *  com.hikrobotics.solution.module.line.service.imp.LineServiceImpl
 *  com.hikrobotics.solution.module.line.service.imp.PlanToLineService
 *  jakarta.annotation.PostConstruct
 *  org.assertj.core.util.Lists
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.hikrobotics.solution.module.line.service.imp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Maps;
import com.hikrobotics.solution.common.constants.CommonMethod;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.alarm.constant.AlarmReasonEnum;
import com.hikrobotics.solution.module.alarm.dto.AlarmCountDTO;
import com.hikrobotics.solution.module.alarm.mapper.AlarmRecordDAO;
import com.hikrobotics.solution.module.alarm.service.imp.AlarmRecordServiceImpl;
import com.hikrobotics.solution.module.defect.model.LineDefectTypePO;
import com.hikrobotics.solution.module.defect.service.ILineDefectTypeService;
import com.hikrobotics.solution.module.detect.enums.DeviceStatus;
import com.hikrobotics.solution.module.detect.mapper.DefectDayRecordDAO;
import com.hikrobotics.solution.module.detect.mapper.LineDayRecordDAO;
import com.hikrobotics.solution.module.detect.mapper.StatusRecordDAO;
import com.hikrobotics.solution.module.detect.model.StatusRecordPO;
import com.hikrobotics.solution.module.detect.service.IDefectDayRecordService;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import com.hikrobotics.solution.module.detect.util.TimeRange;
import com.hikrobotics.solution.module.line.constant.PlanStatusEnum;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import com.hikrobotics.solution.module.line.dto.DefectCountDisPlayDTO;
import com.hikrobotics.solution.module.line.dto.LineBodyDTO;
import com.hikrobotics.solution.module.line.dto.LineCountDTO;
import com.hikrobotics.solution.module.line.dto.LinePanelDTO;
import com.hikrobotics.solution.module.line.dto.LinePanelQueryDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanBindDTO;
import com.hikrobotics.solution.module.line.dto.LinePlanSwitchDTO;
import com.hikrobotics.solution.module.line.dto.LineTreeItemDTO;
import com.hikrobotics.solution.module.line.dto.LineUpdateDTO;
import com.hikrobotics.solution.module.line.dto.ToDayCountDTO;
import com.hikrobotics.solution.module.line.mapper.LineDAO;
import com.hikrobotics.solution.module.line.mapper.PlanToLineDAO;
import com.hikrobotics.solution.module.line.model.LinePO;
import com.hikrobotics.solution.module.line.model.PlanToLinePO;
import com.hikrobotics.solution.module.line.service.ILineOrderService;
import com.hikrobotics.solution.module.line.service.ILineService;
import com.hikrobotics.solution.module.line.service.imp.PlanToLineService;
import jakarta.annotation.PostConstruct;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LineServiceImpl
extends ServiceImpl<LineDAO, LinePO>
implements ILineService {
    @Autowired
    private ILineDefectTypeService lineDefectTypeService;
    @Autowired
    private ILineOrderService lineOrderService;
    @Autowired
    private LineDAO lineDAO;
    @Autowired
    private PlanToLineService planToLineService;
    @Autowired
    private PlanToLineDAO planToLineDAO;
    @Autowired
    private WebSocketHandler webSocketHandler;
    @Autowired
    private IStatusRecordService iStatusRecordService;
    @Autowired
    private StatusRecordDAO statusRecordDAO;
    @Autowired
    private AlarmRecordServiceImpl alarmRecordService;
    @Autowired
    private LineDayRecordDAO lineDayRecordDAO;
    @Autowired
    private DefectDayRecordDAO defectDayRecordDAO;
    @Autowired
    private AlarmRecordDAO alarmRecordDAO;
    @Autowired
    private IDefectDayRecordService defectDayRecordService;

    public BaseResult listAll(PageQuery pageQuery) {
        if (pageQuery.isPaged()) {
            return BaseResult.build().data((Object)this.lineDAO.listAll((IPage)pageQuery.getPage()));
        }
        return BaseResult.build().data((Object)this.lineDAO.listAll());
    }

    @PostConstruct
    public void init() {
        List<Integer> lineIds;
        if (this.lineOrderService.count() == 0L && CollectionUtils.isNotEmpty(lineIds = this.list().stream().map(LinePO::getId).toList())) {
            this.lineOrderService.addLineOrder(lineIds);
        }
    }

    @Transactional(rollbackFor={Exception.class})
    public BaseResult add(LineBodyDTO lineDTO) {
        LambdaQueryWrapper lambdaQuery = (LambdaQueryWrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(LinePO::getLineNo, (Object)lineDTO.getLineNo())).eq(LinePO::getFaceNo, (Object)lineDTO.getFaceNo());
        if (this.count((Wrapper)lambdaQuery) > 0L) {
            return BaseResult.build().error("20202").log("add line failed, lineNo+faceNO dupicated.", lineDTO.toString());
        }
        LinePO lineData = (LinePO)BeanUtil.copyProperties((Object)lineDTO, LinePO.class, (String[])new String[0]);
        lineData.setClientNo(lineData.getLineNo() + "-" + lineData.getFaceNo());
        this.save((Object)lineData);
        this.lineOrderService.addLineOrder((List)Lists.newArrayList((Object[])new Integer[]{lineData.getId()}));
        return BaseResult.build();
    }

    public BaseResult modify(LineUpdateDTO lineUpdateDTO) {
        LinePO line = (LinePO)this.getById((Serializable)lineUpdateDTO.getId());
        if (line == null) {
            return BaseResult.build().error();
        }
        LambdaQueryWrapper lambdaQuery = (LambdaQueryWrapper)((LambdaQueryWrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(LinePO::getLineNo, (Object)lineUpdateDTO.getLineNo())).eq(LinePO::getFaceNo, (Object)lineUpdateDTO.getFaceNo())).ne(LinePO::getId, (Object)line.getId());
        if (this.count((Wrapper)lambdaQuery) > 0L) {
            return BaseResult.build().error("20202").log("update line failed, ineNo+faceNO dupicated.", lineUpdateDTO.toString());
        }
        LinePO lineData = (LinePO)BeanUtil.copyProperties((Object)lineUpdateDTO, LinePO.class, (String[])new String[0]);
        lineData.setClientNo(lineData.getLineNo() + "-" + lineData.getFaceNo());
        this.updateById((Object)lineData);
        return BaseResult.build();
    }

    @Transactional(rollbackFor={Exception.class})
    public BaseResult delete(Integer id) {
        LinePO line = (LinePO)this.getById((Serializable)id);
        if (line == null) {
            return BaseResult.build().error("20204").log("delete line failed, line not find.", String.valueOf(id));
        }
        StatusRecordPO clientStatusData = this.iStatusRecordService.searchClientStatus(line.getLineNo(), line.getFaceNo());
        if (clientStatusData != null && clientStatusData.getStatus().equals(DeviceStatus.ONLINE.getValue())) {
            return BaseResult.build().error("20208").log("delete line failed, client is online,line id.", String.valueOf(id));
        }
        this.removeById((Serializable)id);
        if (clientStatusData != null) {
            this.statusRecordDAO.deleteById((Serializable)clientStatusData.getId());
            this.alarmRecordService.dealClientAlarm(line.getLineNo(), line.getFaceNo(), AlarmReasonEnum.DISCONNECT.getValue());
        }
        this.lineOrderService.removeByLineId(line.getId());
        return BaseResult.build();
    }

    public BaseResult bindPlan(LinePlanBindDTO linePlanBindDTO) {
        Integer currentPlanId;
        Integer lineId = linePlanBindDTO.getLineId();
        List planIds = linePlanBindDTO.getPlanIds();
        LambdaQueryWrapper lambdaQuery = (LambdaQueryWrapper)Wrappers.lambdaQuery().eq(PlanToLinePO::getLineId, (Object)lineId);
        List currentRelatedPlans = this.planToLineDAO.selectList((Wrapper)lambdaQuery);
        if (CollectionUtil.isNotEmpty((Collection)currentRelatedPlans)) {
            List currentRelatedPlanIds = currentRelatedPlans.stream().map(PlanToLinePO::getPlanId).collect(Collectors.toList());
            Collections.sort(planIds);
            Collections.sort(currentRelatedPlanIds);
            if (planIds.equals(currentRelatedPlanIds)) {
                return BaseResult.build();
            }
        }
        PlanToLinePO currentRunPlan = null;
        for (PlanToLinePO currentRelatedPlan : currentRelatedPlans) {
            if (!currentRelatedPlan.getStatus().equals(PlanStatusEnum.ENABLE.getValue())) continue;
            currentRunPlan = currentRelatedPlan;
            break;
        }
        if (currentRunPlan != null) {
            currentPlanId = currentRunPlan.getPlanId();
            if (!planIds.contains(currentPlanId)) {
                return BaseResult.build().error("20205").log("bind plan failed,current run plan can't be cancel dispatch.", String.valueOf(currentPlanId));
            }
        } else {
            currentPlanId = null;
        }
        lambdaQuery = (LambdaQueryWrapper)Wrappers.lambdaQuery().eq(PlanToLinePO::getLineId, (Object)lineId);
        this.planToLineDAO.delete((Wrapper)lambdaQuery);
        ArrayList newPlanToLine = new ArrayList();
        planIds.forEach(id -> {
            PlanToLinePO planToLine = new PlanToLinePO();
            planToLine.setLineId(lineId);
            planToLine.setPlanId(id);
            if (id.equals(currentPlanId)) {
                planToLine.setStatus(PlanStatusEnum.ENABLE.getValue());
            }
            newPlanToLine.add(planToLine);
        });
        if (CollectionUtil.isNotEmpty(newPlanToLine)) {
            this.planToLineService.saveBatch(newPlanToLine);
        }
        CommonMethod.sendPlanChange((WebSocketHandler)this.webSocketHandler, (String)linePlanBindDTO.getClientNo());
        return BaseResult.build();
    }

    public BaseResult switchPlan(LinePlanSwitchDTO linePlanSwitchDTO) {
        LambdaQueryWrapper lambdaquery = (LambdaQueryWrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(PlanToLinePO::getLineId, (Object)linePlanSwitchDTO.getLineId())).eq(PlanToLinePO::getStatus, (Object)1);
        PlanToLinePO currentRunPlan = (PlanToLinePO)this.planToLineDAO.selectOne((Wrapper)lambdaquery);
        Integer switchPlanId = linePlanSwitchDTO.getPlanId();
        lambdaquery = (LambdaQueryWrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(PlanToLinePO::getLineId, (Object)linePlanSwitchDTO.getLineId())).eq(PlanToLinePO::getPlanId, (Object)linePlanSwitchDTO.getPlanId());
        PlanToLinePO switchPlan = (PlanToLinePO)this.planToLineDAO.selectOne((Wrapper)lambdaquery);
        if (switchPlan == null) {
            return BaseResult.build().error("20207").log("switch plan faild,switch plan is not dipatched to line.", linePlanSwitchDTO.toString());
        }
        if (currentRunPlan != null) {
            if (currentRunPlan.getPlanId().equals(switchPlanId)) {
                return BaseResult.build();
            }
            currentRunPlan.setStatus(PlanStatusEnum.DISABLE.getValue());
            this.planToLineDAO.updateById((Object)currentRunPlan);
        }
        switchPlan.setStatus(PlanStatusEnum.ENABLE.getValue());
        this.planToLineDAO.updateById((Object)switchPlan);
        CommonMethod.sendPlanChange((WebSocketHandler)this.webSocketHandler, (String)linePlanSwitchDTO.getClientNo());
        return BaseResult.build();
    }

    public BaseResult planPanel(LinePanelQueryDTO form) {
        LinePO line = (LinePO)this.getById((Serializable)form.getFaceId());
        if (null == line) {
            return BaseResult.build().error("20204");
        }
        LinePanelDTO panel = new LinePanelDTO();
        LocalDateTime localStart = LocalDateTimeUtil.beginOfDay((LocalDateTime)form.localStartTime());
        LocalDateTime localEnd = LocalDateTimeUtil.endOfDay((LocalDateTime)form.localEndTime());
        String start = HikDateUtil.formatLocalDate((LocalDateTime)localStart);
        String end = HikDateUtil.formatLocalDate((LocalDateTime)localEnd);
        HashMap sortLineProductionByTime = Maps.newHashMap();
        this.lineDayRecordDAO.selectLineCountDay(start, end, line.getLineNo(), line.getFaceNo()).forEach(record -> {
            String time = record.getTime().substring(0, 10);
            LineCountDTO old = sortLineProductionByTime.getOrDefault(time, new LineCountDTO().setTime(time));
            old.setErrorCount(Integer.valueOf(old.getErrorCount() + record.getErrorCount())).setCount(Integer.valueOf(old.getCount() + record.getCount())).setTime(time);
            sortLineProductionByTime.put(time, old);
        });
        TimeRange range = new TimeRange(localStart, localEnd, TimeRange.TimePattern.YYYY_MM_DD);
        while (range.hasNext()) {
            String time = HikDateUtil.formatLocalDate((LocalDateTime)range.next(), (String)range.getPattern());
            if (sortLineProductionByTime.containsKey(time)) continue;
            LineCountDTO count2 = new LineCountDTO().setCount(Integer.valueOf(0)).setErrorCount(Integer.valueOf(0)).setTime(time).calPercentage();
            sortLineProductionByTime.put(time, count2);
        }
        panel.setLineCountDTOS(sortLineProductionByTime.values().stream().map(LineCountDTO::calPercentage).sorted(Comparator.comparing(LineCountDTO::getTime)).toList());
        List<String> enableCountDefects = this.lineDefectTypeService.listIfShowEnable(line.getLineNo(), line.getFaceNo()).stream().map(LineDefectTypePO::getName).toList();
        if (CollectionUtils.isNotEmpty(enableCountDefects)) {
            HashMap sortByTypeAndTime = Maps.newHashMap();
            this.defectDayRecordDAO.selectDefectCountDay(start, end, line.getLineNo(), line.getFaceNo(), enableCountDefects).forEach(count -> {
                Map sortByTime = sortByTypeAndTime.getOrDefault(count.getType(), Maps.newHashMap());
                String time = count.getTime().substring(0, 10);
                DefectCountDTO temp = sortByTime.getOrDefault(time, new DefectCountDTO().setCount(Integer.valueOf(0)).setTime(time).setType(count.getType()));
                temp.setCount(Integer.valueOf(temp.getCount() + count.getCount()));
                sortByTime.put(time, temp);
                sortByTypeAndTime.put(count.getType(), sortByTime);
            });
            enableCountDefects.forEach(defect -> {
                range.init();
                DefectCountDisPlayDTO dto = new DefectCountDisPlayDTO().setType(defect);
                Map sortByTime = sortByTypeAndTime.getOrDefault(defect, Maps.newHashMap());
                while (range.hasNext()) {
                    String time = HikDateUtil.formatLocalDate((LocalDateTime)range.next(), (String)HikDateUtil.simplePattern);
                    dto.getTime().add(time);
                    DefectCountDTO count = sortByTime.getOrDefault(time, new DefectCountDTO());
                    dto.getCount().add(count.getCount());
                }
                panel.getDefectCountDTOS().add(dto);
            });
        }
        HashMap sortAlarmByTime = Maps.newHashMap();
        this.alarmRecordDAO.selectAlarmCountDay(start, end, line.getLineNo(), line.getFaceNo()).forEach(alarm -> sortAlarmByTime.put(alarm.getCountTime(), alarm));
        range.init();
        while (range.hasNext()) {
            String time = HikDateUtil.formatLocalDate((LocalDateTime)range.next(), (String)HikDateUtil.simplePattern);
            if (sortAlarmByTime.containsKey(time)) continue;
            sortAlarmByTime.put(time, new AlarmCountDTO().setCount(Integer.valueOf(0)).setCountTime(time));
        }
        panel.setAlarmCountDTOS(sortAlarmByTime.values().stream().sorted(Comparator.comparing(AlarmCountDTO::getCountTime)).toList());
        List statusRecordPOS = this.statusRecordDAO.selectList((Wrapper)((LambdaQueryWrapper)((LambdaQueryWrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(StatusRecordPO::getLineNo, (Object)line.getLineNo())).eq(StatusRecordPO::getFaceNo, (Object)line.getFaceNo())).orderByDesc(StatusRecordPO::getStatus)).orderByDesc(StatusRecordPO::getType));
        panel.setStatusRecordPOS(statusRecordPOS);
        ToDayCountDTO toDayCountDTO = this.lineDayRecordDAO.selectRightAndError(line.getLineNo(), line.getFaceNo());
        if (toDayCountDTO == null) {
            toDayCountDTO = new ToDayCountDTO();
        }
        toDayCountDTO.calPercentage(Integer.valueOf(toDayCountDTO.getRightCount()), Integer.valueOf(toDayCountDTO.getErrorCount()));
        panel.setToDayCountDTO(toDayCountDTO);
        return BaseResult.build().data((Object)panel);
    }

    public BaseResult planStatus(LinePanelQueryDTO linePanelQueryDTO) {
        LinePO line = (LinePO)this.lineDAO.selectById((Serializable)linePanelQueryDTO.getFaceId());
        if (null != line) {
            List status = this.statusRecordDAO.selectList((Wrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(StatusRecordPO::getLineNo, (Object)line.getLineNo())).eq(StatusRecordPO::getFaceNo, (Object)line.getFaceNo()));
            return BaseResult.build().data((Object)status);
        }
        return BaseResult.build().error("20204");
    }

    public BaseResult lineGroup() {
        List linePOS = this.lineDAO.selectList((Wrapper)new QueryWrapper().select(new String[]{"distinct NAME,line_no"}));
        return BaseResult.build().data((Object)linePOS);
    }

    public List<LinePO> listLine() {
        return this.lineDAO.selectLine();
    }

    public BaseResult chgLineOrder(List<ChgLineOrderDTO> lineOrders) {
        if ((long)lineOrders.size() != this.count()) {
            return BaseResult.build().error("20209");
        }
        if (!this.lineOrderService.modLineOrder(lineOrders).booleanValue()) {
            return BaseResult.build().error("20210");
        }
        return BaseResult.build().ok();
    }

    public LinePO getByLineNoAndFaceNo(String lineNo, String faceNo) {
        return (LinePO)this.getOne((Wrapper)((LambdaQueryWrapper)Wrappers.lambdaQuery().eq(LinePO::getLineNo, (Object)lineNo)).eq(LinePO::getFaceNo, (Object)faceNo));
    }

    public BaseResult handleLineTreeSearch() {
        HashMap sortByLineNo = Maps.newHashMap();
        this.list().forEach(line -> {
            LineTreeItemDTO tree = sortByLineNo.getOrDefault(line.getLineNo(), new LineTreeItemDTO(line));
            tree.getChilds().add(new LineTreeItemDTO(line).setLineNo(line.getFaceNo()));
            sortByLineNo.put(line.getLineNo(), tree);
        });
        List<LineTreeItemDTO> data = sortByLineNo.values().stream().sorted(Comparator.comparing(LineTreeItemDTO::getId)).toList();
        return BaseResult.build().data(data);
    }

    public List<LinePO> listByLineNo(List<String> lineNos) {
        if (CollectionUtil.isNotEmpty(lineNos)) {
            return this.list((Wrapper)Wrappers.lambdaQuery().in(LinePO::getLineNo, lineNos));
        }
        return Lists.newArrayList();
    }
}

