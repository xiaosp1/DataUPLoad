package com.hikrobotics.solution.module.alarm.config;

import java.util.List;
import org.assertj.core.util.Lists;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DataupLoad alarm 报警配置（沿用 PSM DefectAlarmConfig 语义）。
 * <p>
 * 配置项形如：
 * <pre>
 * alarm:
 *   global-enabled: true           # W-X21 全局开关（默认 true）；false 时 AlarmRecordServiceImpl.add() 直接 return
 *   config:
 *     - type: defect
 *       template: '(?&lt;=\[)[^]]+(?=\])'
 *     - type: system
 *       template: '^([^。]*)'
 * </pre>
 *
 * <h2>W-X21 全局开关（紧急可一键关停）</h2>
 * <p>
 * PM 锋卫 2026-07-23 14:25 派工：增加 {@code alarm.global-enabled} 顶层配置项（默认 {@code true}）。
 * 当 {@code false} 时 {@code AlarmRecordServiceImpl.add()} 入口即 return {@code BaseResult.build().ok()}，
 * 报警既不落 PG 也不推 yk / WebSocket；用于老板紧急关停（老板指令时 PM 派工单改 false）。
 */
@Component
@ConfigurationProperties("alarm")
public class DefectAlarmConfig {
   /**
    * W-X21 全局开关（默认 true）。false 时报警入口直接 return，不落库不推送。
    * <p>对应配置 {@code alarm.global-enabled}。
    */
   private boolean globalEnabled = true;
   private List<DefectTypeConfig> config = Lists.newArrayList();

   public boolean isGlobalEnabled() {
      return this.globalEnabled;
   }

   public void setGlobalEnabled(boolean globalEnabled) {
      this.globalEnabled = globalEnabled;
   }

   public List<DefectTypeConfig> getConfig() {
      return this.config;
   }

   public void setConfig(List<DefectTypeConfig> config) {
      this.config = config;
   }

   public static class DefectTypeConfig {
      private String type;
      private String template;
      private List<String> names = Lists.newArrayList();

      public String getType() {
         return this.type;
      }

      public String getTemplate() {
         return this.template;
      }

      public List<String> getNames() {
         return this.names;
      }

      public void setType(String type) {
         this.type = type;
      }

      public void setTemplate(String template) {
         this.template = template;
      }

      public void setNames(List<String> names) {
         this.names = names;
      }
   }
}
