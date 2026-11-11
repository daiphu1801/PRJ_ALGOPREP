package com.algoprep.aireview.application.command;

import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Validates and normalises the LLM answer of a Solution Review (DD validation F5 section 3 and
 * BR-AI-02 steps 9 and 10). Wrong shape is rejected with AIR-502; small excesses are cut or
 * overwritten quietly. The result JSON never contains the readability score (own column).
 */
final class SolutionReviewNormalizer {
  /** Normalised JSON text plus the extracted readability score. */
  record Normalized(String json, int readabilityScore) {}

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final Set<String> VERDICTS = Set.of("OPTIMAL", "IMPROVABLE", "NOT_OPTIMAL");
  private static final int MAX_ITEMS = 10;

  private SolutionReviewNormalizer() {}

  static Normalized normalize(String rawJson, String schemaVersion, boolean optimumKnown) {
    JsonNode root;
    try {
      root = MAPPER.readTree(rawJson == null ? "" : rawJson);
    } catch (RuntimeException failure) {
      throw new AiInvalidResponseException("LLM output is not JSON", failure);
    }
    if (root == null || !root.isObject()) {
      throw new AiInvalidResponseException("LLM output is not a JSON object");
    }
    String verdict = text(root, "verdict", true, 20);
    if (!VERDICTS.contains(verdict)) {
      throw new AiInvalidResponseException("verdict is not one of the allowed values");
    }
    JsonNode cx = root.get("complexity");
    if (cx == null || !cx.isObject()) {
      throw new AiInvalidResponseException("complexity is missing");
    }
    int readability = readability(root.get("readabilityScore"));

    Map<String, Object> complexity = new LinkedHashMap<>();
    complexity.put("actualTime", text(cx, "actualTime", true, 30));
    complexity.put("optimalTime", optimumKnown ? text(cx, "optimalTime", false, 30) : null);
    complexity.put("timeExplanation", text(cx, "timeExplanation", true, 500));
    complexity.put("actualSpace", text(cx, "actualSpace", true, 30));
    complexity.put("optimalSpace", optimumKnown ? text(cx, "optimalSpace", false, 30) : null);
    complexity.put("spaceExplanation", text(cx, "spaceExplanation", true, 500));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("schemaVersion", schemaVersion);
    out.put("approachTitle", text(root, "approachTitle", true, 60));
    out.put("verdict", verdict);
    out.put("isApproachOptimal", "OPTIMAL".equals(verdict));
    out.put("complexity", complexity);
    out.put("strengths", list(root, "strengths", 2000));
    out.put("improvements", list(root, "improvements", 2000));
    out.put("edgeCases", list(root, "edgeCases", 2000));
    out.put("followUpTopics", list(root, "followUpTopics", 100));
    out.put("codeDiff", "OPTIMAL".equals(verdict) ? null : codeDiff(root.get("codeDiff")));
    return new Normalized(MAPPER.writeValueAsString(out), readability);
  }

  private static String text(JsonNode node, String key, boolean required, int max) {
    JsonNode value = node.get(key);
    if (value == null || value.isNull()) {
      if (required) {
        throw new AiInvalidResponseException(key + " is missing");
      }
      return null;
    }
    if (!value.isString() || value.asString().isBlank()) {
      if (required) {
        throw new AiInvalidResponseException(key + " must be a non-empty string");
      }
      return null;
    }
    return cut(value.asString(), max);
  }

  private static int readability(JsonNode value) {
    if (value == null || !value.isIntegralNumber()) {
      throw new AiInvalidResponseException("readabilityScore must be an integer");
    }
    int score = value.asInt();
    if (score < 1 || score > 5) {
      throw new AiInvalidResponseException("readabilityScore is outside 1..5");
    }
    return score;
  }

  private static List<String> list(JsonNode node, String key, int max) {
    JsonNode value = node.get(key);
    List<String> out = new ArrayList<>();
    if (value == null || value.isNull()) {
      return out;
    }
    if (!value.isArray()) {
      throw new AiInvalidResponseException(key + " must be an array");
    }
    for (JsonNode item : value) {
      if (!item.isString()) {
        throw new AiInvalidResponseException(key + " must contain strings only");
      }
      if (out.size() < MAX_ITEMS) {
        out.add(cut(item.asString(), max));
      }
    }
    return out;
  }

  /** A broken diff never rejects the review; it just drops the optional part. */
  private static Map<String, Object> codeDiff(JsonNode diff) {
    if (diff == null || !diff.isObject()) {
      return null;
    }
    JsonNode code = diff.get("suggestedCode");
    JsonNode why = diff.get("explanation");
    if (code == null
        || !code.isString()
        || code.asString().isBlank()
        || code.asString().length() > 50_000
        || why == null
        || !why.isString()
        || why.asString().isBlank()) {
      return null;
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("suggestedCode", code.asString());
    out.put("explanation", cut(why.asString(), 1000));
    return out;
  }

  private static String cut(String value, int max) {
    return value.length() <= max ? value : value.substring(0, max);
  }
}
