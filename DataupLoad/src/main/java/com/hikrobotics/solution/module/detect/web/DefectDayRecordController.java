package com.hikrobotics.solution.module.detect.web;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.detect.entity.DefectDayRecord;
import com.hikrobotics.solution.module.detect.service.IDefectDayRecordService;
import com.hikrobotics.solution.module.line.dto.DefectCountDTO;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单 W-DET-03 detect 模块 day-record HTTP 入口。
 *
 * <p>对应 PSM 反编译 {@code com.hikrobotics.solution.module.detect.web.DefectDayRecordController}
 * （PSM 反编译产物中本类不独立存在 —— DayRecord 相关 endpoint 在原 PSM 也未单独成 controller；
 * 本工单按 W-DET-03 spec 新建独立 controller，URL 命名 {@code /web/detect/day-record/**}）。</p>
 *
 * <p>endpoint 与 service 方法 1:1 对齐：</p>
 * <ul>
 *   <li>{@code POST /web/detect/day-record/list-by-attribute} → {@link IDefectDayRecordService#listByAttribute}</li>
 *   <li>{@code POST /web/detect/day-record/list-by-start} → {@link IDefectDayRecordService#listByStartTime}</li>
 *   <li>{@code POST /web/detect/day-record/list-between} → {@link IDefectDayRecordService#listBetween}</li>
 *   <li>{@code POST /web/detect/day-record/search-defect-count} → {@link IDefectDayRecordService#searchDefectCount}</li>
 * </ul>
 *
 * <p>按工单 W-DET-03 要求，{@code @RequestParam} 全部显式声明 {@code name} 属性，
 * 避免依赖 Spring 隐式反射（编译产物里很多 PSM 反编译类没保留参数名）。</p>
 *
 * <p>不做 service 层改动；本类仅做"参数 → service 调用 → BaseResult.data(...)"薄封装。</p>
 */
@RestController
@RequestMapping("/web/detect/day-record")
public class DefectDayRecordController {

   private static final Logger log = LoggerFactory.getLogger(DefectDayRecordController.class);

   @Autowired
   private IDefectDayRecordService defectDayRecordService;

   /**
    * 按任意字段（{@code SFunction}）等值查询 defect_day_record。
    *
    * <p>对应 service 方法 {@link IDefectDayRecordService#listByAttribute}。</p>
    *
    * <p>实现要点：service 接口签名是 {@code <T> listByAttribute(T value, SFunction<DefectDayRecord,T> getter)}；
    * controller 层根据 {@code attr} 字符串（"time"/"lineNo"/"faceNo"/"type"/"id"/"count"）
    * 映射到 DefectDayRecord 已知 getter，按已知字段做精确查询。
    * 当前 PSM 风格只暴露以下白名单字段，其它 attr 一律 400 等价（返回空 list）。</p>
    */
   @PostMapping("/list-by-attribute")
   public BaseResult listByAttribute(@RequestParam(name = "attr") String attr,
                                      @RequestParam(name = "value", required = false) String value) {
      log.debug("listByAttribute attr={}, value={}", attr, value);
      SFunction<DefectDayRecord, ?> getter = resolveGetter(attr);
      if (getter == null) {
         log.warn("listByAttribute unsupported attr={}", attr);
         return BaseResult.build().data(List.of());
      }
      @SuppressWarnings({"unchecked", "rawtypes"})
      List<DefectDayRecord> data = this.defectDayRecordService.listByAttribute(value, (SFunction) getter);
      return BaseResult.build().data(data);
   }

   /**
    * 按 time 下界字符串（"yyyy-MM-dd HH" 整点 / "yyyy-MM-dd HH:mm:ss" 全格式）查询。
    *
    * <p>对应 service 方法 {@link IDefectDayRecordService#listByStartTime}。</p>
    */
   @PostMapping("/list-by-start")
   public BaseResult listByStartTime(@RequestParam(name = "startTime") String startTime) {
      log.debug("listByStartTime startTime={}", startTime);
      List<DefectDayRecord> data = this.defectDayRecordService.listByStartTime(startTime);
      return BaseResult.build().data(data);
   }

   /**
    * 按 time 闭区间 [startTime, endTime] 查询 defect_day_record（按 time 倒序）。
    *
    * <p>对应 service 方法 {@link IDefectDayRecordService#listBetween}。</p>
    */
   @PostMapping("/list-between")
   public BaseResult listBetween(@RequestParam(name = "startTime") String startTime,
                                  @RequestParam(name = "endTime") String endTime) {
      log.debug("listBetween startTime={}, endTime={}", startTime, endTime);
      List<DefectDayRecord> data = this.defectDayRecordService.listBetween(startTime, endTime);
      return BaseResult.build().data(data);
   }

   /**
    * 按精确 time + lineNo + faceNo + defects 集合查询缺陷数（聚合返回 {@code List<DefectCountDTO>}）。
    *
    * <p>对应 service 方法 {@link IDefectDayRecordService#searchDefectCount(String, String, String, List)}。</p>
    *
    * <p>字段：</p>
    * <ul>
    *   <li>{@code time} — 精确整点字符串（"yyyy-MM-dd HH:mm:ss"），与 defect_day_record.time 等值匹配</li>
    *   <li>{@code lineNo} — 产线号</li>
    *   <li>{@code faceNo} — 面编号</li>
    *   <li>{@code defects} — 缺陷名集合（{@code @RequestBody} JSON 数组）；空集合时 service 直接返回空 list</li>
    * </ul>
    */
   @PostMapping("/search-defect-count")
   public BaseResult searchDefectCount(@RequestParam(name = "time") String time,
                                        @RequestParam(name = "lineNo") String lineNo,
                                        @RequestParam(name = "faceNo") String faceNo,
                                        @RequestBody List<String> defects) {
      log.debug("searchDefectCount time={}, lineNo={}, faceNo={}, defects={}",
              time, lineNo, faceNo, defects);
      List<DefectCountDTO> data = this.defectDayRecordService.searchDefectCount(time, lineNo, faceNo, defects);
      return BaseResult.build().data(data);
   }

   // ============================== 私有辅助 ==============================

   /**
    * 把 {@code attr} 字符串映射到 DefectDayRecord 的白名单 getter。
    *
    * <p>仅暴露 PSM 已有字段（id / count / time / lineNo / faceNo / type），
    * 其余 attr 视为非法 → 返回 null，调用方按空 list 处理。</p>
    *
    * <p>类型擦除：返回类型擦为 {@code SFunction<DefectDayRecord, ?>}，调用方不需要具体 T 类型。</p>
    */
   @SuppressWarnings("unchecked")
   private static SFunction<DefectDayRecord, ?> resolveGetter(String attr) {
      if (Objects.isNull(attr)) {
         return null;
      }
      switch (attr) {
         case "id":
            return DefectDayRecord::getId;
         case "count":
            return DefectDayRecord::getCount;
         case "time":
            return DefectDayRecord::getTime;
         case "lineNo":
            return DefectDayRecord::getLineNo;
         case "faceNo":
            return DefectDayRecord::getFaceNo;
         case "type":
            return DefectDayRecord::getType;
         default:
            return null;
      }
   }
}
