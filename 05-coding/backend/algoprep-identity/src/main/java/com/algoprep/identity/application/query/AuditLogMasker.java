package com.algoprep.identity.application.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * DD BR-16: the one place that hides sensitive values of an audit snapshot. Every key that matches
 * {@code (?i)password|secret|token} (which also covers {@code password_hash} and {@code
 * token_hash}) gets the value {@code "***"} at any depth; the rest of the structure stays. Counter
 * and budget keys such as {@code maxTokens} or {@code token_budget} are not secrets and stay
 * visible (owner decision 2026-11-01); the exception list is deliberately narrow, so a new key that
 * merely contains "token" is still hidden until someone adds it here.
 */
final class AuditLogMasker {
  static final String MASK = "***";

  private static final Pattern SENSITIVE_KEY = Pattern.compile("(?i)password|secret|token");
  private static final Pattern ALWAYS_SENSITIVE_KEY = Pattern.compile("(?i)password|secret");
  private static final Pattern COUNTER_KEY =
      Pattern.compile(
          "(?i)(max|total|input|output|prompt|completion|budget|remaining)[_-]?tokens"
              + "|tokens?[_-]?(budget|limit|count|used|usage)");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<Map<String, Object>> OBJECT = new TypeReference<>() {};

  private AuditLogMasker() {}

  /**
   * Parameters a sentence template may use: the masked {@code after_json} object, or the masked
   * {@code before_json} object when there is no after snapshot (a deletion keeps its facts in
   * "before"). Anything that is not a JSON object yields an empty map. {@code [SoT: Suy luan]}, the
   * DD only says the parameters come from {@code after_json}.
   */
  static Map<String, Object> safeParams(String beforeJson, String afterJson) {
    Map<String, Object> after = parseObject(afterJson);
    Map<String, Object> source = after != null ? after : parseObject(beforeJson);
    if (source == null) {
      return Map.of();
    }
    return mask(source);
  }

  private static Map<String, Object> parseObject(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return JSON.readValue(json, OBJECT);
    } catch (JacksonException notAnObject) {
      return null;
    }
  }

  private static Map<String, Object> mask(Map<?, ?> object) {
    Map<String, Object> masked = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : object.entrySet()) {
      String key = String.valueOf(entry.getKey());
      masked.put(key, isSensitive(key) ? MASK : maskValue(entry.getValue()));
    }
    return masked;
  }

  private static boolean isSensitive(String key) {
    if (ALWAYS_SENSITIVE_KEY.matcher(key).find()) {
      return true;
    }
    return SENSITIVE_KEY.matcher(key).find() && !COUNTER_KEY.matcher(key).find();
  }

  private static Object maskValue(Object value) {
    if (value instanceof Map<?, ?> nested) {
      return mask(nested);
    }
    if (value instanceof List<?> list) {
      List<Object> masked = new ArrayList<>(list.size());
      for (Object element : list) {
        masked.add(maskValue(element));
      }
      return masked;
    }
    return value;
  }
}
