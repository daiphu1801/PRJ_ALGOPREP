package com.algoprep.aireview.application.command;

import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.model.RubricCriterion;
import com.algoprep.aireview.domain.model.RubricScore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Schema text for the provider and the strict check of the rubric answer (DD validation F5 section
 * 5, BR-AI-09 step 5). Anything off is AIR-502; an out-of-range score is rejected, never clamped.
 */
final class RubricSchema {
  static final String NAME = "interview_rubric";
  static final int COMMENT_MAX = 1000;
  static final String JSON =
      """
      {"type":"object","required":["criteria"],"properties":{"criteria":{"type":"array",\
      "minItems":4,"maxItems":4,"items":{"type":"object","required":["code","score","comment"],\
      "properties":{"code":{"type":"string","enum":["CLARITY","TECHNICAL_ACCURACY",\
      "PUSHBACK_HANDLING","COMPLEXITY_AWARENESS"]},"score":{"type":"number","minimum":0,\
      "maximum":10},"comment":{"type":"string","minLength":1}}}}}}""";

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final BigDecimal MAX_SCORE = BigDecimal.TEN;

  private RubricSchema() {}

  /** The four scores in the fixed criterion order, scores at two decimals, comments trimmed. */
  static List<RubricScore> parse(String rawJson) {
    JsonNode root;
    try {
      root = MAPPER.readTree(rawJson == null ? "" : rawJson);
    } catch (RuntimeException failure) {
      throw new AiInvalidResponseException("Rubric output is not JSON", failure);
    }
    JsonNode criteria = root == null ? null : root.get("criteria");
    if (criteria == null || !criteria.isArray() || criteria.size() != 4) {
      throw new AiInvalidResponseException("Rubric needs exactly four criteria");
    }
    Map<RubricCriterion, RubricScore> byCode = new EnumMap<>(RubricCriterion.class);
    for (JsonNode one : criteria) {
      RubricScore score = one(one);
      if (byCode.put(score.criterion(), score) != null) {
        throw new AiInvalidResponseException("Rubric repeats a criterion");
      }
    }
    List<RubricScore> ordered = new ArrayList<>();
    for (RubricCriterion criterion : RubricCriterion.values()) {
      ordered.add(byCode.get(criterion));
    }
    return ordered;
  }

  private static RubricScore one(JsonNode node) {
    JsonNode code = node.get("code");
    JsonNode score = node.get("score");
    JsonNode comment = node.get("comment");
    if (code == null || !code.isString() || score == null || !score.isNumber()) {
      throw new AiInvalidResponseException("Rubric criterion has no code or score");
    }
    RubricCriterion criterion;
    try {
      criterion = RubricCriterion.valueOf(code.asString());
    } catch (IllegalArgumentException unknown) {
      throw new AiInvalidResponseException("Rubric criterion code is unknown", unknown);
    }
    BigDecimal value = score.decimalValue().setScale(2, RoundingMode.HALF_UP);
    if (value.signum() < 0 || value.compareTo(MAX_SCORE) > 0) {
      throw new AiInvalidResponseException("Rubric score is outside 0 to 10");
    }
    if (comment == null || !comment.isString() || comment.asString().isBlank()) {
      throw new AiInvalidResponseException("Rubric comment is empty");
    }
    String text = comment.asString().strip();
    return new RubricScore(
        criterion, value, text.length() > COMMENT_MAX ? text.substring(0, COMMENT_MAX) : text);
  }
}
