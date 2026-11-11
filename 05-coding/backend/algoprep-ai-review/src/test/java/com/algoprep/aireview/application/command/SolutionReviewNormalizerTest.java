package com.algoprep.aireview.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SolutionReviewNormalizerTest {
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private static String valid(String verdict, String extra) {
    return """
        {"approachTitle":"Hash map","verdict":"%s","isApproachOptimal":false,
         "complexity":{"actualTime":"O(n)","optimalTime":"O(1)","timeExplanation":"one pass",
         "actualSpace":"O(n)","optimalSpace":"O(1)","spaceExplanation":"map"},
         "strengths":["a"],"improvements":["b"],"edgeCases":[],"followUpTopics":["dp"],
         "readabilityScore":4,
         "codeDiff":{"suggestedCode":"int x;","explanation":"why"}%s}
        """
        .formatted(verdict, extra);
  }

  private static JsonNode run(String json, boolean optimum) {
    return MAPPER.readTree(SolutionReviewNormalizer.normalize(json, "v1", optimum).json());
  }

  private static void rejected(String json) {
    assertThatThrownBy(() -> SolutionReviewNormalizer.normalize(json, "v1", false))
        .isInstanceOf(AiInvalidResponseException.class);
  }

  @Test
  void aValidAnswerIsNormalisedAndTheScoreLeavesTheJson() {
    var result = SolutionReviewNormalizer.normalize(valid("IMPROVABLE", ""), "v1", false);
    JsonNode json = MAPPER.readTree(result.json());
    assertThat(result.readabilityScore()).isEqualTo(4);
    assertThat(json.has("readabilityScore")).isFalse();
    assertThat(json.get("schemaVersion").asString()).isEqualTo("v1");
    assertThat(json.get("isApproachOptimal").asBoolean()).isFalse();
    assertThat(json.get("codeDiff").get("suggestedCode").asString()).isEqualTo("int x;");
  }

  @Test
  void theVerdictWinsOverAContradictingOptimalFlagAndOptimalDropsTheDiff() {
    JsonNode json = run(valid("OPTIMAL", ""), false);
    assertThat(json.get("isApproachOptimal").asBoolean()).isTrue();
    assertThat(json.get("codeDiff").isNull()).isTrue();
  }

  @Test
  void optimumValuesAreDroppedWhenNoOptimumIsKnownAndKeptWhenItIs() {
    assertThat(run(valid("IMPROVABLE", ""), false).get("complexity").get("optimalTime").isNull())
        .isTrue();
    assertThat(run(valid("IMPROVABLE", ""), true).get("complexity").get("optimalTime").asString())
        .isEqualTo("O(1)");
  }

  @Test
  void unknownKeysAreDropped() {
    JsonNode json =
        run(valid("IMPROVABLE", ",\"criteriaScores\":{\"a\":1},\"summaryText\":\"x\""), false);
    assertThat(json.has("criteriaScores")).isFalse();
    assertThat(json.has("summaryText")).isFalse();
  }

  @Test
  void longTextsAndListsAreCutQuietly() {
    String longTitle = "t".repeat(80);
    String items = "[" + "\"x\",".repeat(12) + "\"y\"]";
    String json =
        valid("IMPROVABLE", "")
            .replace("Hash map", longTitle)
            .replace("\"strengths\":[\"a\"]", "\"strengths\":" + items);
    JsonNode out = run(json, false);
    assertThat(out.get("approachTitle").asString()).hasSize(60);
    assertThat(out.get("strengths")).hasSize(10);
  }

  @Test
  void aMissingListIsEmptyButAWrongTypeIsRejected() {
    String missing = valid("IMPROVABLE", "").replace("\"edgeCases\":[],", "");
    assertThat(run(missing, false).get("edgeCases")).isEmpty();
    rejected(valid("IMPROVABLE", "").replace("\"edgeCases\":[]", "\"edgeCases\":\"none\""));
    rejected(valid("IMPROVABLE", "").replace("\"edgeCases\":[]", "\"edgeCases\":[1]"));
  }

  @Test
  void aBrokenOrOversizedDiffIsDroppedNotRejected() {
    String noWhy = valid("IMPROVABLE", "").replace("\"explanation\":\"why\"", "\"x\":1");
    assertThat(run(noWhy, false).get("codeDiff").isNull()).isTrue();
    String huge = valid("IMPROVABLE", "").replace("int x;", "x".repeat(50_001));
    assertThat(run(huge, false).get("codeDiff").isNull()).isTrue();
    String notObject =
        valid("IMPROVABLE", "").replaceAll("\"codeDiff\":\\{[^}]*\\}", "\"codeDiff\":3");
    assertThat(run(notObject, false).get("codeDiff").isNull()).isTrue();
  }

  @Test
  void readabilityMustBeAnIntegerFromOneToFive() {
    rejected(valid("IMPROVABLE", "").replace("\"readabilityScore\":4", "\"readabilityScore\":7"));
    rejected(valid("IMPROVABLE", "").replace("\"readabilityScore\":4", "\"readabilityScore\":0"));
    rejected(valid("IMPROVABLE", "").replace("\"readabilityScore\":4", "\"readabilityScore\":3.5"));
    rejected(valid("IMPROVABLE", "").replace("\"readabilityScore\":4,", ""));
    rejected(
        valid("IMPROVABLE", "").replace("\"readabilityScore\":4", "\"readabilityScore\":\"4\""));
  }

  @Test
  void requiredFieldsAndShapeAreRejected() {
    rejected("not json");
    rejected("[1]");
    rejected(null);
    rejected(valid("GREAT", ""));
    rejected(
        valid("IMPROVABLE", "")
            .replace("\"approachTitle\":\"Hash map\"", "\"approachTitle\":\" \""));
    rejected(valid("IMPROVABLE", "").replace("\"actualTime\":\"O(n)\",", ""));
    rejected(valid("IMPROVABLE", "").replace("\"complexity\":", "\"cx\":"));
  }
}
