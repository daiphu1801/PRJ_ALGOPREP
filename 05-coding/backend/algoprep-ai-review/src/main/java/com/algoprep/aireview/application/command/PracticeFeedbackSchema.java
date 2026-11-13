package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.PracticeFeedback;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Schema text for the provider and the strict check of its answer against the rubric snapshot (DD
 * validation F6 section 7). Exactly one criteria element per snapshot code, no duplicate, no extra;
 * the code matches case-insensitively and is replaced by the snapshot string. Unknown keys are
 * dropped. The rubric text sent to the model has {@code <} and {@code >} escaped, so the escaped
 * form of a code is accepted as well.
 */
final class PracticeFeedbackSchema {
  static final String NAME = "practice_feedback";
  static final String JSON =
      """
      {"type":"object","required":["criteria"],"properties":{\
      "achievedPoints":{"type":"array","items":{"type":"string"}},\
      "missingPoints":{"type":"array","items":{"type":"string"}},\
      "improvementSuggestions":{"type":"array","items":{"type":"string"}},\
      "criteria":{"type":"array","items":{"type":"object",\
      "required":["criterionCode","achievementPercent"],"properties":{\
      "criterionCode":{"type":"string"},\
      "achievementPercent":{"type":"integer","minimum":0,"maximum":100}}}}}}""";

  static final int MAX_POINTS = 10;
  static final int MAX_POINT_CHARS = 500;
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private PracticeFeedbackSchema() {}

  static String escape(String text) {
    return text.replace("<", "&lt;").replace(">", "&gt;");
  }

  static PracticeFeedback.Output parse(
      String rawJson, List<PracticeFeedback.Criterion> rubric, String modelName) {
    JsonNode root;
    try {
      root = MAPPER.readTree(rawJson == null ? "" : rawJson);
    } catch (RuntimeException failure) {
      throw new AiInvalidResponseException("LLM output is not JSON", failure);
    }
    if (root == null || !root.isObject()) {
      throw new AiInvalidResponseException("LLM output is not a JSON object");
    }
    return new PracticeFeedback.Output(
        points(root, "achievedPoints"),
        points(root, "missingPoints"),
        points(root, "improvementSuggestions"),
        criteria(root.get("criteria"), rubric),
        modelName);
  }

  private static List<String> points(JsonNode root, String key) {
    JsonNode node = root.get(key);
    List<String> out = new ArrayList<>();
    if (node == null || node.isNull()) {
      return out;
    }
    if (!node.isArray()) {
      throw new AiInvalidResponseException(key + " is not an array");
    }
    for (JsonNode one : node) {
      if (!one.isString()) {
        throw new AiInvalidResponseException(key + " holds a non-string item");
      }
      if (out.size() < MAX_POINTS) {
        String text = one.asString();
        out.add(text.length() > MAX_POINT_CHARS ? text.substring(0, MAX_POINT_CHARS) : text);
      }
    }
    return out;
  }

  private static List<PracticeFeedback.Scored> criteria(
      JsonNode node, List<PracticeFeedback.Criterion> rubric) {
    if (node == null || !node.isArray()) {
      throw new AiInvalidResponseException("criteria is missing or not an array");
    }
    Map<String, String> canonical = new HashMap<>();
    for (PracticeFeedback.Criterion c : rubric) {
      canonical.put(c.criterionCode().toLowerCase(Locale.ROOT), c.criterionCode());
      canonical.putIfAbsent(escape(c.criterionCode()).toLowerCase(Locale.ROOT), c.criterionCode());
    }
    Map<String, Integer> byCode = new HashMap<>();
    Set<String> seen = new HashSet<>();
    for (JsonNode one : node) {
      JsonNode code = one.get("criterionCode");
      JsonNode percent = one.get("achievementPercent");
      if (code == null || !code.isString() || percent == null || !percent.isIntegralNumber()) {
        throw new AiInvalidResponseException("a criteria element has a bad code or percent");
      }
      String exact = canonical.get(code.asString().strip().toLowerCase(Locale.ROOT));
      int value = percent.canConvertToInt() ? percent.intValue() : -1;
      if (exact == null || value < 0 || value > 100 || !seen.add(exact)) {
        throw new AiInvalidResponseException(
            "a criteria element is unknown, duplicated or out of range");
      }
      byCode.put(exact, value);
    }
    if (seen.size() != rubric.size()) {
      throw new AiInvalidResponseException("criteria does not cover every rubric criterion");
    }
    List<PracticeFeedback.Scored> out = new ArrayList<>();
    for (PracticeFeedback.Criterion c : rubric) {
      out.add(new PracticeFeedback.Scored(c.criterionCode(), byCode.get(c.criterionCode())));
    }
    return out;
  }
}
