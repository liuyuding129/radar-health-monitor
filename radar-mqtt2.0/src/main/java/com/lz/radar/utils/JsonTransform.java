package com.lz.radar.utils;

import com.google.common.base.Strings;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class JsonTransform {

  /*public static <T, tClass> List<T> build(String data, T tClass) {
    if (Strings.isNullOrEmpty(data)) {
      return null;
    }

    if (null == tClass) {
      return null;
    }

    List<T> result = JsonUtils.fromJson(data, new TypeToken<List<tClass>>() {}.getType());
    return result;
  }*/

  public static <T> List<T> build(String data, Class<T> clazz) {
    if (Strings.isNullOrEmpty(data)) {
      return null;
    }

    if (null == clazz) {
      return null;
    }

    Type type = TypeToken.getParameterized(List.class, clazz).getType();
    return JsonUtils.fromJson(data, type);
  }

}
