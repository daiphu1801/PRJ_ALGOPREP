package com.algoprep.identity.application.command;

import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Free-form audit snapshots of role changes; never carries personal data (BR-19). */
final class RbacAuditJson {
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private RbacAuditJson() {}

  /** Builds a JSON object from alternating keys and values, keeping their order. */
  static String of(Object... keysAndValues) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      map.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return MAPPER.writeValueAsString(map);
  }
}
