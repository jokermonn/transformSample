package com.joker.hunter;

import java.util.Map;

public final class HunterRegistry {
  private static Map<Class<?>, Object> services;

  //{
  //  services.put(, );
  //}

  private HunterRegistry() {

  }

  @SuppressWarnings("unchecked") public static <T> T get(Class<T> key) {
    return (T) services.get(key);
  }
}
