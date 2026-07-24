package com.hikrobotics.solution.common.utils;

import java.util.function.Function;
import org.apache.commons.lang3.EnumUtils;

public class EnumUtil {
   public static <T extends Enum<T>, R> T getEnumByValue(R value, Function<T, ? extends R> getter, Class<T> enumClazz) {
      for (T anEnum : EnumUtils.getEnumList(enumClazz)) {
         R tempValue = (R)getter.apply(anEnum);
         if (tempValue.equals(value)) {
            return anEnum;
         }
      }

      return null;
   }
}
