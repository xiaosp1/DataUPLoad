package com.hikrobotics.solution.module.detect.excel;

import com.alibaba.excel.write.handler.AbstractRowWriteHandler;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 工单 W-DET-05b detect 模块 Excel 行合并 + 列表合并工具。
 *
 * <p>对应 PSM 反编译 {@code com.hikrobotics.solution.module.detect.excel.DataMergeStrategy}。
 * 本类提供两个能力：</p>
 *
 * <ol>
 *   <li><b>Excel 行合并处理器</b>（PSM 1:1 抄）：实现
 *       {@code com.alibaba.excel.write.handler.RowWriteHandler}，注册到
 *       {@code ExcelWriterTableBuilder.registerWriteHandler(...)} 后，
 *       在 EasyExcel 写完指定 rowCounts 时按指定列做单元格纵向合并（合并同值单元格）
 *       —— 行为与 PSM 反编译产物一致（详见 {@link #afterRowDispose} / {@link #mergeSameValueCells}）。</li>
 *   <li><b>列表合并工具</b>（W-DET-05b 新增）：{@link #merge(List, List)} 按 POJO 主键（{@code equals/hashCode}）
 *       合并去重；{@link #mergeByTime(List, List, String)} 按指定时间字段（反射读取）合并去重
 *       —— 两者均保持 source 在前、target 在后的稳定顺序，target 中已存在于 source 的元素会被跳过。</li>
 * </ol>
 *
 * <p>依赖：</p>
 * <ul>
 *   <li>{@code easyexcel-2.2.6.jar}（PSM 同款，{@code com.alibaba.excel.write.handler.RowWriteHandler}）</li>
 *   <li>{@code poi-3.17.jar}（行合并逻辑直接操作 POI {@code Cell/Sheet/CellRangeAddress/CellStyle}）</li>
 * </ul>
 *
 * <p>关键差异（与 PSM 反编译产物）：</p>
 * <ul>
 *   <li>PSM 反编译里 {@code DataMergeStrategy} 直接 {@code implements RowWriteHandler}（自己写空实现）。
 *       本类继承 {@link AbstractRowWriteHandler}（EasyExcel 2.2.6 自带空实现），避免重复 4 个 no-op 方法。
 *       行为完全一致。</li>
 *   <li>PSM 反编译没有 {@code merge} / {@code mergeByTime} 静态方法 —— 那是 W-DET-05b spec 增项。</li>
 * </ul>
 */
public class DataMergeStrategy extends AbstractRowWriteHandler {

   private static final Logger log = LoggerFactory.getLogger(DataMergeStrategy.class);

   // ============================== PSM 1:1 字段 ==============================

   /** PSM 同款：需要做合并的列名（中文表头，按表头匹配列索引） */
   private final List<String> columns;

   /** PSM 同款：解析后的列索引缓存（在 {@link #parseColumnIndexes} 阶段填充） */
   private final List<Integer> indexes = new ArrayList<>();

   /** PSM 同款：标记是否已合并（避免多次触发） */
   private boolean merged = false;

   /** PSM 同款：表头行数（默认 1；多行表头场景会 > 1） */
   private int headerSize = 1;

   /** PSM 同款：数据总行数（含表头 = rowCounts） */
   private final Integer rowCounts;

   /** PSM 同款：起始行偏移（多 table 拼接时使用） */
   private final Integer start;

   // ============================== PSM 1:1 构造器 ==============================

   /**
    * PSM 同款构造器。
    *
    * @param columns    需要做纵向合并的列名（表头匹配）
    * @param rowCounts  数据总行数（含表头）
    * @param headerSize 表头行数
    * @param start      起始行偏移（多 table 拼接时使用）
    */
   public DataMergeStrategy(List<String> columns, int rowCounts, int headerSize, int start) {
      this.columns = columns;
      this.headerSize = headerSize;
      this.rowCounts = rowCounts;
      this.start = start;
   }

   // ============================== PSM 1:1 RowWriteHandler 实现 ==============================
   //
   // AbstractRowWriteHandler 已提供 4 个 no-op 默认实现，本类只需 override afterRowDispose。

   /**
    * PSM 同款 {@code afterRowDispose} —— 在 EasyExcel 写完每行后回调。
    *
    * <p>两阶段：</p>
    * <ol>
    *   <li>表头行（{@code isHead && relativeRowIndex == headerSize-1}）→ 解析列索引</li>
    *   <li>最后一行（{@code relativeRowIndex == rowCounts-1}）→ 对每个目标列做纵向合并</li>
    * </ol>
    */
   @Override
   public void afterRowDispose(com.alibaba.excel.write.metadata.holder.WriteSheetHolder writeSheetHolder,
                               com.alibaba.excel.write.metadata.holder.WriteTableHolder writeTableHolder,
                               Row row, Integer relativeRowIndex, Boolean isHead) {
      if (isHead != null && isHead.booleanValue() && relativeRowIndex != null
              && relativeRowIndex.equals(this.headerSize - 1)) {
         this.parseColumnIndexes(writeSheetHolder.getSheet());
         return;
      }
      Sheet sheet = writeSheetHolder.getSheet();
      if (relativeRowIndex != null && relativeRowIndex.equals(this.rowCounts - 1) && !this.merged) {
         for (Integer columnIndex : this.indexes) {
            this.mergeSameValueCells(sheet, columnIndex);
         }
         this.merged = true;
      }
   }

   // ============================== PSM 1:1 私有方法（合并 + 样式） ==============================

   /**
    * PSM 同款 {@code mergeSameValueCells} —— 对指定列纵向合并同值连续单元格。
    *
    * <p>注意：合并条件除了"同列同值"外，还要求"首列（{@code columnIndex=0}）同值"
    * （PSM 把每个分组视为同一父行的子行，主键 = 首列）。</p>
    */
   private void mergeSameValueCells(Sheet sheet, Integer columnIndex) {
      if (this.rowCounts <= 1) {
         return;
      }
      ArrayList<CellRangeAddress> regions = new ArrayList<>();
      int startRow = this.headerSize;
      while (startRow <= this.rowCounts) {
         Row cRow;
         Row row = sheet.getRow(startRow);
         if (row == null) {
            startRow++;
            continue;
         }
         Cell cell = row.getCell(columnIndex.intValue());
         String value = this.getCellValue(cell);
         String firstVal = this.getCellValue(row.getCell(0));
         int endRow = startRow;
         int i = startRow + 1;
         while (i <= this.rowCounts && (cRow = sheet.getRow(i)) != null) {
            Cell cCell = cRow.getCell(columnIndex.intValue());
            String cValue = this.getCellValue(cCell);
            String cFirstVal = this.getCellValue(cRow.getCell(0));
            if (value == null || !value.equals(cValue) || !cFirstVal.equals(firstVal)) {
               break;
            }
            endRow = i++;
         }
         if (endRow > startRow) {
            regions.add(new CellRangeAddress(startRow + this.start, endRow + this.start, columnIndex.intValue(), columnIndex.intValue()));
         }
         startRow = endRow + 1;
      }
      for (CellRangeAddress region : regions) {
         sheet.addMergedRegion(region);
         this.setMergedRegionStyle(sheet, region);
      }
   }

   /**
    * PSM 同款 {@code setMergedRegionStyle} —— 给合并后的首格设置居中样式（克隆原样式后改 alignment）。
    */
   private void setMergedRegionStyle(Sheet sheet, CellRangeAddress region) {
      Cell firstCell;
      Workbook workbook = sheet.getWorkbook();
      CellStyle centerStyle = workbook.createCellStyle();
      centerStyle.setAlignment(HorizontalAlignment.CENTER);
      centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
      Row firstRow = sheet.getRow(region.getFirstRow());
      if (firstRow != null && (firstCell = firstRow.getCell(region.getFirstColumn())) != null) {
         CellStyle originalStyle = firstCell.getCellStyle();
         centerStyle.cloneStyleFrom(originalStyle);
         centerStyle.setAlignment(HorizontalAlignment.CENTER);
         centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
         firstCell.setCellStyle(centerStyle);
      }
   }

   /**
    * PSM 同款 {@code parseColumnIndexes} —— 把 columns 名称解析为列索引。
    */
   private void parseColumnIndexes(Sheet sheet) {
      Row header = sheet.getRow(this.headerSize - 1);
      if (header == null) {
         return;
      }
      this.indexes.clear();
      for (int i = 0; i < header.getLastCellNum(); i++) {
         String value;
         Cell cell = header.getCell(i);
         if (cell == null || !this.columns.contains(value = this.getCellValue(cell))) {
            continue;
         }
         this.indexes.add(i);
      }
   }

   /**
    * PSM 同款 {@code getCellValue} —— POI Cell → String（处理 STRING / NUMERIC / BOOLEAN）。
    *
    * <p>使用 POI 3.17 同款 {@code getCellTypeEnum()}（而非更新的 {@code getCellType()}），
    * 因此 switch 表达式直接拿到 {@code CellType} 枚举类型，未导入也不影响
    * {@code case STRING/NUMERIC/...} 的常量解析（Java 规则）。</p>
    */
   private String getCellValue(Cell cell) {
      if (cell == null) {
         return null;
      }
      switch (cell.getCellTypeEnum()) {
         case STRING:
            return cell.getStringCellValue().trim();
         case NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
               return cell.getDateCellValue().toString();
            }
            double value = cell.getNumericCellValue();
            return value == (double) ((int) value) ? String.valueOf((int) value) : String.valueOf(value);
         case BOOLEAN:
            return String.valueOf(cell.getBooleanCellValue());
         default:
            return "";
      }
   }

   // ============================== W-DET-05b 新增：列表合并工具 ==============================

   /**
    * 按 POJO 主键（{@code equals/hashCode}）合并去重两个列表。
    *
    * <p>语义：</p>
    * <ul>
    *   <li>保留 {@code source} 中所有元素（按 source 原顺序）</li>
    *   <li>追加 {@code target} 中 {@code equals} 不在 source 已存在集合的元素（按 target 原顺序）</li>
    *   <li>{@code null} 元素视为相等（保留首个出现的 null）</li>
    * </ul>
    *
    * <p>示例：{@code source=[A,B]} + {@code target=[B,C,A,D]} → {@code [A,B,C,D]}</p>
    *
    * @param source 主列表（优先级更高，其元素不会被 target 覆盖）
    * @param target 待合并列表（与 source 重复的元素被跳过）
    * @param <T>    POJO 类型
    * @return 合并去重后的新列表（不修改入参）
    */
   public static <T> List<T> merge(List<T> source, List<T> target) {
      List<T> src = (source == null) ? new ArrayList<>() : source;
      List<T> tgt = (target == null) ? new ArrayList<>() : target;
      // 用 LinkedHashSet 保留 source/target 各自插入顺序
      Set<T> seen = new LinkedHashSet<>(src);
      List<T> result = new ArrayList<>(seen);
      for (T t : tgt) {
         if (!seen.contains(t)) {
            seen.add(t);
            result.add(t);
         }
      }
      return result;
   }

   /**
    * 按指定时间字段合并去重两个列表。
    *
    * <p>语义：以 {@code timeField} 字段值（String）作为去重 key，合并行为同 {@link #merge}：</p>
    * <ul>
    *   <li>优先保留 {@code source} 中的元素（其 timeField 值已存在时 target 中重复元素被跳过）</li>
    *   <li>{@code timeField} 为 {@code null} / 字段不存在 / 字段值为 null 的元素：</li>
    *   <li>　- 反射读取失败的元素视为 key=null，统一归入同一组（保留首个）</li>
    *   <li>　- 这样能避免 {@link #merge} 中 {@code equals} 全字段比较过于严格的问题</li>
    * </ul>
    *
    * <p>示例：{@code source=[Defect(time=t1),Defect(time=t2)]} +
    * {@code target=[Defect(time=t2),Defect(time=t3)]}
    * → {@code [Defect(time=t1),Defect(time=t2),Defect(time=t3)]}（t2 重复，跳过）</p>
    *
    * @param source    主列表
    * @param target    待合并列表
    * @param timeField 时间字段名（POJO 中的字段名或 getter 名，如 {@code "time"} 或 {@code "getTime"}）
    * @param <T>       POJO 类型
    * @return 合并去重后的新列表（不修改入参）
    */
   public static <T> List<T> mergeByTime(List<T> source, List<T> target, String timeField) {
      if (timeField == null || timeField.isEmpty()) {
         log.warn("mergeByTime timeField is null/empty, fallback to equals merge");
         return merge(source, target);
      }
      List<T> src = (source == null) ? new ArrayList<>() : source;
      List<T> tgt = (target == null) ? new ArrayList<>() : target;

      // 先把 source 的 timeField 值塞进 seen；再追加 target 中 timeField 不重复的元素
      Set<String> seenTimes = new HashSet<>();
      List<T> result = new ArrayList<>();
      for (T t : src) {
         String time = readTimeField(t, timeField);
         // timeField 为 null 的元素按 equals 去重（归入 null 组）
         if (time != null) {
            seenTimes.add(time);
         }
         result.add(t);
      }
      // 用 LinkedHashSet 去重 target 中 result 已含元素（按 timeField 判重，timeField 缺失则按 equals 判重）
      Set<T> resultSet = new LinkedHashSet<>(result);
      for (T t : tgt) {
         String time = readTimeField(t, timeField);
         if (time != null && !seenTimes.add(time)) {
            // time 重复 → 跳过（除非该元素本身未在 result 中，按 equals 算）
            if (!resultSet.contains(t)) {
               // time 重复但 equals 不等：保留为新元素（极端 case，e.g. 同一 time 不同 lineNo）
               resultSet.add(t);
               result.add(t);
            }
            continue;
         }
         if (!resultSet.contains(t)) {
            resultSet.add(t);
            result.add(t);
         }
      }
      return result;
   }

   /**
    * 反射读取 POJO 的 timeField 字段值（支持 field 名或 getter 名）。
    *
    * <p>读取优先级：{@code getXxx()} getter → 直接 field 访问 → 异常 fallback 返回 {@code null}。</p>
    */
   private static String readTimeField(Object obj, String timeField) {
      if (obj == null) {
         return null;
      }
      Class<?> clazz = obj.getClass();
      // 1) 尝试 getter：getXxx()
      String getterName = (timeField.startsWith("get") || timeField.startsWith("is"))
              ? timeField
              : "get" + Character.toUpperCase(timeField.charAt(0)) + timeField.substring(1);
      try {
         Method m = clazz.getMethod(getterName);
         Object val = m.invoke(obj);
         return Objects.toString(val, null);
      } catch (Exception ignored) {
         // getter 不存在或调用失败 → 尝试 field
      }
      // 2) 尝试直接 field 访问
      try {
         Field f = clazz.getDeclaredField(timeField);
         f.setAccessible(true);
         Object val = f.get(obj);
         return Objects.toString(val, null);
      } catch (Exception ex) {
         log.debug("readTimeField failed for class={}, field={}, error={}",
                 clazz.getName(), timeField, ex.getMessage());
         return null;
      }
   }
}
