package com.algoprep.aireview.application.command;

import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Schema text for the provider and the strict check of its answer (DD validation F5 section 6). */
final class TestcaseScriptSchema {
  static final String NAME = "testcase_script";
  static final String JSON =
      """
      {"type":"object","required":["generatorScript","validatorScript"],"properties":{\
      "generatorScript":{"type":"string","minLength":1},\
      "validatorScript":{"type":"string","minLength":1},\
      "categoryLabels":{"type":"array","items":{"type":"string"}}}}""";

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  record Parsed(String generatorScript, String validatorScript, List<String> categoryLabels) {}

  private TestcaseScriptSchema() {}

  static Parsed parse(String rawJson) {
    JsonNode root;
    try {
      root = MAPPER.readTree(rawJson == null ? "" : rawJson);
    } catch (RuntimeException failure) {
      throw new AiInvalidResponseException("LLM output is not JSON", failure);
    }
    if (root == null || !root.isObject()) {
      throw new AiInvalidResponseException("LLM output is not a JSON object");
    }
    List<String> labels = new ArrayList<>();
    JsonNode raw = root.get("categoryLabels");
    if (raw != null && !raw.isNull()) {
      if (!raw.isArray()) {
        throw new AiInvalidResponseException("categoryLabels is not an array");
      }
      for (JsonNode one : raw) {
        if (!one.isString()) {
          throw new AiInvalidResponseException("categoryLabels holds a non-string item");
        }
        labels.add(one.asString());
      }
    }
    return new Parsed(script(root, "generatorScript"), script(root, "validatorScript"), labels);
  }

  private static String script(JsonNode root, String key) {
    JsonNode node = root.get(key);
    if (node == null || !node.isString() || node.asString().isBlank()) {
      throw new AiInvalidResponseException(key + " is missing or not a non-empty string");
    }
    return node.asString();
  }
}
