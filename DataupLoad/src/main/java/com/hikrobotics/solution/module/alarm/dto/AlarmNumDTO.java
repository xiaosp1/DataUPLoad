package com.hikrobotics.solution.module.alarm.dto;

/**
 * 报警数量 DTO（PSM 同名）。
 *
 * <p>W-ALM-02 修复：补齐 PSM 端 {@code builder()} 静态工厂方法，
 * 让 {@code AlarmRecordServiceImpl.handleAlarmNumGet} 可以
 * {@code AlarmNumDTO.builder().totalNum(...).highNum(...).build()} 一行链式构建。</p>
 */
public class AlarmNumDTO {
   private Integer totalNum = 0;
   private Integer highNum = 0;

   public static AlarmNumDTOBuilder builder() {
      return new AlarmNumDTOBuilder();
   }

   public Integer getTotalNum() { return this.totalNum; }
   public Integer getHighNum() { return this.highNum; }
   public AlarmNumDTO setTotalNum(Integer totalNum) { this.totalNum = totalNum; return this; }
   public AlarmNumDTO setHighNum(Integer highNum) { this.highNum = highNum; return this; }

   /** PSM 1:1 builder（@Builder 注解生成的同名类）。 */
   public static class AlarmNumDTOBuilder {
      private Integer totalNum = 0;
      private Integer highNum = 0;

      public AlarmNumDTOBuilder totalNum(Integer totalNum) { this.totalNum = totalNum; return this; }
      public AlarmNumDTOBuilder highNum(Integer highNum) { this.highNum = highNum; return this; }

      public AlarmNumDTO build() {
         AlarmNumDTO dto = new AlarmNumDTO();
         dto.totalNum = this.totalNum;
         dto.highNum = this.highNum;
         return dto;
      }
   }
}
