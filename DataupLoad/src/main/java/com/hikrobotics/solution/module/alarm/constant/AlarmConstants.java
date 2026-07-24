package com.hikrobotics.solution.module.alarm.constant;

/**
 * DataupLoad alarm 常量（沿用 PSM AlarmConstants 字段 + W-ALM-05 增补）。
 *
 * <h2>W-ALM-05 字段</h2>
 * <ul>
 *   <li>{@link #SOUND_PLAY_COUNT_CFG_KEY} —— sound 播放次数配置键（PSM 同款，对应 system_config.config_key）</li>
 *   <li>{@link #SOUND_PLAY_INTERVAL_CFG_KEY} —— sound 播放间隔配置键（任务声明 PSM 同款；
 *       PSM 反编译产物未直接出现该 key，但大屏前端需要 interval 字段控制轮播节奏，按任务要求补齐）</li>
 *   <li>{@link #SOUND_PLAY_DEFAULT_URI} / {@link #SOUND_PLAY_DEFAULT_INTERVAL_SECONDS} / 
 *       {@link #SOUND_PLAY_DEFAULT_COUNT} —— ISystemConfigService 未启用时的兜底值
 *       （DataupLoad 当前未启用 system_config 模块，无法像 PSM 那样从 DB 取值；WS 声音推送链路
 *       必须能用常数兜底跑通）</li>
 * </ul>
 *
 * <h2>已知限制</h2>
 * PSM {@code sendAlarmSoundWsMessage} 通过 {@code ISystemConfigService} 读取 uri/count；
 * DataupLoad 不引入该组件（任务约束：不要修改其它模块），所以 uri/interval 取硬编码默认值。
 */
public class AlarmConstants {
    /** PSM 同款：system_config.config_key = 'sound_play_count'，控制 sound 播放次数 */
    public static final String SOUND_PLAY_COUNT_CFG_KEY = "sound_play_count";

    /**
     * W-ALM-05 新增：sound 播放间隔（秒）。
     * <p>
     * 任务声明"PSM 同款"，但 PSM 反编译产物里没有这个 key——按任务简报的字面要求补齐，
     * 便于后续接 ISystemConfigService 时不用再改常量名。
     */
    public static final String SOUND_PLAY_INTERVAL_CFG_KEY = "sound_play_interval";

    /**
     * WS 推送默认 uri（DPL 兜底值）。
     * <p>
     * PSM 同款字段在 system_config 表里（key = defect_alarm_sound_uri / system_alarm_sound_uri /
     * device_alarm_sound_uri，按 AlarmTypeEnum.soundConfigKey 取）；DPL 没启用 system_config，
     * 这里用静态资源相对路径兜底，前端大屏可直接拼 host 拿。
     */
    public static final String SOUND_PLAY_DEFAULT_URI = "sound/alarm.wav";

    /** WS 推送默认播放间隔 5 秒（任务简报：interval 5s） */
    public static final Integer SOUND_PLAY_DEFAULT_INTERVAL_SECONDS = 5;

    /** WS 推送默认播放次数（PSM 从 system_config 取；DPL 用常数） */
    public static final Integer SOUND_PLAY_DEFAULT_COUNT = 1;
}
