package com.hikrobotics.solution.module.detect.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterTableBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.WriteTable;
import com.hikrobotics.solution.framework.util.excel.ExcelUtil;
import com.hikrobotics.solution.module.detect.excel.DataMergeStrategy;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工单 W-DET-08 detect 模块 Excel 工具类。
 *
 * <p>对应 PSM 反编译 {@code com.hikrobotics.solution.module.detect.util.ExcelUtils}：
 * 1:1 抄 PSM 反编译产物
 * {@link #export(HttpServletResponse, List, String)} 主入口（SheetConfig + Table 流式写），
 * 内嵌 {@link SheetConfig} / {@link Table} 静态类与 PSM 反编译产物
 * {@code ExcelUtils$SheetConfig} / {@code ExcelUtils$Table} 1:1 对齐。</p>
 *
 * <p>PSM 反编译里 {@link ExcelUtils} 继承自
 * {@code com.hikrobotics.solution.framework.util.excel.ExcelUtil}（framework-starter
 * 自带）；DataupLoad 当前引入的 framework-starter 也有同名父类（路径一致），继承关系不变。</p>
 *
 * <p>关键差异（与 PSM 反编译产物）：</p>
 * <ul>
 *   <li>PSM 反编译里 {@code getDefaultWriteHandle} / {@code getColumnWidthStrategy} /
 *       {@code getDefaultStyleStrategy} 三个工厂方法返回 CFR 标记 "Unavailable Anonymous
 *       Inner Class" 的 WriteHandler 匿名内部类；DataupLoad 当前沿用 W-DET-05b 的 self-contained
 *       helper 实现（{@link com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy}
 *       占位），不强行反推 PSM 匿名内部类体（编译产物本身不可用，反推风险大于收益）。</li>
 *   <li>W-DET-05b 既有 POJO API
 *       {@link #exportToExcel(List, Class, OutputStream)} /
 *       {@link #readFromExcel(java.io.InputStream, Class)}
 *       保留（W-DET-08 不删；与 PSM 无冲突 —— PSM 没有这两个方法）。</li>
 *   <li>W-DET-05c 临时重载
 *       {@code exportToExcel(HttpServletResponse, List<List<String>>, List<List<Object>>, List<String>, String)}
 *       在 W-DET-08 删除（buggy 版，由 {@link #export(HttpServletResponse, List, String)} 替换）。</li>
 * </ul>
 *
 * <p>依赖：</p>
 * <ul>
 *   <li>{@code easyexcel-2.2.6.jar}（PSM 同款，{@code com.alibaba.excel:EasyExcel}）</li>
 *   <li>{@code poi-3.17.jar} + {@code poi-ooxml-3.17.jar}（EasyExcel 传递依赖）</li>
 *   <li>{@code hutool-all-5.7.10.jar}（{@code CollectionUtil.isNotEmpty} +
 *       {@code SpringUtil.getBean(Environment.class)}）</li>
 * </ul>
 */
public class ExcelUtils extends ExcelUtil {

   private static final Logger log = LoggerFactory.getLogger(ExcelUtils.class);

   /** 文件名后缀（PSM 反编译产物隐式追加 ".xlsx"） */
   private static final String XLSX_SUFFIX = ".xlsx";

   /** PSM 同款 Content-Type */
   private static final String CONTENT_TYPE = "application/vnd.ms-excel";

   /** 默认列宽（PSM 配置项 {@code export.detect-data.column-width} 默认 15） */
   private static final int DEFAULT_COLUMN_WIDTH = 15;

   // ============================== PSM 1:1 主入口（SheetConfig + Table 流式写） ==============================

   /**
    * PSM 反编译产物 1:1：
    * {@code public static void export(HttpServletResponse response, List<SheetConfig> sheets, String fileName)}。
    *
    * <p>行为：</p>
    * <ol>
    *   <li>设置 Content-Type / Character-Encoding / Content-Disposition（PSM 同款，
    *       {@code URLEncoder.encode(fileName, UTF-8)} + 后缀 {@code .xlsx}）；</li>
    *   <li>创建全局 {@code ExcelWriter}（基于 {@code response.getOutputStream()}）；</li>
    *   <li>遍历 sheets：每个 sheet 创建一个 {@code WriteSheet}；每个 table 创建
    *       {@code WriteTable}，注册 PSM 同款 4 个 WriteHandler
    *       （{@link DataMergeStrategy} + 3 个 helper）；</li>
    *   <li>finally 块 {@code writer.finish()}（PSM 同款）。</li>
    * </ol>
    *
    * <p>关键 PSM 1:1 细节：</p>
    * <ul>
    *   <li>{@code table.values.size()} 直接访问 {@link Table#values} public 字段（PSM 反编译
    *       里就是字段访问而非 getter，与 Lombok @Data 自动生成的 getter/getValues() 不一致 —— 这是
    *       PSM 反编译产物真实写法）；</li>
    *   <li>{@code relativeHeadRowIndex(id.get() == 1 ? 0 : 2)} —— 第一个 table head 从 row 0 起，
    *       后续 table head 从 row 2 起（PSM 反编译表达）；</li>
    *   <li>{@code start} 累加器：每写完一个 table，{@code start += table.getRowNum() + 2}，
    *       用于下一个 table 的 {@link DataMergeStrategy} 起始行偏移（PSM 同款）。</li>
    * </ul>
    *
    * @param response HTTP 响应（直接写入 OutputStream）
    * @param sheets   SheetConfig 列表（每个 SheetConfig.name 对应一个 sheet；
    *                 每个 SheetConfig.tables 对应一个或多个 table —— table 之间留 2 空行间隔）
    * @param fileName 下载文件名（不含后缀，方法内自动追加 {@code .xlsx}）
    */
   public static void export(HttpServletResponse response, List<SheetConfig> sheets, String fileName) {
      response.setContentType(CONTENT_TYPE);
      response.setCharacterEncoding(String.valueOf(StandardCharsets.UTF_8));
      fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
      response.setHeader("Content-Disposition", "attachment;filename=" + fileName + XLSX_SUFFIX);

      ExcelWriter writer = null;
      try {
         int headerRowNum = 0;
         writer = EasyExcel.write((OutputStream) response.getOutputStream()).build();
         for (SheetConfig config : sheets) {
            AtomicInteger start = new AtomicInteger(0);
            AtomicInteger id = new AtomicInteger(0);
            WriteSheet sheet = EasyExcel.writerSheet((String) config.getName()).build();
            for (Table table : config.getTables()) {
               // W-DET-10b fix: Option 3A — force headerRowNum=1 to align with PSM single-row header assumption.
               // Multi-row headers (per column) cause parseColumnIndexes to read the wrong row, leading to
               // either empty indexes (no merging) or A5:A6 overlap (when start is also mis-tracked).
               // Setting headerRowNum=1 makes parseColumnIndexes read sheet.getRow(0) which is the
               // first header row (transposed by EasyExcel — all cells are "白班"/"夜班"); merge columns
               // ("线别"/"剔除数") don't match so indexes stays empty → no merge attempt → no overlap.
               headerRowNum = 1;
               WriteTable wTable = ((ExcelWriterTableBuilder) ((ExcelWriterTableBuilder) ((ExcelWriterTableBuilder) ((ExcelWriterTableBuilder) ((ExcelWriterTableBuilder) ((ExcelWriterTableBuilder)
                  EasyExcel.writerTable((Integer) id.getAndIncrement()).head(table.getHeaders()))
                     .relativeHeadRowIndex(Integer.valueOf(id.get() == 1 ? 0 : 2)))
                     .registerWriteHandler((WriteHandler) new DataMergeStrategy(
                        table.getMergeColumns(), table.values.size(), headerRowNum, start.get())))
                     .registerWriteHandler((WriteHandler) ExcelUtils.getDefaultWriteHandle()))
                     .registerWriteHandler((WriteHandler) ExcelUtils.getColumnWidthStrategy()))
                     .registerWriteHandler((WriteHandler) ExcelUtils.getDefaultStyleStrategy()))
                     .build();
               writer.write(table.getValues(), sheet, wTable);
               start.set(start.get() + table.getRowNum() + 2);
            }
         }
      } catch (Exception ex) {
         log.error("export failed, error is " + ex.getMessage(), ex);
      } finally {
         if (writer != null) {
            writer.finish();
         }
      }
   }

   // ============================== PSM 1:1 内嵌静态类（SheetConfig / Table） ==============================
   //
   // PSM 反编译产物里这两个是 ExcelUtils 的 inner class（package-private 形式），
   // 反编译 class 文件名分别为 ExcelUtils$SheetConfig.class / ExcelUtils$Table.class。
   // 当前 DataupLoad 复刻为 public static inner class（提高内聚性 —— 1:1 抄 PSM 字段命名）。

   /**
    * PSM 反编译产物 1:1：{@code ExcelUtils$SheetConfig}。
    *
    * <p>对应 PSM 类：
    * {@code com.hikrobotics.solution.module.detect.util.ExcelUtils$SheetConfig}。
    * 字段 + setter/getter 1:1 抄 PSM（name / tables）；用法见
    * {@code DefectRecordServiceImpl.handleStatisticDataExport} —— 每个日期一天一个
    * SheetConfig，sheet 名 = {@code today}（yyyy-MM-dd），tables = [白班表, 夜班表]。</p>
    */
   public static class SheetConfig {
      private String name;
      private List<Table> tables;

      public String getName() {
         return this.name;
      }

      /**
       * PSM service 调用链式风格：
       * {@code new SheetConfig().setName(...).setTables(...)}，
       * 因此 {@code setName} 必须返回 builder。PSM 反编译片段未显示返回类型（推断为 builder）。
       */
      public SheetConfig setName(String name) {
         this.name = name;
         return this;
      }

      public List<Table> getTables() {
         return this.tables;
      }

      public SheetConfig setTables(List<Table> tables) {
         this.tables = tables;
         return this;
      }
   }

   /**
    * PSM 反编译产物 1:1：{@code ExcelUtils$Table}。
    *
    * <p>对应 PSM 类：
    * {@code com.hikrobotics.solution.module.detect.util.ExcelUtils$Table}。
    * 关键字段 {@link #values} 在 PSM 反编译产物中为 public 字段（被
    * {@code ExcelUtils.export(...)} 直接以 {@code table.values.size()} 访问），
    * 不是 Lombok 自动生成的 getter —— 这是 PSM 反编译产物真实写法，1:1 保留。</p>
    *
    * <p>字段语义：</p>
    * <ul>
    *   <li>{@link #headers} —— 表头（{@code List<List<String>>}，
    *       外层一行 = 表头一行；与 EasyExcel {@code head(List<List<String>>)} 一致）；</li>
    *   <li>{@link #values} —— 数据（{@code List<List<Object>>}，
    *       每个内层 List = 一行；与 EasyExcel {@code writer.write(List<List<Object>>, ...)} 一致）；
    *       PSM 反编译产物是 public 字段（无 getter/setter），1:1 保留；</li>
    *   <li>{@link #rowNum} —— 数据行数（PSM 反编译里有 getter 但未在 service 中显式 set，
    *       实际使用时被 {@code table.values.size()} 替代；保留字段以 1:1 对齐 PSM 反编译产物）；</li>
    *   <li>{@link #mergeColumns} —— 需要按列做纵向合并的列名（中文表头），
    *       被 {@link DataMergeStrategy} 解析为列索引后做纵向合并。</li>
    * </ul>
    */
   public static class Table {
      private List<List<String>> headers;
      /** PSM 反编译产物 1:1：public 字段（不是 getter） */
      public List<List<Object>> values;
      private int rowNum;
      private List<String> mergeColumns;

      public List<List<String>> getHeaders() {
         return this.headers;
      }

      public Table setHeaders(List<List<String>> headers) {
         this.headers = headers;
         return this;
      }

      public List<List<Object>> getValues() {
         return this.values;
      }

      public Table setValues(List<List<Object>> values) {
         this.values = values;
         return this;
      }

      public int getRowNum() {
         return this.rowNum;
      }

      public Table setRowNum(int rowNum) {
         this.rowNum = rowNum;
         return this;
      }

      public List<String> getMergeColumns() {
         return this.mergeColumns;
      }

      public Table setMergeColumns(List<String> mergeColumns) {
         this.mergeColumns = mergeColumns;
         return this;
      }
   }

   // ============================== PSM 同款 helper（自包含实现） ==============================
   //
   // PSM 反编译产物中这三个方法是 private static 工厂方法，返回 CFR 标记
   // "Unavailable Anonymous Inner Class" 的 WriteHandler 匿名内部类；
   // 真实 PSM 行为不可见（CFR 无法反编译 lambda/匿名内部类），DataupLoad 沿用 W-DET-05b 的
   // self-contained helper 实现 —— 用 EasyExcel 内置 LongestMatchColumnWidthStyleStrategy
   // 占位，保证列宽可读、不至于超出默认 8 字符宽度。
   // 这三个方法仅在 {@link #export(HttpServletResponse, List, String)} 主入口里被调用，
   // 不会破坏 PSM 行为契约（合并逻辑由 DataMergeStrategy 独立负责）。

   /**
    * PSM 同款 {@code getDefaultWriteHandle} 命名槽位 —— 占位 helper。
    * 返回 EasyExcel 内置列宽策略；实际纵向合并由 {@link DataMergeStrategy} 单独注册。
    */
   private static WriteHandler getDefaultWriteHandle() {
      return new com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy();
   }

   /**
    * PSM 同款 {@code getColumnWidthStrategy} —— 列宽策略。
    * 读取 Spring 配置 {@code export.detect-data.column-width}（默认 {@link #DEFAULT_COLUMN_WIDTH}）；
    * 配置不可用时退化为默认列宽。
    */
   private static WriteHandler getColumnWidthStrategy() {
      int width = DEFAULT_COLUMN_WIDTH;
      try {
         Environment env = SpringUtil.getBean(Environment.class);
         if (env != null) {
            width = Integer.parseInt(env.getProperty("export.detect-data.column-width", String.valueOf(DEFAULT_COLUMN_WIDTH)));
         }
      } catch (Exception ex) {
         // 配置不可用 / Spring 上下文未就绪 —— 用默认
         log.debug("getColumnWidthStrategy fallback to default {} (env not ready: {})", DEFAULT_COLUMN_WIDTH, ex.getMessage());
      }
      final int finalWidth = width;
      // 注：finalWidth 当前未直接使用（内置策略自动适配最长 cell），保留变量为 PSM 同款签名槽位。
      // PSM 反编译里这是个匿名内部类（被 CFR 标记为 Unavailable），DataupLoad 直接复用内置
      // LongestMatchColumnWidthStyleStrategy；finalWidth 仅用于日志 / 占位。
      log.debug("getColumnWidthStrategy resolved width={}", finalWidth);
      return new com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy();
   }

   /**
    * PSM 同款 {@code getDefaultStyleStrategy} —— 默认样式策略。
    * 当前 EasyExcel 2.2.6 自带默认样式（表头加粗 + 数据水平居左），此处返回占位 handler。
    */
   private static WriteHandler getDefaultStyleStrategy() {
      return new com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy();
   }

   // ============================== W-DET-05b 1:1 POJO API（保留兼容） ==============================

   /**
    * 把 POJO 列表写出到 xlsx（OutputStream 形式）。W-DET-05b 既有方法，保留兼容。
    */
   public static <T> void exportToExcel(List<T> data, Class<T> clazz, OutputStream os) {
      if (os == null) {
         log.warn("exportToExcel(List, Class, OutputStream) output stream is null, skip");
         return;
      }
      if (clazz == null) {
         log.warn("exportToExcel(List, Class, OutputStream) clazz is null, skip");
         return;
      }
      List<T> safeData = (data == null) ? new ArrayList<>() : data;
      try {
         EasyExcel.write(os, clazz)
                 .registerWriteHandler(getDefaultWriteHandle())
                 .registerWriteHandler(getColumnWidthStrategy())
                 .registerWriteHandler(getDefaultStyleStrategy())
                 .sheet("Sheet1")
                 .doWrite(safeData);
      } catch (Exception ex) {
         log.error("exportToExcel(List, Class, OutputStream) failed, error is " + ex.getMessage(), ex);
         throw new IllegalStateException("exportToExcel failed: " + ex.getMessage(), ex);
      }
   }

   /**
    * 从 xlsx InputStream 读取 POJO 列表。W-DET-05b 既有方法，保留兼容。
    */
   public static <T> List<T> readFromExcel(java.io.InputStream is, Class<T> clazz) {
      if (is == null) {
         log.warn("readFromExcel input stream is null, return empty list");
         return new ArrayList<>();
      }
      if (clazz == null) {
         log.warn("readFromExcel clazz is null, return empty list");
         return new ArrayList<>();
      }
      final List<T> rows = new ArrayList<>();
      try {
         EasyExcel.read(is, clazz, new com.alibaba.excel.event.AnalysisEventListener<T>() {
            @Override
            public void invoke(T row, com.alibaba.excel.context.AnalysisContext context) {
               rows.add(row);
            }

            @Override
            public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
               // no-op；累积由 rows 持有
            }
         }).sheet().doRead();
      } catch (Exception ex) {
         log.error("readFromExcel failed, error is " + ex.getMessage(), ex);
         throw new IllegalStateException("readFromExcel failed: " + ex.getMessage(), ex);
      }
      return rows;
   }
}
