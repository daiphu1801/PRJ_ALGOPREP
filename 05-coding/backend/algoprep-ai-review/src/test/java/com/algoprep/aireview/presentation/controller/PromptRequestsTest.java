package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.FeatureCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class PromptRequestsTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String GUARDS =
      "{\"citeLines\":true,\"answerInVietnamese\":true,\"noFullSolution\":false}";

  private static JsonNode json(String text) {
    return JSON.readTree(text);
  }

  private static void code(Runnable action, String code) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            AiInvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void featureCodeMustBeOneOfTheThree() {
    assertThat(PromptRequests.feature("MOCK_INTERVIEW")).isEqualTo(FeatureCode.MOCK_INTERVIEW);
    code(() -> PromptRequests.feature("OTHER"), "AIR-V11");
    code(() -> PromptRequests.feature("PRACTICE_FEEDBACK"), "AIR-V11");
    code(() -> PromptRequests.feature("solution_review"), "AIR-V11");
  }

  @Test
  void instructionBoundaries() {
    String limit = "a".repeat(PromptRequests.MAX_INSTRUCTION);
    assertThat(PromptRequests.instruction(json("{\"systemInstruction\":\"" + limit + "\"}")))
        .hasSize(20_000);
    code(
        () -> PromptRequests.instruction(json("{\"systemInstruction\":\"" + limit + "a\"}")),
        "AIR-V12");
    code(() -> PromptRequests.instruction(json("{\"systemInstruction\":\"   \"}")), "AIR-V12");
    code(() -> PromptRequests.instruction(json("{}")), "AIR-V12");
    code(() -> PromptRequests.instruction(json("{\"systemInstruction\":5}")), "AIR-V12");
    assertThat(PromptRequests.instruction(json("{\"systemInstruction\":\"x\"}"))).isEqualTo("x");
  }

  @Test
  void guardsShapePerFeature() {
    var ok = PromptRequests.guards(FeatureCode.SOLUTION_REVIEW, json(GUARDS), false);
    assertThat(ok.noFullSolution()).isFalse();
    code(() -> PromptRequests.guards(FeatureCode.SOLUTION_REVIEW, null, false), "AIR-V13");
    code(
        () ->
            PromptRequests.guards(FeatureCode.SOLUTION_REVIEW, json("{\"citeLines\":true}"), false),
        "AIR-V13");
    code(
        () ->
            PromptRequests.guards(
                FeatureCode.SOLUTION_REVIEW, json(GUARDS.replace("}", ",\"extra\":true}")), false),
        "AIR-V13");
    code(
        () ->
            PromptRequests.guards(
                FeatureCode.SOLUTION_REVIEW, json(GUARDS.replace("false", "\"no\"")), false),
        "AIR-V13");
    assertThat(PromptRequests.guards(FeatureCode.GENERATE_TESTCASE, null, false)).isNull();
    assertThat(PromptRequests.guards(FeatureCode.GENERATE_TESTCASE, json("null"), false)).isNull();
    code(
        () -> PromptRequests.guards(FeatureCode.GENERATE_TESTCASE, json(GUARDS), false), "AIR-V13");
    assertThat(PromptRequests.guards(FeatureCode.SOLUTION_REVIEW, null, true)).isNull();
  }

  @Test
  void publishChangesListRules() {
    UUID id = UUID.randomUUID();
    String one = "{\"featureCode\":\"SOLUTION_REVIEW\",\"expectedActivePromptId\":\"" + id + "\"}";
    assertThat(PromptRequests.changes(json("{\"changes\":[" + one + "]}"))).hasSize(1);
    code(() -> PromptRequests.changes(json("{\"changes\":[]}")), "AIR-V14");
    code(() -> PromptRequests.changes(json("{}")), "AIR-V14");
    code(() -> PromptRequests.changes(json("{\"changes\":[" + one + "," + one + "]}")), "AIR-V14");
    code(
        () -> PromptRequests.changes(json("{\"changes\":[{\"featureCode\":\"SOLUTION_REVIEW\"}]}")),
        "AIR-V14");
    code(
        () ->
            PromptRequests.changes(
                json(
                    "{\"changes\":[{\"featureCode\":\"X\",\"expectedActivePromptId\":\""
                        + id
                        + "\"}]}")),
        "AIR-V11");
    String four =
        "{\"changes\":["
            + one
            + ","
            + one.replace("SOLUTION_REVIEW", "MOCK_INTERVIEW")
            + ","
            + one.replace("SOLUTION_REVIEW", "GENERATE_TESTCASE")
            + ",{}]}";
    code(() -> PromptRequests.changes(json(four)), "AIR-V14");
  }
}
