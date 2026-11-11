package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.PublishChange;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.ResponseGuards;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Shape checks of endpoints 15 to 17: AIR-V11 to AIR-V14 (DD validation F5 section 9). */
final class PromptRequests {
  static final int MAX_INSTRUCTION = 20_000;
  private static final Set<FeatureCode> FEATURES =
      EnumSet.of(
          FeatureCode.SOLUTION_REVIEW, FeatureCode.MOCK_INTERVIEW, FeatureCode.GENERATE_TESTCASE);
  private static final Set<String> GUARD_KEYS =
      Set.of("citeLines", "answerInVietnamese", "noFullSolution");

  private PromptRequests() {}

  static FeatureCode feature(String raw) {
    for (FeatureCode code : FEATURES) {
      if (code.name().equals(raw)) {
        return code;
      }
    }
    throw new AiInvalidRequestException("AIR-V11", "featureCode is not valid");
  }

  /** Required instruction of the draft endpoint. */
  static String instruction(JsonNode body) {
    return instruction(body.get("systemInstruction"), false);
  }

  /** Null when absent and optional; otherwise non-blank and at most 20,000 characters. */
  static String instruction(JsonNode node, boolean optional) {
    if (node == null || node.isNull()) {
      if (optional) {
        return null;
      }
      throw v12();
    }
    if (!node.isString()
        || node.asString().isBlank()
        || node.asString().length() > MAX_INSTRUCTION) {
      throw v12();
    }
    return node.asString();
  }

  /** Null when absent or not text; the allowed-list check (AIR-202) happens in the use case. */
  static String modelId(JsonNode node) {
    return node != null && node.isString() ? node.asString() : null;
  }

  /** GENERATE_TESTCASE needs null; the others an object of exactly the three boolean keys. */
  static ResponseGuards guards(FeatureCode feature, JsonNode node, boolean optional) {
    boolean absent = node == null || node.isNull();
    if (feature == FeatureCode.GENERATE_TESTCASE) {
      if (!absent) {
        throw v13();
      }
      return null;
    }
    if (absent) {
      if (optional) {
        return null;
      }
      throw v13();
    }
    if (!node.isObject() || node.size() != GUARD_KEYS.size()) {
      throw v13();
    }
    for (String key : GUARD_KEYS) {
      if (!node.path(key).isBoolean()) {
        throw v13();
      }
    }
    return new ResponseGuards(
        node.get("citeLines").asBoolean(),
        node.get("answerInVietnamese").asBoolean(),
        node.get("noFullSolution").asBoolean());
  }

  static List<PublishChange> changes(JsonNode body) {
    JsonNode array = body == null ? null : body.get("changes");
    if (array == null || !array.isArray() || array.isEmpty() || array.size() > FEATURES.size()) {
      throw v14();
    }
    List<PublishChange> result = new ArrayList<>();
    Set<FeatureCode> seen = EnumSet.noneOf(FeatureCode.class);
    for (JsonNode item : array) {
      if (!item.isObject()) {
        throw v14();
      }
      JsonNode featureNode = item.get("featureCode");
      FeatureCode feature =
          feature(featureNode != null && featureNode.isString() ? featureNode.asString() : null);
      if (!seen.add(feature)) {
        throw v14();
      }
      result.add(
          new PublishChange(
              feature,
              expectedId(item.get("expectedActivePromptId")),
              instruction(item.get("systemInstruction"), true),
              modelId(item.get("modelId")),
              guards(feature, item.get("responseGuards"), true)));
    }
    return result;
  }

  private static UUID expectedId(JsonNode node) {
    if (node == null || !node.isString()) {
      throw v14();
    }
    try {
      return UUID.fromString(node.asString());
    } catch (IllegalArgumentException notUuid) {
      throw v14();
    }
  }

  private static AiInvalidRequestException v12() {
    return new AiInvalidRequestException("AIR-V12", "systemInstruction is empty or too long");
  }

  private static AiInvalidRequestException v13() {
    return new AiInvalidRequestException("AIR-V13", "responseGuards has the wrong shape");
  }

  private static AiInvalidRequestException v14() {
    return new AiInvalidRequestException("AIR-V14", "changes list is invalid");
  }
}
