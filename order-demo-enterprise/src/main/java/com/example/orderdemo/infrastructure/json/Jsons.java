package com.example.orderdemo.infrastructure.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class Jsons {
  private Jsons() {}

  private static final ObjectMapper OM = new ObjectMapper().registerModule(new JavaTimeModule());

  public static String toJson(Object o) {
    try {
      return OM.writeValueAsString(o);
    } catch (Exception e) {
      throw new IllegalArgumentException("json serialize failed: " + e.getMessage(), e);
    }
  }

  public static <T> T fromJson(String json, Class<T> type) {
    try {
      return OM.readValue(json, type);
    } catch (Exception e) {
      throw new IllegalArgumentException("json parse failed: " + e.getMessage(), e);
    }
  }

  public static ObjectMapper mapper() {
    return OM;
  }
}
