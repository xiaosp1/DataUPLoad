package com.hikrobotics.solution.common;

public class MathUtils {
    public static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    public static double divide(int dividend, int divisor) {
        if (divisor == 0) return 0;
        return (double) dividend / divisor;
    }

    public static int percent(int part, int total) {
        if (total == 0) return 0;
        return (int) Math.round((double) part / total * 100);
    }
}
