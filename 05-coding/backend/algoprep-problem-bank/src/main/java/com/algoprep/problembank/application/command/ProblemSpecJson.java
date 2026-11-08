package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.SpecEvaluation;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.problembank.domain.exception.ProblemSpecInvalidException;
import com.algoprep.problembank.domain.model.ProblemSpecData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Raw JSON validation before P5; unknown kinds are mapped only for evaluation, never storage. */
public final class ProblemSpecJson {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ProblemSpecJson() {}

  /** Public so the F3 signature preview reads a body exactly as a save of the spec does. */
  public static ProblemSpecData data(JsonNode body, ProblemSpecData previous) {
    if (body == null || !body.isObject()) {
      throw invalid();
    }
    String matching = text(body, "matchingStrategy", 32);
    if (matching == null) {
      matching = previous == null ? "EXACT" : previous.matchingStrategy();
    }
    try {
      MatchingStrategy.valueOf(matching);
    } catch (IllegalArgumentException failure) {
      throw invalid();
    }
    JsonNode epsilon = body.get("epsilonValue");
    if (present(epsilon) && !epsilon.isNumber()) {
      throw invalid();
    }
    return new ProblemSpecData(
        matching,
        present(epsilon) ? epsilon.decimalValue().stripTrailingZeros() : null,
        text(body, "stdinFormatMd", 20000),
        text(body, "stdoutFormatMd", 20000),
        text(body, "functionName", Integer.MAX_VALUE),
        raw(body.get("returnType")),
        raw(body.get("parameters")),
        raw(body.get("nameOverrides")));
  }

  public static FunctionSpecDraft draft(ProblemSpecData data) {
    return new FunctionSpecDraft(
        data.functionName(),
        type(parse(data.returnTypeJson()), "/returnType"),
        parameters(parse(data.parametersJson())),
        overrides(parse(data.nameOverridesJson())),
        MatchingStrategy.valueOf(data.matchingStrategy()),
        data.epsilonValue());
  }

  static ProblemSpecData normalize(ProblemSpecData data) {
    JsonNode overrides = parse(data.nameOverridesJson());
    ObjectNode normalized = JSON.createObjectNode();
    if (present(overrides)) {
      overrides
          .properties()
          .forEach(e -> normalized.set(e.getKey().toUpperCase(Locale.ROOT), e.getValue()));
    }
    return new ProblemSpecData(
        data.matchingStrategy(),
        data.epsilonValue(),
        data.stdinFormatMd(),
        data.stdoutFormatMd(),
        data.functionName(),
        data.returnTypeJson(),
        data.parametersJson(),
        present(overrides) ? normalized.toString() : null);
  }

  public static void checkEpsilon(BigDecimal epsilon) {
    if (epsilon != null
        && (epsilon.signum() <= 0
            || epsilon.scale() > 12
            || epsilon.compareTo(new BigDecimal("999999.999999999999")) > 0)) {
      throw hard("HRN-V20", "/epsilonValue");
    }
  }

  static ObjectNode document(ProblemSpecData data) {
    ObjectNode node = JSON.createObjectNode();
    node.put("matchingStrategy", data.matchingStrategy());
    node.put("epsilonValue", data.epsilonValue());
    node.put("stdinFormatMd", data.stdinFormatMd());
    node.put("stdoutFormatMd", data.stdoutFormatMd());
    node.put("functionName", data.functionName());
    node.set("returnType", parse(data.returnTypeJson()));
    node.set("parameters", parse(data.parametersJson()));
    node.set("nameOverrides", parse(data.nameOverridesJson()));
    return node;
  }

  static List<String> changed(ProblemSpecData before, ProblemSpecData after) {
    var result = new ArrayList<String>();
    ObjectNode old = before == null ? JSON.createObjectNode() : document(before);
    document(after)
        .properties()
        .forEach(
            e -> {
              if (!same(e.getValue(), old.get(e.getKey()))) {
                result.add(e.getKey());
              }
            });
    return result;
  }

  private static boolean same(JsonNode left, JsonNode right) {
    if (left.isNumber() && right != null && right.isNumber()) {
      return left.decimalValue().compareTo(right.decimalValue()) == 0;
    }
    return left.equals(right);
  }

  private static List<FunctionParameter> parameters(JsonNode node) {
    if (!present(node)) {
      return null;
    }
    if (!node.isArray()) {
      throw invalid();
    }
    var result = new ArrayList<FunctionParameter>();
    for (int i = 0; i < node.size(); i++) {
      JsonNode parameter = node.get(i);
      String path = "/parameters/" + i;
      shape(parameter, Set.of("name", "type"), path);
      result.add(
          new FunctionParameter(
              text(parameter, "name", Integer.MAX_VALUE),
              type(parameter.get("type"), path + "/type")));
    }
    return result;
  }

  private static Map<String, String> overrides(JsonNode node) {
    if (!present(node)) {
      return Map.of();
    }
    if (!node.isObject()) {
      throw invalid();
    }
    var values = new LinkedHashMap<String, String>();
    node.properties()
        .forEach(
            e -> values.put(e.getKey(), e.getValue().isString() ? e.getValue().asString() : null));
    return values;
  }

  private static TypeNode type(JsonNode node, String path) {
    if (!present(node)) {
      return null;
    }
    shape(node, Set.of("kind", "of", "dimensions"), path);
    if (!node.path("kind").isString()) {
      throw hard("HRN-V10", path);
    }
    TypeKind kind;
    try {
      kind = TypeKind.valueOf(node.path("kind").asString());
    } catch (IllegalArgumentException unknown) {
      kind = TypeKind.OTHER;
    }
    JsonNode dimensions = node.get("dimensions");
    if (dimensions != null
        && (!dimensions.isIntegralNumber()
            || !dimensions.canConvertToInt()
            || dimensions.intValue() < 1)) {
      throw hard("HRN-V10", path);
    }
    JsonNode child = node.get("of");
    if (child != null && !child.isObject()) {
      throw hard("HRN-V10", path);
    }
    return new TypeNode(
        kind, type(child, path + "/of"), dimensions == null ? null : dimensions.intValue());
  }

  private static void shape(JsonNode node, Set<String> fields, String path) {
    if (!node.isObject() || node.propertyStream().anyMatch(e -> !fields.contains(e.getKey()))) {
      throw hard("HRN-V10", path);
    }
  }

  private static String text(JsonNode node, String key, int limit) {
    JsonNode value = node.get(key);
    if (!present(value)) {
      return null;
    }
    if (!value.isString()
        || value.asString().codePointCount(0, value.asString().length()) > limit) {
      throw invalid();
    }
    return value.asString();
  }

  private static boolean present(JsonNode node) {
    return node != null && !node.isNull();
  }

  private static String raw(JsonNode node) {
    return present(node) ? node.toString() : null;
  }

  private static JsonNode parse(String json) {
    return json == null ? JSON.nullNode() : JSON.readTree(json);
  }

  private static InvalidRequestException invalid() {
    return new InvalidRequestException("PRB-001", "Invalid specification request shape");
  }

  private static ProblemSpecInvalidException hard(String code, String path) {
    return new ProblemSpecInvalidException(
        List.of(new SpecEvaluation.Violation(code, path, "Invalid function specification")));
  }
}
