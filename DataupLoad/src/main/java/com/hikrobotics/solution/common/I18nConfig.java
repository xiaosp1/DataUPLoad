package com.hikrobotics.solution.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.Locale;
import java.util.ResourceBundle;

@Configuration
public class I18nConfig {
    private static final String BASE_NAME = "i18n.messages";

    public static String getMessage(String key) {
        ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, LocaleContextHolder.getLocale());
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    public static String getMessage(String key, Object... args) {
        String msg = getMessage(key);
        return String.format(msg, args);
    }
}
