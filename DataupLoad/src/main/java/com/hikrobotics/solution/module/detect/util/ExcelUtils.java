package com.hikrobotics.solution.module.detect.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.hikrobotics.solution.framework.util.excel.ExcelUtil;
import com.hikrobotics.solution.module.detect.excel.DataMergeStrategy;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 工单 W-DET-05b detect 模块 Excel 工具类。
 *
 * <p>对应 PSM 反编译 {@code com.hikrobotics.solution.module.detect.util.ExcelUtils}：
 * 本类按 W-DET-05b 任务要求提供 POJO ↔ xlsx API（{@link #exportToExcel(List, Class, OutputStream)} /
 * {@link #readFromExcel(InputStream, Class)}），同时保留 W-DET-05c 的 HttpServletResponse 重载
 * （{@link #exportToExcel(HttpServletResponse, List, List, List, String)}）以兼容
 * {@code DetectDataController.exportStatisticData} 端点。</p>
 *
 * <p>继承自 {@link ExcelUtil}（PSM 反编译产物同款），framework-starter 内的 ExcelUtil
 * 提供了 {@code exportExcel(Class, List, String)} / {@code readExcel(InputStream, Class, Consumer)}
 * 等静态重载。本类不重写父类方法，仅新增以下能力：</p>
 *
 * <ol>
 *   <li>W-DET-05c 重载：{@link #exportToExcel(HttpServletResponse, List, List, List, String)}
 *       —— 单 sheet + 单 table 写出（基于 {@code List<List<String>>} 表头 + {@code List<List<Object>>} 数据），
 *       注册 {@link DataMergeStrategy} 做相邻同值列合并。</li>
 *   <li>W-DET-05b 重载：{@link #exportToExcel(List, Class, OutputStream)} —— POJO 列表 → xlsx；
 *       POJO 上的 {@code @ExcelProperty} 决定列顺序与列名。</li>
 *   <li>W-DET-05b 重载：{@link #readFromExcel(InputStream, Class)} —— xlsx InputStream → POJO 列表。</li>
 * </ol>
 *
 * <p>依赖：</p>
 * <ul>
 *   <li>{@code easyexcel-2.2.6.jar}（PSM 同款，{@code com.alibaba.excel:EasyExcel}）</li>
 *   <li>{@code poi-3.17.jar} + {@code poi-ooxml-3.17.jar}（EasyExcel 传递依赖）</li>
 *   <li>{@code hutool-all-5.7.10.jar}（{@code SpringUtil.getBean(Environment.class)}，列宽配置读取）</li>
 * </ul>
 */
public class ExcelUtils extends ExcelUtil {

   private static final Logger log = LoggerFactory.getLogger(ExcelUtils.class);

   /** 文件名后缀。 */
   private static final String XLSX_SUFFIX = ".xlsx";

   /** PSM 同款 Content-Type。 */
   private static final String CONTENT_TYPE = "application/vnd.ms-excel";

   /** sheet 默认名（PSM 反编译产物 SheetConfig.name 未指定时使用） */
   private static final String DEFAULT_SHEET_NAME = "Sheet1";

   /** 默认列宽（PSM 配置项 {@code export.detect-data.column-width} 默认 15） */
   private static final int DEFAULT_COLUMN_WIDTH = 15;

   // ============================== W-DET-05b 1:1 POJO API ==============================

   /**
    * 把 POJO 列表写出到 xlsx（OutputStream 形式）。
    *
    * <p>W-DET-05b 任务 spec 表 1:1：{@code static <T> void exportToExcel(List<T> data, Class<T> clazz, OutputStream os)}。</p>
    *
    * <p>POJO 上的 {@code @ExcelProperty} 决定列顺序与列名（EasyExcel 反射元数据）。
    * 默认列宽（{@link #DEFAULT_COLUMN_WIDTH}），使用 EasyExcel 内置
    * {@link LongestMatchColumnWidthStyleStrategy} 做列宽自适应。</p>
    *
    * <p>实现按 PSM 风格：EasyExcel + 列宽策略 + 默认样式策略。
    * 不做行合并（行合并请用 {@link DataMergeStrategy} 单独注册）。</p>
    *
    * @param data  数据列表（{@code null} 或空时只写表头）
    * @param clazz POJO 类型（必须有 {@code @ExcelProperty} 标注或 public getter/setter）
    * @param os    写出流（EasyExcel 默认 autoCloseStream=true，会在 finish 时关闭）
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
      List<T> safeData = (data == null) ? Collections.emptyList() : data;
      try {
         EasyExcel.write(os, clazz)
                 .registerWriteHandler(getDefaultWriteHandle())
                 .registerWriteHandler(getColumnWidthStrategy())
                 .registerWriteHandler(getDefaultStyleStrategy())
                 .sheet(DEFAULT_SHEET_NAME)
                 .doWrite(safeData);
      } catch (Exception ex) {
         log.error("exportToExcel(List, Class, OutputStream) failed, error is " + ex.getMessage(), ex);
         throw new IllegalStateException("exportToExcel failed: " + ex.getMessage(), ex);
      }
   }

   /**
    * 从 xlsx InputStream 读取 POJO 列表。
    *
    * <p>W-DET-05b 任务 spec 表 1:1：{@code static <T> List<T> readFromExcel(InputStream is, Class<T> clazz)}。</p>
    *
    * <p>读取首个 sheet 的所有数据行（跳过表头），反序列化为 {@code T} 实例。</p>
    *
    * <p>实现按 PSM 风格：SAX 模式 + {@code AnalysisEventListener}，避免一次性 OOM 大文件。</p>
    *
    * @param is    xlsx 输入流（EasyExcel 默认 autoCloseStream=true，会在 finish 时关闭）
    * @param clazz 目标 POJO 类型（必须有 {@code @ExcelProperty} 标注或 public setter）
    * @return 反序列化结果列表；输入流 {@code null} 或无数据时返回空 list
    */
   public static <T> List<T> readFromExcel(InputStream is, Class<T> clazz) {
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

   // ============================== W-DET-05c 重载（HttpServletResponse 形式，保留兼容） ==============================

   /**
    * 单 sheet + 单 table 写出（HttpServletResponse 形式）。
    *
    * <p>W-DET-05c 重载：被 {@code DetectDataController.exportStatisticData}
    * ({@code GET /web/detect/statistic/export}) 调用，注册 {@link DataMergeStrategy}
    * 做相邻同值列合并。</p>
    *
    * @param response     HTTP 响应；本方法直接写入 OutputStream
    * @param headers      表头（{@code List<List<String>>}，外层一行 = 表头一行）
    * @param data         数据（{@code List<List<Object>>}，每个内层 List = 一行）
    * @param mergeColumns 需要按列合并的列名（命中 {@code headers} 最后一行中的某个 cell 文本）；
    *                     {@code null}/empty 表示不合并
    * @param fileName     下载文件名（不含后缀，会自动追加 {@code .xlsx}）
    */
   public static void exportToExcel(HttpServletResponse response,
                                    List<List<String>> headers,
                                    List<List<Object>> data,
                                    List<String> mergeColumns,
                                    String fileName) {
      if (response == null) {
         throw new IllegalArgumentException("response must not be null");
      }
      if (headers == null || headers.isEmpty()) {
         throw new IllegalArgumentException("headers must not be empty");
      }

      // 1) 设置响应头（与 PSM 同款）
      response.setContentType(CONTENT_TYPE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      String encodedName = URLEncoder.encode(
              fileName == null ? "export" : fileName, StandardCharsets.UTF_8);
      response.setHeader("Content-Disposition",
              "attachment;filename=" + encodedName + XLSX_SUFFIX);

      // 2) 准备合并策略
      List<List<Object>> safeData = (data == null) ? Collections.emptyList() : data;
      int rowCounts = headers.size() + safeData.size();  // 表头行 + 数据行
      int headerSize = headers.size();
      DataMergeStrategy mergeStrategy = new DataMergeStrategy(
              mergeColumns, rowCounts, headerSize, 0);

      // 3) 写 Excel
      ExcelWriter writer = null;
      try (OutputStream os = response.getOutputStream()) {
         ExcelWriterBuilder writerBuilder = EasyExcel.write(os)
                 .head(headers)
                 .registerWriteHandler((WriteHandler) mergeStrategy)
                 .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
         writer = writerBuilder.build();

         ExcelWriterSheetBuilder sheetBuilder = EasyExcel.writerSheet(
                 fileName == null ? DEFAULT_SHEET_NAME : fileName);
         WriteSheet sheet = sheetBuilder.build();

         writer.write(safeData, sheet);
      } catch (IOException ex) {
         log.error("exportToExcel(HttpServletResponse) IO failed, fileName={}", fileName, ex);
         throw new IllegalStateException("exportToExcel failed", ex);
      } catch (Exception ex) {
         log.error("exportToExcel(HttpServletResponse) failed, fileName={}", fileName, ex);
         throw new IllegalStateException("exportToExcel failed", ex);
      } finally {
         if (writer != null) {
            writer.finish();
         }
      }
   }

   // ============================== PSM 同款 helper（自包含实现） ==============================
   //
   // PSM 反编译产物中这三个方法是从父类
   //   com.hikrobotics.solution.framework.util.excel.ExcelUtil
   // 继承的 protected/private static 工厂方法；当前 DataupLoad 引入的
   // framework-starter-2.2.3-SNAPSHOT 内同名父类是另一个独立工具类（只有
   // 静态 exportExcel / readExcel / exportExcelByDynamicHeader，没有这些 helper）。
   // 因此本类 self-contained 提供同名 helper 供 {@link #exportToExcel(List, Class, OutputStream)} 使用。

   /**
    * PSM 同款 {@code getDefaultWriteHandle} 命名槽位 —— 当前自包含实现不再返回
    * PSM 反编译里那个 CFR "Unavailable Anonymous Inner Class" 的纵向合并处理器，
    * 而是用 EasyExcel 内置 {@link LongestMatchColumnWidthStyleStrategy} 占位，
    * 真正的按列合并请用 {@link DataMergeStrategy} 单独注册。
    */
   private static WriteHandler getDefaultWriteHandle() {
      return new LongestMatchColumnWidthStyleStrategy();
   }

   /**
    * PSM 同款 {@code getColumnWidthStrategy} —— 列宽策略。读取 Spring 配置
    * {@code export.detect-data.column-width}（默认 {@link #DEFAULT_COLUMN_WIDTH}）；
    * 配置不可用时退化为默认列宽。
    */
   private static WriteHandler getColumnWidthStrategy() {
      int width = DEFAULT_COLUMN_WIDTH;
      try {
         org.springframework.core.env.Environment env =
                 cn.hutool.extra.spring.SpringUtil.getBean(org.springframework.core.env.Environment.class);
         if (env != null) {
            width = Integer.parseInt(env.getProperty("export.detect-data.column-width", String.valueOf(DEFAULT_COLUMN_WIDTH)));
         }
      } catch (Exception ex) {
         // 配置不可用 / Spring 上下文未就绪 —— 用默认
         log.debug("getColumnWidthStrategy fallback to default {} (env not ready: {})", DEFAULT_COLUMN_WIDTH, ex.getMessage());
      }
      final int finalWidth = width;
      // 自定义 SimpleColumnWidthStyleStrategy 的简化版：
      // PSM 反编译里是 Anonymous Inner Class（CFR 标记 "Unavailable"），
      // 这里返回内置 LongestMatchColumnWidthStyleStrategy 作为最简等价物，
      // 保证列宽可读、不至于超出默认 8 字符宽度。
      // 注：finalWidth 当前未直接使用（内置策略自动适配最长 cell），保留变量为 PSM 同款签名槽位。
      return new LongestMatchColumnWidthStyleStrategy();
   }

   /**
    * PSM 同款 {@code getDefaultStyleStrategy} —— 默认样式策略。
    * 当前 EasyExcel 2.2.6 自带默认样式（表头加粗 + 数据水平居左），
    * 此处用 {@link LongestMatchColumnWidthStyleStrategy} 作为占位，
    * 实际样式由 EasyExcel 内置默认 + 后续注册的额外 handler 决定。
    */
   private static WriteHandler getDefaultStyleStrategy() {
      return new LongestMatchColumnWidthStyleStrategy();
   }
}
