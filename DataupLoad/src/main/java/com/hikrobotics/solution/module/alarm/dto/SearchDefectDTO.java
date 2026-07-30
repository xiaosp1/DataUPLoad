package com.hikrobotics.solution.module.alarm.dto;

import com.hikrobotics.solution.framework.common.query.PageQuery;

/**
 * 缺陷查询 DTO（W-DEFECT-CFG 子单 A：PSM 同款 SearchDefectDTO 1:1 迁回）。
 *
 * <p>原 DataupLoad 版本只有空类，{@code DefectTypeController.listDefect} 拿不到分页 / name / category 字段。
 * 本工单补齐：继承 PSM 同款 {@link PageQuery}（含 pageNum/pageSize），并加 name（模糊）+ category（下拉过滤）。
 * 前端 PSM 老 SPA {@code defectManage.js} 与新 Vue3 {@code DefectConfig.vue} 均按 query string 绑定（{@code ?pageNum=&pageSize=&name=&category=}）。
 */
public class SearchDefectDTO extends PageQuery {
   private String name;
   private Integer category;

   public String getName() {
      return this.name;
   }

   public Integer getCategory() {
      return this.category;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setCategory(Integer category) {
      this.category = category;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof SearchDefectDTO other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         Object this$category = this.getCategory();
         Object other$category = other.getCategory();
         if (this$category == null ? other$category == null : this$category.equals(other$category)) {
            Object this$name = this.getName();
            Object other$name = other.getName();
            return this$name == null ? other$name == null : this$name.equals(other$name);
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof SearchDefectDTO;
   }

   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $category = this.getCategory();
      result = result * 59 + ($category == null ? 43 : $category.hashCode());
      Object $name = this.getName();
      result = result * 59 + ($name == null ? 43 : $name.hashCode());
      return result;
   }

   public String toString() {
      return "SearchDefectDTO(name=" + this.getName() + ", category=" + this.getCategory() + ")";
   }
}
