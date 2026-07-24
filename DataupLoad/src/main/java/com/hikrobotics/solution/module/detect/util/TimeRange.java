package com.hikrobotics.solution.module.detect.util;

import java.time.LocalDateTime;

/**
 * 时间范围游标类 — 1:1 抄自 PSM 反编译产物
 * {@code com.hikrobotics.solution.module.detect.util.TimeRange}。
 * <p>
 * 使用模式：构造时传入 start / end / pattern，
 * 循环中先 {@link #hasNext()} 再 {@link #next()}（next 返回当前游标后将其按 pattern 步进）。
 * 末尾 {@link #init()} 可重置游标回 start 以便复用同一个实例遍历多次（见 PSM LineServiceImpl#planPanel 三段循环）。
 *
 * @see com.hikrobotics.solution.framework.util.TimeRangeUtil DataupLoad framework 自带的批量切片工具（返回 {@code List<TimeRangeUtil.TimeRange>}，API 不同）。
 */
public class TimeRange {

    /**
     * 时间维度枚举（1:1 抄 PSM {@code TimeRange$TimePattern}）。
     * <p>
     * 源顺序依据 PSM 反编译 {@code next()} 内 switch-case 推导：
     * ordinal 0 / 1 → plusDays（DAY 步进，两种 day 粒度均落到同一天步进逻辑上），
     * ordinal 2 → plusMonths（MONTH 步进），
     * ordinal 3 → plusHours（HOUR 步进）。
     * <p>
     * {@link #getDesc()} 返回的字符串可直接作为 {@code HikDateUtil.formatLocalDate(LocalDateTime, String)} 的 pattern 参数。
     */
    public enum TimePattern {
        /** 日步进，{@code getDesc() = "yyyy-MM-dd"}（对应 {@code HikDateUtil.simplePattern}）。 */
        YYYY_MM_DD("yyyy-MM-dd"),
        /** 日步进（另一种日期表示），{@code getDesc() = "MM-dd"}。 */
        MM_DD("MM-dd"),
        /** 月步进，{@code getDesc() = "yyyy-MM"}（对应 {@code HikDateUtil.YEAR_MONTH}）。 */
        YYYY_MM("yyyy-MM"),
        /** 小时步进，{@code getDesc() = "yyyy-MM-dd HH"}。 */
        HH("yyyy-MM-dd HH");

        private final String desc;

        TimePattern(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return this.desc;
        }
    }

    private final TimePattern pattern;
    private final LocalDateTime start;
    private final LocalDateTime end;
    /**
     * 当前游标位置（mutable）。{@link #hasNext()} 以 {@code current.isBefore(end)} 为判断条件；
     * {@link #next()} 返回 {@code current} 后按 {@link #pattern} 步进。
     */
    private LocalDateTime current;

    public TimeRange(LocalDateTime start, LocalDateTime end, TimePattern pattern) {
        this.pattern = pattern;
        this.end = end;
        this.start = start;
        this.current = start;
    }

    public String getPattern() {
        return this.pattern.getDesc();
    }

    public boolean hasNext() {
        return this.current.isBefore(this.end);
    }

    public LocalDateTime next() {
        LocalDateTime result = this.current;
        switch (this.pattern) {
            case YYYY_MM_DD:
            case MM_DD:
                this.current = this.current.plusDays(1L);
                break;
            case YYYY_MM:
                this.current = this.current.plusMonths(1L);
                break;
            case HH:
                this.current = this.current.plusHours(1L);
                break;
        }
        return result;
    }

    public void init() {
        this.current = this.start;
    }

    public LocalDateTime getStart() {
        return this.start;
    }

    public LocalDateTime getEnd() {
        return this.end;
    }
}
