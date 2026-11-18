package com.algoprep.harness.application.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Adapter-specific command templates, without stdin, limits or cached file identifiers. */
public record JudgePayload(String engine, Map<String, Object> body, String artifact) {
  public JudgePayload {
    body = Collections.unmodifiableMap(new LinkedHashMap<>(body));
  }
}
