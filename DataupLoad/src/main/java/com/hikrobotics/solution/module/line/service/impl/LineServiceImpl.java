package com.hikrobotics.solution.module.line.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Maps;
import com.hikrobotics.solution.common.constants.CommonMethod;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.framework.component.ws.handler.WebSocketHandler;
import com.hikrobotics.solution.framework.util.HikDateUtil;
import com.hikrobotics.solution.module.alarm.constant.AlarmReasonEnum;
import com.hikrobotics.solution.module.alarm.dto.AlarmCountDTO;
import com.hikrobotics.solution.module.alarm.mapper.AlarmRecordMapper;
import com.hikrobotics.solution.module.alarm.service.impl.AlarmRecordServiceImpl;
import com.hikrobotics.solution.module.defect.service.ILineDefectTypeService;
import com.hikrobotics.solution.module.detect.enums.DeviceStatus;
import com.hikrobotics.solution.module.detect.entity.StatusRecord;
import com.hikrobotics.solution.module.detect.mapper.DefectDayRecordMapper;
import com.hikrobotics.solution.module.detect.mapper.StatusRecordMapper;
import com.hikrobotics.solution.module.detect.service.IDefectDayRecordService;
import com.hikrobotics.solution.module.detect.service.IStatusRecordService;
import com.hikrobotics.solution.module.line.constant.PlanStatusEnum;
import com.hikrobotics.solution.module.line.dto.ChgLineOrderDTO;
import com.hikrobotics.solution.module.line.dto.ClientPlanResultDTO;
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
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.entity.LineDefectType;
import com.hikrobotics.solution.module.line.entity.PlanToLine;
import com.hikrobotics.solution.module.line.mapper.LineDayRecordMapper;
import com.hikrobotics.solution.module.line.mapper.LineMapper;
import com.hikrobotics.solution.module.line.mapper.PlanMapper;
import com.hikrobotics.solution.module.line.mapper.PlanToLineMapper;
import com.hikrobotics.solution.module.line.service.ILineOrderService;
import com.hikrobotics.solution.module.line.service.ILineService;
// DataupLoad 没有 PSM {@code PlanToLineService} 接口（PSM 本身就是 @Service 类），
// 这里直接用 DataupLoad 已有的 {@code PlanToLineServiceImpl} 作 bean 注入。
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 线体服务实现（W-B03 + W-B05 + W-LIN-01）。
 *
 * <p>W-LIN-01 1:1 对齐 PSM 反编译 LineServiceImpl（包名 {@code module.line.service.imp}，
 * 注意 PSM 用 {@code imp} 不是 {@code impl}）的 8 个核心业务方法：
 * <ul>
 *   <li>{@link #add(LineBodyDTO)} 新增产线</li>
 *   <li>{@link #modify(LineUpdateDTO)} 修改产线</li>
 *   <li>{@link #delete(Integer)} 删除产线</li>
 *   <li>{@link #bindPlan(LinePlanBindDTO)} 配方分发</li>
 *   <li>{@link #switchPlan(LinePlanSwitchDTO)} 配方切换</li>
 *   <li>{@link #planPanel(LinePanelQueryDTO)} 大屏面板聚合</li>
 *   <li>{@link #planStatus(LinePanelQueryDTO)} 大屏实时状态</li>
 *   <li>{@link #init()} {@code @PostConstruct} 初始化 line_order</li>
 * </ul>
 *
 * <p>DataupLoad vs PSM 实体差异：
 * <ul>
 *   <li>PSM LinePO → DataupLoad {@link Line}（字段一致，TableName 都是 {@code line}；W-CLEAN-03 起 LinePO 已删除）</li>
 *   <li>PSM PlanToLinePO → DataupLoad {@link PlanToLine}</li>
 *   <li>PSM StatusRecordPO → DataupLoad {@link StatusRecord}</li>
 *   <li>PSM LineDefectTypePO → DataupLoad {@link LineDefectType}（包名 line/entity）</li>
 *   <li>PSM LineDAO / PlanToLineDAO / StatusRecordDAO → DataupLoad Mapper 名称</li>
 *   <li>PSM TimeRange（detect/util/TimeRange.java）→ 用 hutool LocalDateTimeUtil 内联 day-step 循环
 *       （DataupLoad 框架 TimeRangeUtil API 不一致，按 PSM 语义改写）</li>
 * </ul>
 * </p>
 *
 * <p>注：lineGroup / chgLineOrder / handleLineTreeSearch / listByLineNo(List) 等
 * PSM 接口方法不在本工单（W-LIN-01）8 个核心业务方法范围内，未实现。</p>
 */
@Service
public class LineServiceImpl extends ServiceImpl<LineMapper, Line> implements ILineService {

    // ============================================================
    // 13 个 PSM 1:1 @Autowired 注入（W-LIN-01 任务要求 + W-LIN-06 追加 1 个）
    // 原始 PSM: ILineDefectTypeService / ILineOrderService / LineDAO / PlanToLineService /
    //          PlanToLineDAO / WebSocketHandler / IStatusRecordService / StatusRecordDAO /
    //          AlarmRecordServiceImpl / LineDayRecordDAO / DefectDayRecordDAO /
    //          AlarmRecordDAO / IDefectDayRecordService
    // DataupLoad 改造：
    //   - LineDAO → LineMapper (BaseMapper<Line>)
    //   - PlanToLineDAO → PlanToLineMapper
    //   - StatusRecordDAO → StatusRecordMapper
    //   - LineDayRecordDAO → LineDayRecordMapper
    //   - DefectDayRecordDAO → DefectDayRecordMapper
    //   - AlarmRecordDAO → AlarmRecordMapper
    //   - AlarmRecordServiceImpl 包名从 module.alarm.service.imp 改为 .impl
    //   - 去掉 LineDAO 注入（PSM 冗余，DataupLoad 统一用 baseMapper）
    //   - 保留 PlanToLineDAO 注入的语义，改名为 PlanToLineMapper
    //   - 追加 PlanMapper（W-LIN-06）：用于 planOrderDtos 调用 planMapper.selectClientPlan(lineNo, faceNo)
    //     替代 PSM PlanServiceImpl.clientPlan 的 planDAO.selectClientPlan 语义
    // ============================================================

    @Autowired
    private ILineDefectTypeService lineDefectTypeService;

    @Autowired
    private ILineOrderService lineOrderService;

    @Autowired
    private PlanToLineServiceImpl planToLineService;

    @Autowired
    private PlanToLineMapper planToLineDAO;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private IStatusRecordService iStatusRecordService;

    @Autowired
    private StatusRecordMapper statusRecordDAO;

    @Autowired
    private AlarmRecordServiceImpl alarmRecordService;

    @Autowired
    private LineDayRecordMapper lineDayRecordDAO;

    @Autowired
    private DefectDayRecordMapper defectDayRecordDAO;

    @Autowired
    private AlarmRecordMapper alarmRecordDAO;

    @Autowired
    private IDefectDayRecordService defectDayRecordService;

    @Autowired
    private PlanMapper planMapper;

    // ============================================================
    // W-B03 既有方法（保留）
    // ============================================================

    @Override
    public BaseResult listAll(PageQuery pageQuery) {
        if (pageQuery != null && pageQuery.isPaged()) {
            return BaseResult.build().data(this.baseMapper.selectPage(pageQuery.getPage(), null));
        }
        return BaseResult.build().data(this.baseMapper.selectList(Wrappers.lambdaQuery(Line.class).orderByAsc(Line::getId)));
    }

    @Override
    public List<Line> listByLineNo(String lineNo) {
        if (lineNo == null || lineNo.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Line> wrapper = Wrappers.<Line>lambdaQuery()
            .eq(Line::getLineNo, lineNo)
            .orderByAsc(Line::getId);
        return this.list(wrapper);
    }

    @Override
    public Line getByLineNo(String lineNo) {
        List<Line> lines = listByLineNo(lineNo);
        return lines.isEmpty() ? null : lines.get(0);
    }

    @Override
    public Line getByLineNoAndFaceNo(String lineNo, String faceNo) {
        if (lineNo == null || lineNo.isEmpty() || faceNo == null || faceNo.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Line> wrapper = Wrappers.<Line>lambdaQuery()
            .eq(Line::getLineNo, lineNo)
            .eq(Line::getFaceNo, faceNo)
            .orderByAsc(Line::getId)
            .last("limit 1");
        return this.getOne(wrapper);
    }

    @Override
    public List<Line> listLine() {
        return this.list(Wrappers.<Line>lambdaQuery().orderByAsc(Line::getId));
    }

    // ============================================================
    // W-LIN-01 8 个核心业务方法
    // ============================================================

    /**
     * W-LIN-01 #8：{@code @PostConstruct} 初始化 line_order。
     *
     * <p>1:1 抄 PSM：若 {@code line_order} 表为空且 {@code line} 表有数据，则按 id 升序填充 line_order。</p>
     */
    @PostConstruct
    public void init() {
        List<Integer> lineIds;
        if (this.lineOrderService.count() == 0L
            && CollectionUtils.isNotEmpty(lineIds = this.list().stream().map(Line::getId).toList())) {
            this.lineOrderService.addLineOrder(lineIds);
        }
    }

    /**
     * W-LIN-01 #1：新增产线（PSM 1:1）。
     *
     * <p>逻辑：</p>
     * <ol>
     *   <li>检查 (lineNo, faceNo) 是否已存在 → 重复返回错误 20202</li>
     *   <li>DTO → PO 拷贝；自动设置 {@code clientNo = lineNo + "-" + faceNo}</li>
     *   <li>保存 line 行</li>
     *   <li>同步加入 line_order 表</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BaseResult add(LineBodyDTO lineDTO) {
        LambdaQueryWrapper<Line> lambdaQuery = Wrappers.<Line>lambdaQuery()
            .eq(Line::getLineNo, lineDTO.getLineNo())
            .eq(Line::getFaceNo, lineDTO.getFaceNo());
        if (this.count(lambdaQuery) > 0L) {
            return BaseResult.build().error("20202").log("add line failed, lineNo+faceNO dupicated.", lineDTO.toString());
        }
        Line lineData = BeanUtil.copyProperties(lineDTO, Line.class);
        lineData.setClientNo(lineData.getLineNo() + "-" + lineData.getFaceNo());
        this.save(lineData);
        this.lineOrderService.addLineOrder(Lists.newArrayList(lineData.getId()));
        return BaseResult.build();
    }

    /**
     * W-LIN-01 #2：修改产线（PSM 1:1）。
     *
     * <p>逻辑：</p>
     * <ol>
     *   <li>按 id 查询 line → 不存在返回错误</li>
     *   <li>检查 (lineNo, faceNo) 是否被其它记录占用 → 占用返回错误 20202</li>
     *   <li>DTO → PO 拷贝（id 已含）；自动设置 {@code clientNo = lineNo + "-" + faceNo}</li>
     *   <li>按 id 更新</li>
     * </ol>
     */
    @Override
    public BaseResult modify(LineUpdateDTO lineUpdateDTO) {
        Line line = this.getById(lineUpdateDTO.getId());
        if (line == null) {
            return BaseResult.build().error();
        }
        LambdaQueryWrapper<Line> lambdaQuery = Wrappers.<Line>lambdaQuery()
            .eq(Line::getLineNo, lineUpdateDTO.getLineNo())
            .eq(Line::getFaceNo, lineUpdateDTO.getFaceNo())
            .ne(Line::getId, line.getId());
        if (this.count(lambdaQuery) > 0L) {
            return BaseResult.build().error("20202").log("update line failed, lineNo+faceNO dupicated.", lineUpdateDTO.toString());
        }
        Line lineData = BeanUtil.copyProperties(lineUpdateDTO, Line.class);
        lineData.setClientNo(lineData.getLineNo() + "-" + lineData.getFaceNo());
        this.updateById(lineData);
        return BaseResult.build();
    }

    /**
     * W-LIN-01 #3：删除产线（PSM 1:1）。
     *
     * <p>逻辑：</p>
     * <ol>
     *   <li>按 id 查询 line → 不存在返回错误 20204</li>
     *   <li>查询 (lineNo, faceNo) 的 CLIENT 状态记录</li>
     *   <li>若客户端 ONLINE → 拒绝删除（错误 20208）</li>
     *   <li>删除 line 行</li>
     *   <li>若存在 status_record，删除对应状态记录并触发 {@code alarmRecordService.dealClientAlarm}
     *       清理 UNSOLVED 掉线告警</li>
     *   <li>从 line_order 表移除该 lineId</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BaseResult delete(Integer id) {
        Line line = this.getById(id);
        if (line == null) {
            return BaseResult.build().error("20204").log("delete line failed, line not find.", String.valueOf(id));
        }
        StatusRecord clientStatusData = this.iStatusRecordService.searchClientStatus(line.getLineNo(), line.getFaceNo());
        if (clientStatusData != null && clientStatusData.getStatus().equals(DeviceStatus.ONLINE.getValue())) {
            return BaseResult.build().error("20208").log("delete line failed, client is online,line id.", String.valueOf(id));
        }
        this.removeById(id);
        if (clientStatusData != null) {
            this.statusRecordDAO.deleteById(clientStatusData.getId());
            this.alarmRecordService.dealClientAlarm(line.getLineNo(), line.getFaceNo(), AlarmReasonEnum.DISCONNECT.getValue());
        }
        this.lineOrderService.removeByLineId(line.getId());
        return BaseResult.build();
    }

    /**
     * W-LIN-01 #4：配方分发（PSM 1:1）。
     *
     * <p>逻辑：</p>
     * <ol>
     *   <li>查询当前 line 已分发的 plan 列表</li>
     *   <li>若新 planIds 排序后等于当前 → no-op 返回 ok</li>
     *   <li>若存在当前运行中的 plan（status=ENABLE）且新列表不包含 → 拒绝（错误 20205）</li>
     *   <li>删除当前 line 所有 plan_to_line 行</li>
     *   <li>批量插入新 plan_to_line 行（若新 id == 当前运行 planId 则 status=ENABLE）</li>
     *   <li>WebSocket 广播 changePlan</li>
     * </ol>
     */
    @Override
    public BaseResult bindPlan(LinePlanBindDTO linePlanBindDTO) {
        Integer currentPlanId;
        Integer lineId = linePlanBindDTO.getLineId();
        List<Integer> planIds = linePlanBindDTO.getPlanIds();
        LambdaQueryWrapper<PlanToLine> lambdaQuery = Wrappers.<PlanToLine>lambdaQuery()
            .eq(PlanToLine::getLineId, lineId);
        List<PlanToLine> currentRelatedPlans = this.planToLineDAO.selectList(lambdaQuery);
        if (CollectionUtil.isNotEmpty(currentRelatedPlans)) {
            List<Integer> currentRelatedPlanIds = currentRelatedPlans.stream()
                .map(PlanToLine::getPlanId)
                .collect(java.util.stream.Collectors.toList());
            Collections.sort(planIds);
            Collections.sort(currentRelatedPlanIds);
            if (planIds.equals(currentRelatedPlanIds)) {
                return BaseResult.build();
            }
        }
        PlanToLine currentRunPlan = null;
        for (PlanToLine currentRelatedPlan : currentRelatedPlans) {
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
        lambdaQuery = Wrappers.<PlanToLine>lambdaQuery().eq(PlanToLine::getLineId, lineId);
        this.planToLineDAO.delete(lambdaQuery);
        ArrayList<PlanToLine> newPlanToLine = new ArrayList<>();
        planIds.forEach(id -> {
            PlanToLine planToLine = new PlanToLine();
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
        CommonMethod.sendPlanChange(this.webSocketHandler, linePlanBindDTO.getClientNo());
        return BaseResult.build();
    }

    /**
     * W-LIN-01 #5：配方切换（PSM 1:1）。
     *
     * <p>逻辑：</p>
     * <ol>
     *   <li>查询当前 line 的运行中 plan（status=1=ENABLE）</li>
     *   <li>查询目标 plan（status != 必查，无所谓）</li>
     *   <li>目标 plan 不在线 → 错误 20207</li>
     *   <li>目标 plan == 当前 plan → no-op</li>
     *   <li>当前 plan → status=DISABLE，目标 plan → status=ENABLE</li>
     *   <li>WebSocket 广播 changePlan</li>
     * </ol>
     */
    @Override
    public BaseResult switchPlan(LinePlanSwitchDTO linePlanSwitchDTO) {
        LambdaQueryWrapper<PlanToLine> lambdaquery = Wrappers.<PlanToLine>lambdaQuery()
            .eq(PlanToLine::getLineId, linePlanSwitchDTO.getLineId())
            .eq(PlanToLine::getStatus, 1);
        PlanToLine currentRunPlan = this.planToLineDAO.selectOne(lambdaquery);
        Integer switchPlanId = linePlanSwitchDTO.getPlanId();
        lambdaquery = Wrappers.<PlanToLine>lambdaQuery()
            .eq(PlanToLine::getLineId, linePlanSwitchDTO.getLineId())
            .eq(PlanToLine::getPlanId, linePlanSwitchDTO.getPlanId());
        PlanToLine switchPlan = this.planToLineDAO.selectOne(lambdaquery);
        if (switchPlan == null) {
            return BaseResult.build().error("20207").log("switch plan faild,switch plan is not dipatched to line.", linePlanSwitchDTO.toString());
        }
        if (currentRunPlan != null) {
            if (currentRunPlan.getPlanId().equals(switchPlanId)) {
                return BaseResult.build();
            }
            currentRunPlan.setStatus(PlanStatusEnum.DISABLE.getValue());
            this.planToLineDAO.updateById(currentRunPlan);
        }
        switchPlan.setStatus(PlanStatusEnum.ENABLE.getValue());
        this.planToLineDAO.updateById(switchPlan);
        CommonMethod.sendPlanChange(this.webSocketHandler, linePlanSwitchDTO.getClientNo());
        return BaseResult.build();
    }

    /**
     * W-LIN-01 #6：大屏面板聚合（PSM 1:1）。
     *
     * <p>逻辑（按 LinePanelDTO 4 个数据集合聚合）：</p>
     * <ol>
     *   <li>按 faceId 查 line → 不存在返回错误 20204</li>
     *   <li>lineCountDTOS：line_day_record.selectLineCountDay(start, end, lineNo, faceNo)
     *       按 (time) 分组累加 errorCount/count；缺失日期用 0 补齐（PSM TimeRange.YYYY_MM_DD 步进，
     *       DataupLoad 用 LocalDateTimeUtil 内联 day 循环）</li>
     *   <li>defectCountDTOS：defect_day_record.selectDefectCountDay 限定 lineDefectTypeService.listIfShowEnable
     *       返回的缺陷名，按 (type, time) 分组；再按所有日期补齐 0 槽位</li>
     *   <li>alarmCountDTOS：alarm_record.selectAlarmCountDay；缺失日期补齐 0 槽位</li>
     *   <li>statusRecordPOS：status_record 按 lineNo+faceNo 查所有设备状态（order by status desc, type desc）</li>
     *   <li>toDayCountDTO：line_day_record.selectRightAndError → 计算百分比</li>
     * </ol>
     */
    @Override
    public BaseResult planPanel(LinePanelQueryDTO form) {
        Line line = this.getById(form.getFaceId());
        if (null == line) {
            return BaseResult.build().error("20204");
        }
        LinePanelDTO panel = new LinePanelDTO();
        LocalDateTime localStart = LocalDateTimeUtil.beginOfDay(form.localStartTime());
        LocalDateTime localEnd = LocalDateTimeUtil.endOfDay(form.localEndTime());
        String start = HikDateUtil.formatLocalDate(localStart);
        String end = HikDateUtil.formatLocalDate(localEnd);

        // 1) lineCountDTOS：产量聚合 + 缺失日期补 0
        HashMap<String, LineCountDTO> sortLineProductionByTime = Maps.newHashMap();
        this.lineDayRecordDAO.selectLineCountDay(start, end, line.getLineNo(), line.getFaceNo()).forEach(record -> {
            String time = record.getTime().substring(0, 10);
            LineCountDTO old = sortLineProductionByTime.getOrDefault(time, new LineCountDTO().setTime(time));
            old.setErrorCount(old.getErrorCount() + record.getErrorCount())
               .setCount(old.getCount() + record.getCount())
               .setTime(time);
            sortLineProductionByTime.put(time, old);
        });
        LocalDateTime cursor = localStart;
        while (cursor.isBefore(localEnd) || cursor.isEqual(localEnd)) {
            String time = HikDateUtil.formatLocalDate(cursor, HikDateUtil.simplePattern);
            if (!sortLineProductionByTime.containsKey(time)) {
                LineCountDTO count = new LineCountDTO()
                    .setCount(0)
                    .setErrorCount(0)
                    .setTime(time)
                    .calPercentage();
                sortLineProductionByTime.put(time, count);
            }
            cursor = cursor.plusDays(1);
        }
        panel.setLineCountDTOS(
            sortLineProductionByTime.values().stream()
                .map(LineCountDTO::calPercentage)
                .sorted(Comparator.comparing(LineCountDTO::getTime))
                .toList());

        // 2) defectCountDTOS：缺陷按 (type, time) 分组 + 补 0 槽位
        List<String> enableCountDefects = this.lineDefectTypeService
            .listIfShowEnable(line.getLineNo(), line.getFaceNo())
            .stream()
            .map(LineDefectType::getName)
            .toList();
        if (CollectionUtils.isNotEmpty(enableCountDefects)) {
            HashMap<String, Map<String, DefectCountDTO>> sortByTypeAndTime = Maps.newHashMap();
            this.defectDayRecordDAO.selectDefectCountDay(
                    start, end, line.getLineNo(), line.getFaceNo(), enableCountDefects)
                .forEach(count -> {
                    Map<String, DefectCountDTO> sortByTime =
                        sortByTypeAndTime.getOrDefault(count.getType(), Maps.newHashMap());
                    String time = count.getTime().substring(0, 10);
                    DefectCountDTO temp = sortByTime.getOrDefault(time,
                        new DefectCountDTO().setCount(0).setTime(time).setType(count.getType()));
                    temp.setCount(temp.getCount() + count.getCount());
                    sortByTime.put(time, temp);
                    sortByTypeAndTime.put(count.getType(), sortByTime);
                });
            enableCountDefects.forEach(defect -> {
                DefectCountDisPlayDTO dto = new DefectCountDisPlayDTO().setType(defect);
                Map<String, DefectCountDTO> sortByTime =
                    sortByTypeAndTime.getOrDefault(defect, Maps.newHashMap());
                LocalDateTime cur = localStart;
                while (cur.isBefore(localEnd) || cur.isEqual(localEnd)) {
                    String time = HikDateUtil.formatLocalDate(cur, HikDateUtil.simplePattern);
                    dto.getTime().add(time);
                    DefectCountDTO cnt = sortByTime.getOrDefault(time, new DefectCountDTO());
                    dto.getCount().add(cnt.getCount());
                    cur = cur.plusDays(1);
                }
                panel.getDefectCountDTOS().add(dto);
            });
        }

        // 3) alarmCountDTOS：按天聚合 + 补 0 槽位
        HashMap<String, AlarmCountDTO> sortAlarmByTime = Maps.newHashMap();
        this.alarmRecordDAO.selectAlarmCountDay(start, end, line.getLineNo(), line.getFaceNo())
            .forEach(alarm -> sortAlarmByTime.put(alarm.getCountTime(), alarm));
        LocalDateTime alarmCursor = localStart;
        while (alarmCursor.isBefore(localEnd) || alarmCursor.isEqual(localEnd)) {
            String time = HikDateUtil.formatLocalDate(alarmCursor, HikDateUtil.simplePattern);
            if (!sortAlarmByTime.containsKey(time)) {
                sortAlarmByTime.put(time, new AlarmCountDTO().setCount(0).setCountTime(time));
            }
            alarmCursor = alarmCursor.plusDays(1);
        }
        panel.setAlarmCountDTOS(
            sortAlarmByTime.values().stream()
                .sorted(Comparator.comparing(AlarmCountDTO::getCountTime))
                .toList());

        // 4) statusRecordPOS：按 lineNo+faceNo 全设备状态（order by status desc, type desc）
        List<StatusRecord> statusRecordPOS = this.statusRecordDAO.selectList(
            Wrappers.<StatusRecord>lambdaQuery()
                .eq(StatusRecord::getLineNo, line.getLineNo())
                .eq(StatusRecord::getFaceNo, line.getFaceNo())
                .orderByDesc(StatusRecord::getStatus)
                .orderByDesc(StatusRecord::getType));
        panel.setStatusRecordPOS(statusRecordPOS);

        // 5) toDayCountDTO：当日正/次品聚合 + 计算百分比
        ToDayCountDTO toDayCountDTO = this.lineDayRecordDAO.selectRightAndError(line.getLineNo(), line.getFaceNo());
        if (toDayCountDTO == null) {
            toDayCountDTO = new ToDayCountDTO();
        }
        toDayCountDTO.calPercentage(toDayCountDTO.getRightCount(), toDayCountDTO.getErrorCount());
        panel.setToDayCountDTO(toDayCountDTO);

        return BaseResult.build().data(panel);
    }

    /**
     * W-LIN-01 #7：大屏实时状态（PSM 1:1）。
     *
     * <p>逻辑：按 faceId 查 line → 存在则返回该 line/face 下所有 status_record；不存在返回错误 20204。</p>
     */
    @Override
    public BaseResult planStatus(LinePanelQueryDTO linePanelQueryDTO) {
        Line line = this.baseMapper.selectById(linePanelQueryDTO.getFaceId());
        if (null != line) {
            List<StatusRecord> status = this.statusRecordDAO.selectList(
                Wrappers.<StatusRecord>lambdaQuery()
                    .eq(StatusRecord::getLineNo, line.getLineNo())
                    .eq(StatusRecord::getFaceNo, line.getFaceNo()));
            return BaseResult.build().data(status);
        }
        return BaseResult.build().error("20204");
    }

    // ============================================================
    // W-LIN-05 — PSM 1:1 剩余 4 个方法
    //   lineGroup / chgLineOrder / handleLineTreeSearch / listByLineNo(List)
    // ============================================================

    /**
     * W-LIN-05 #1：产线分组查询（PSM 1:1）。
     *
     * <p>对应 PSM LineServiceImpl.lineGroup()。实现：
     * {@code baseMapper.selectList(new QueryWrapper().select("distinct NAME,line_no"))}，
     * 返回仅含 {@code name} 与 {@code lineNo} 两个字段的去重 line 列表。</p>
     *
     * <p>DataupLoad 改造：{@code Line} 实体替代 PSM {@code LinePO}（字段一致；W-CLEAN-03 起 LinePO 已删除）。
     * DataupLoad 当前 {@link LineMapper} 仅 {@code BaseMapper<Line>}，未声明自定义查询，
     * 但 PSM 用的也是空 DAO（{@code lineDAO.selectList(QueryWrapper)}），
     * 因此 {@code baseMapper.selectList} 与 PSM 行为等价。</p>
     */
    @Override
    public BaseResult lineGroup() {
        List<Line> lineList = this.baseMapper.selectList(
            new QueryWrapper<Line>().select("distinct NAME,line_no"));
        return BaseResult.build().data(lineList);
    }

    /**
     * W-LIN-05 #2：调整线体顺序（PSM 1:1）。
     *
     * <p>对应 PSM LineServiceImpl.chgLineOrder()。逻辑：</p>
     * <ol>
     *   <li>校验入参 size 与 line 表总记录数一致 → 否则错误 20209</li>
     *   <li>调用 {@code lineOrderService.modLineOrder(lineOrders)}；返回 false 则错误 20210</li>
     *   <li>否则成功</li>
     * </ol>
     *
     * <p>DataupLoad 改造：{@code lineOrderService.modLineOrder} 已在 W-B03 实现
     * （{@link com.hikrobotics.solution.module.line.service.ILineOrderService#modLineOrder}）。</p>
     */
    @Override
    public BaseResult chgLineOrder(List<ChgLineOrderDTO> lineOrders) {
        if ((long) lineOrders.size() != this.count()) {
            return BaseResult.build().error("20209");
        }
        if (!Boolean.TRUE.equals(this.lineOrderService.modLineOrder(lineOrders))) {
            return BaseResult.build().error("20210");
        }
        return BaseResult.build().ok();
    }

    /**
     * W-LIN-05 #3：产线树查询（PSM 1:1）。
     *
     * <p>对应 PSM LineServiceImpl.handleLineTreeSearch()。逻辑：</p>
     * <ol>
     *   <li>遍历 {@code this.list()}（所有 line）</li>
     *   <li>按 lineNo 分组：首个出现的 line 创建父节点 {@code LineTreeItemDTO(line)}，
     *       后续同 lineNo 的 line 创建子节点 {@code new LineTreeItemDTO(line).setLineNo(line.getFaceNo())}</li>
     *   <li>最终按父节点 id 排序，返回 {@code List<LineTreeItemDTO>}</li>
     * </ol>
     *
     * <p>DataupLoad 改造：{@link LineTreeItemDTO} 构造器签名在 W-CLEAN-03 起直接接受
     * {@code Line}（{@code LineTreeItemDTO(Line po)}），与 PSM 反编译 {@code LinePO}
     * 字段一致（id/name/lineNo/faceNo），不需要 {@code BeanUtil.copyProperties} 中转。</p>
     */
    @Override
    public BaseResult handleLineTreeSearch() {
        HashMap<String, LineTreeItemDTO> sortByLineNo = Maps.newHashMap();
        for (Line line : this.list()) {
            LineTreeItemDTO tree = sortByLineNo.computeIfAbsent(
                line.getLineNo(),
                k -> new LineTreeItemDTO(line));
            tree.getChilds().add(
                new LineTreeItemDTO(line)
                    .setLineNo(line.getFaceNo()));
        }
        List<LineTreeItemDTO> data = sortByLineNo.values().stream()
            .sorted(Comparator.comparing(LineTreeItemDTO::getId))
            .toList();
        return BaseResult.build().data(data);
    }

    /**
     * W-LIN-05 #4：按 lineNo 列表批量查询线体（PSM 1:1 重载）。
     *
     * <p>对应 PSM LineServiceImpl.listByLineNo(List&lt;String&gt;)。逻辑：</p>
     * <ul>
     *   <li>lineNos 非空 → {@code this.list(Wrappers.lambdaQuery().in(Line::getLineNo, lineNos))}</li>
     *   <li>lineNos 为空 → 返回空 {@code Lists.newArrayList()}</li>
     * </ul>
     *
     * <p>DataupLoad 改造：Java 重载区分参数类型，与已有 {@code listByLineNo(String)}（W-B03）
     * 共存无歧义。</p>
     */
    @Override
    public List<Line> listByLineNo(List<String> lineNos) {
        if (CollectionUtil.isNotEmpty(lineNos)) {
            return this.list(Wrappers.<Line>lambdaQuery().in(Line::getLineNo, lineNos));
        }
        return Lists.newArrayList();
    }

    // ============================================================
    // W-LIN-06 — plan/manage endpoint 真实业务实装
    //   业务入口从 PSM PlanServiceImpl.clientPlan(ClientPlanQueryDTO)
    //   迁移到 LineServiceImpl.planOrderDtos(String, String, Integer, Integer)，
    //   与 /web/line/plan/manage endpoint 联通。
    // ============================================================

    /**
     * W-LIN-06：产线配方大屏管理分页查询。
     *
     * <p>业务语义对齐 PSM {@code PlanServiceImpl.clientPlan(ClientPlanQueryDTO)}：
     * 按 {@code (lineNo, faceNo)} 联查 {@code plan} × {@code plan_to_line} × {@code line}，
     * 返回该产线下分发到该面的全部配方（含运行状态）。DataupLoad 沿用既有
     * {@link com.hikrobotics.solution.module.line.mapper.PlanMapper#selectClientPlan(String, String)}
     * 执行同样 SQL。</p>
     *
     * <p>PSM {@code PlanServiceImpl.clientPlan} 原签名仅接受 {@code ClientPlanQueryDTO(lineNo, faceNo)}
     * 并返回全量列表；DataupLoad 在本工单补齐分页维度（{@code page / size}），分页模式与项目其它
     * listPage 端点保持一致：</p>
     * <ul>
     *   <li>{@code page == null || page <= 0} → 退化为第 1 页</li>
     *   <li>{@code size == null || size <= 0} → 不分页，返回全量列表
     *       （{@code BaseResult.data(List<ClientPlanResultDTO>)}）</li>
     *   <li>否则 → {@code Page<ClientPlanResultDTO>} 内存分页
     *       （{@code BaseResult.data(IPage<ClientPlanResultDTO>)}）</li>
     * </ul>
     *
     * <p>注：{@code PlanMapper.selectClientPlan} 是无 {@code @Param("pageable")} 参数的 SQL，
     * 分页由 service 层在内存中对 {@code List<ClientPlanResultDTO>} 做 subList 切片并包装为
     * {@code Page<ClientPlanResultDTO>}（与 PSM 反编译产物中其它 listPage 端点对 limit-only
     * mapper 的处理模式一致）。</p>
     *
     * <p>与 {@code PlanServiceImpl.clientPlan} 的关系：本方法迁移其业务语义到 {@code LineServiceImpl}，
     * {@code PlanServiceImpl.clientPlan} 仍保留（PSM 1:1），二者调用的底层 SQL 完全相同。</p>
     */
    @Override
    public BaseResult planOrderDtos(String lineNo, String faceNo, Integer page, Integer size) {
        if (lineNo == null || lineNo.isEmpty() || faceNo == null || faceNo.isEmpty()) {
            return BaseResult.build().error("20206")
                .log("planOrderDtos failed, lineNo/faceNo blank.", lineNo + "/" + faceNo);
        }
        List<ClientPlanResultDTO> all = this.planMapper.selectClientPlan(lineNo, faceNo);
        if (all == null) {
            all = Lists.newArrayList();
        }
        // 不分页：size 非法 / null → 原样返回全量
        if (size == null || size <= 0) {
            return BaseResult.build().data(all);
        }
        int p = (page == null || page <= 0) ? 1 : page;
        long total = all.size();
        int fromIndex = Math.min((p - 1) * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        List<ClientPlanResultDTO> pageData = all.subList(fromIndex, toIndex);
        Page<ClientPlanResultDTO> result = new Page<>(p, size, total);
        result.setRecords(pageData);
        return BaseResult.build().data(result);
    }
}
