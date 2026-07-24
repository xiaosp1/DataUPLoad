package com.hikrobotics.solution.module.yingke.config;

import cn.hutool.core.util.URLUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * yingke 配置（沿用 PSM YKConfig 1:1）。
 * <p>
 * 实际配置形如：
 * <pre>
 * yk:
 *   loginEnabled: true
 *   uploadEnabled: false
 *   url: http://192.168.80.33:10031/api/dataportal/invoke
 *   username: HKSJSB
 *   password: HKSJSB123
 *   login-interval: 50
 *   workshop: QZN2
 * </pre>
 * <p>
 * <b>W-X13d 灰盒拆双开关</b>：
 * <ul>
 *   <li>{@link #loginEnabled} —— 是否调 MES AuthenticationController.Login 拿 ticket / 续约（凭证预热）</li>
 *   <li>{@link #uploadEnabled} —— 报警来了是否真推 yk 到 MES</li>
 *   <li>灰盒默认 {@code loginEnabled=true, uploadEnabled=false}：ticket 预热、推送静默，正式上线改 {@code uploadEnabled=true}</li>
 * </ul>
 */
@Component
@ConfigurationProperties("yk")
public class YKConfig {
   /**
    * 老字段，@Deprecated 保留 0.x 版本平滑过渡。getter 走 {@code loginEnabled || uploadEnabled} 兼容语义。
    * 新配置请直接用 {@link #loginEnabled} / {@link #uploadEnabled}。
    */
   @Deprecated
   private boolean enable;
   /** 灰盒拆双开关：是否调 MES AuthenticationController.Login 拿 / 续 ticket（凭证预热，默认开）。 */
   private boolean loginEnabled = true;
   /** 灰盒拆双开关：报警来了是否真推 yk 到 MES（灰盒默认关 / 上线改 true）。 */
   private boolean uploadEnabled = false;
   private String url;
   private String username;
   private String password;
   private Integer loginInterval;
   private String workshop;
   private boolean searchRemove = true;

   public String getBaseUrl() {
      if (StringUtils.isBlank(this.url)) {
         throw new IllegalArgumentException("yk url should not be null");
      }
      return this.url.replace(this.getUri(), "");
   }

   public String getUri() {
      if (StringUtils.isBlank(this.url)) {
         throw new IllegalArgumentException("yk url should not be null");
      }
      return URLUtil.toURI(this.url).getPath();
   }

   /**
    * 老字段 getter（W-X13d 兼容语义）：loginEnabled 或 uploadEnabled 任一为 true 即视为 true。
    * 新代码请直接调 {@link #isLoginEnabled()} / {@link #isUploadEnabled()}。
    */
   @Deprecated
   public boolean isEnable() {
      return this.loginEnabled || this.uploadEnabled;
   }

   public boolean isLoginEnabled() {
      return this.loginEnabled;
   }

   public boolean isUploadEnabled() {
      return this.uploadEnabled;
   }

   public String getUrl() {
      return this.url;
   }

   public String getUsername() {
      return this.username;
   }

   public String getPassword() {
      return this.password;
   }

   public Integer getLoginInterval() {
      return this.loginInterval;
   }

   public String getWorkshop() {
      return this.workshop;
   }

   public boolean isSearchRemove() {
      return this.searchRemove;
   }

   /**
    * 老字段 setter 保留为兼容性入口。新代码请直接调 {@link #setLoginEnabled(boolean)} /
    * {@link #setUploadEnabled(boolean)}。
    */
   @Deprecated
   public void setEnable(boolean enable) {
      this.enable = enable;
   }

   public void setLoginEnabled(boolean loginEnabled) {
      this.loginEnabled = loginEnabled;
   }

   public void setUploadEnabled(boolean uploadEnabled) {
      this.uploadEnabled = uploadEnabled;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public void setLoginInterval(Integer loginInterval) {
      this.loginInterval = loginInterval;
   }

   public void setWorkshop(String workshop) {
      this.workshop = workshop;
   }

   public void setSearchRemove(boolean searchRemove) {
      this.searchRemove = searchRemove;
   }
}
