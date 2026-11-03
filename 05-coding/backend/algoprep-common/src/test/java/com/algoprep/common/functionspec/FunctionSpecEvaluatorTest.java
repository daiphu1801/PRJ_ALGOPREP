package com.algoprep.common.functionspec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class FunctionSpecEvaluatorTest {
  private static final TypeNode INT = leaf(TypeKind.INT);

  @ParameterizedTest
  @EnumSource(
      value = TypeKind.class,
      names = {"INT", "LONG", "DOUBLE", "BOOLEAN", "CHAR", "STRING"})
  void acceptsEveryScalar(TypeKind kind) {
    assertSupported(draft(leaf(kind)));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void acceptsArrayDimensionsAndListDepthAtEachBoundary(int count) {
    assertSupported(draft(new TypeNode(TypeKind.ARRAY, INT, count)));
    assertSupported(draft(nest(count, INT)));
  }

  @ParameterizedTest
  @EnumSource(
      value = TypeKind.class,
      names = {"LINKED_LIST", "BINARY_TREE"})
  void acceptsSupportedNodeValuesAndRejectsMixedOnes(TypeKind kind) {
    for (TypeKind value : List.of(TypeKind.INT, TypeKind.LONG, TypeKind.DOUBLE, TypeKind.BOOLEAN)) {
      assertSupported(draft(new TypeNode(kind, leaf(value), null)));
    }
    var mixed =
        spec(
            "solve",
            new TypeNode(kind, INT, null),
            List.of(new FunctionParameter("value", new TypeNode(kind, leaf(TypeKind.LONG), null))));
    assertSoft(mixed, "HRN-V06", "/parameters/0/type");
  }

  @Test
  void unsupportedTypesAreSoftAndDoNotRewriteTheInput() {
    assertSoft(draft(leaf(TypeKind.OTHER)), "HRN-V01", "/returnType");
    assertSoft(draft(new TypeNode(TypeKind.LIST, null, null)), "HRN-V02", "/returnType");
    assertSoft(draft(new TypeNode(TypeKind.ARRAY, nest(1, INT), 1)), "HRN-V03", "/returnType");
    assertSoft(
        draft(new TypeNode(TypeKind.LINKED_LIST, leaf(TypeKind.CHAR), null)),
        "HRN-V03",
        "/returnType");
    assertSoft(draft(new TypeNode(TypeKind.ARRAY, INT, 4)), "HRN-V04", "/returnType");
    var deep = draft(nest(4, INT));
    assertSoft(deep, "HRN-V05", "/returnType/of/of/of");
    assertThat(deep.returnType()).isEqualTo(nest(4, INT));
  }

  @Test
  void malformedTypedNodesAreHardFailures() {
    assertHard(draft(new TypeNode(null, null, null)), "HRN-V10", "/returnType");
    assertHard(draft(new TypeNode(TypeKind.INT, INT, null)), "HRN-V10", "/returnType");
    assertHard(draft(new TypeNode(TypeKind.LIST, INT, 1)), "HRN-V10", "/returnType");
    assertHard(draft(new TypeNode(TypeKind.ARRAY, INT, null)), "HRN-V10", "/returnType");
    assertHard(draft(new TypeNode(TypeKind.ARRAY, INT, 0)), "HRN-V10", "/returnType");
    assertHard(
        spec("solve", INT, List.of(new FunctionParameter("value", null))),
        "HRN-V10",
        "/parameters/0/type");
  }

  @Test
  void absentSignatureIsSoftButEveryPartialSignatureIsHard() {
    assertSoft(spec(null, null, null), "HRN-V07", "");
    assertHard(spec("solve", null, null), "HRN-V17", "");
    assertHard(spec(null, INT, null), "HRN-V17", "");
    assertHard(spec(null, null, List.of()), "HRN-V17", "");
    assertHard(spec("solve", INT, null), "HRN-V17", "");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "2solve", "bad-name", "éclair", "_solve"})
  void rejectsInvalidIdentifiers(String name) {
    assertHard(spec(name, INT, List.of()), "HRN-V11", "/functionName");
    assertHard(
        spec("solve", INT, List.of(new FunctionParameter(name, INT))),
        "HRN-V13",
        "/parameters/0/name");
  }

  @Test
  void identifierAndParameterCountLimitsAreInclusive() {
    assertSupported(spec("a".repeat(64), INT, List.of()));
    assertHard(spec("a".repeat(65), INT, List.of()), "HRN-V11", "/functionName");
    assertHard(
        spec("solve", INT, List.of(new FunctionParameter("a".repeat(65), INT))),
        "HRN-V13",
        "/parameters/0/name");
    var parameters =
        IntStream.range(0, 10).mapToObj(i -> new FunctionParameter("p" + i, INT)).toList();
    assertSupported(spec("solve", INT, parameters));
    var eleven = new ArrayList<>(parameters);
    eleven.add(new FunctionParameter("p10", INT));
    assertHard(spec("solve", INT, eleven), "HRN-V16", "/parameters");
  }

  @ParameterizedTest
  @ValueSource(strings = {"class", "new", "lambda", "apHsolve", "APHvalue"})
  void rejectsReservedNamesInAnyLanguage(String name) {
    assertHard(spec(name, INT, List.of()), "HRN-V12", "/functionName");
    assertHard(
        spec("solve", INT, List.of(new FunctionParameter(name, INT))),
        "HRN-V15",
        "/parameters/0/name");
  }

  @Test
  void checksParameterCollisionsAfterConversionAndAgainstTheFunction() {
    assertHard(
        spec(
            "solve",
            INT,
            List.of(new FunctionParameter("maxVal", INT), new FunctionParameter("max_val", INT))),
        "HRN-V14",
        "/parameters/1/name");
    assertHard(
        spec("solve", INT, List.of(new FunctionParameter("self", INT))),
        "HRN-V15",
        "/parameters/0/name");
    assertHard(
        spec("solve", INT, List.of(new FunctionParameter("solve", INT))),
        "HRN-V15",
        "/parameters/0/name");
  }

  @Test
  void validatesOverridesForTheirOwnLanguageAndEscapesPointers() {
    assertHard(overrides(Map.of("RUBY", "solve")), "HRN-V18", "/nameOverrides/RUBY");
    assertHard(overrides(Map.of("x/y~z", "solve")), "HRN-V18", "/nameOverrides/x~1y~0z");
    for (String value : List.of("class", "apHsolve", "bad-name")) {
      assertHard(overrides(Map.of("JAVA", value)), "HRN-V19", "/nameOverrides/JAVA");
    }
    assertSupported(overrides(Map.of("python", "solve_py")));
    assertSupported(overrides(Map.of("JAVA", "lambda")));
    var duplicate = new LinkedHashMap<String, String>();
    duplicate.put("JAVA", "solve");
    duplicate.put("java", "other");
    assertHard(overrides(duplicate), "HRN-V18", "/nameOverrides/java");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-0.001"})
  void requiresPositiveEpsilon(String epsilon) {
    assertHard(
        matching(MatchingStrategy.EPSILON, new BigDecimal(epsilon), INT),
        "HRN-V20",
        "/epsilonValue");
  }

  @Test
  void checksMatchingWithoutAddingUndocumentedRestrictions() {
    assertHard(matching(MatchingStrategy.EPSILON, null, INT), "HRN-V20", "/epsilonValue");
    assertSupported(matching(MatchingStrategy.EPSILON, new BigDecimal("0.001"), INT));
    assertSupported(matching(MatchingStrategy.EXACT, BigDecimal.ZERO, INT));
    assertSupported(matching(MatchingStrategy.UNORDERED_SET, null, INT));
    for (TypeKind kind : List.of(TypeKind.BINARY_TREE, TypeKind.LINKED_LIST)) {
      assertHard(
          matching(MatchingStrategy.UNORDERED_SET, null, new TypeNode(kind, INT, null)),
          "HRN-V22",
          "/returnType");
    }
  }

  @Test
  void evaluationsAreImmutableAndIndependentAcrossCalls() {
    var parameters = new ArrayList<FunctionParameter>();
    var names = new LinkedHashMap<String, String>();
    var input =
        new FunctionSpecDraft("solve", INT, parameters, names, MatchingStrategy.EXACT, null);
    parameters.add(new FunctionParameter("self", INT));
    names.put("RUBY", "solve");
    assertSupported(input);
    var result = FunctionSpecEvaluator.evaluate(input);
    assertThat(FunctionSpecEvaluator.evaluate(input)).isEqualTo(result);
    assertThatThrownBy(() -> result.violations().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> input.parameters().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> input.nameOverrides().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertSoft(draft(leaf(TypeKind.OTHER)), "HRN-V01", "/returnType");
    assertSupported(input);
  }

  @Test
  void nullOverrideValuesReturnViolationsInsteadOfThrowing() {
    var values = new LinkedHashMap<String, String>();
    values.put("JAVA", null);
    assertHard(overrides(values), "HRN-V19", "/nameOverrides/JAVA");
    values.clear();
    values.put(null, "solve");
    assertHard(overrides(values), "HRN-V18", "/nameOverrides/null");
  }

  @Test
  void collectsHardFailuresBelowAnUnsupportedDepthWithoutRecursion() {
    var input = draft(nest(5, new TypeNode(null, null, null)));
    assertHard(input, "HRN-V10", "/returnType/of/of/of/of/of");
    assertThat(FunctionSpecEvaluator.evaluate(input).unsupportedReasons())
        .contains(new SpecEvaluation.UnsupportedReason("HRN-V05", "/returnType/of/of/of"));
    assertThat(FunctionSpecEvaluator.evaluate(draft(nest(1000, INT))).wrapperSupported()).isFalse();
  }

  private static TypeNode leaf(TypeKind kind) {
    return new TypeNode(kind, null, null);
  }

  private static TypeNode nest(int depth, TypeNode leaf) {
    TypeNode result = leaf;
    for (int i = 0; i < depth; i++) {
      result = new TypeNode(TypeKind.LIST, result, null);
    }
    return result;
  }

  private static FunctionSpecDraft draft(TypeNode type) {
    return spec("solve", type, List.of());
  }

  private static FunctionSpecDraft spec(
      String name, TypeNode type, List<FunctionParameter> parameters) {
    return new FunctionSpecDraft(name, type, parameters, Map.of(), MatchingStrategy.EXACT, null);
  }

  private static FunctionSpecDraft overrides(Map<String, String> names) {
    return new FunctionSpecDraft("solve", INT, List.of(), names, MatchingStrategy.EXACT, null);
  }

  private static FunctionSpecDraft matching(
      MatchingStrategy strategy, BigDecimal epsilon, TypeNode type) {
    return new FunctionSpecDraft("solve", type, List.of(), Map.of(), strategy, epsilon);
  }

  private static void assertSupported(FunctionSpecDraft draft) {
    assertThat(FunctionSpecEvaluator.evaluate(draft))
        .isEqualTo(new SpecEvaluation(List.of(), true, List.of()));
  }

  private static void assertHard(FunctionSpecDraft draft, String code, String path) {
    var result = FunctionSpecEvaluator.evaluate(draft);
    assertThat(result.wrapperSupported()).isFalse();
    assertThat(result.violations())
        .anySatisfy(
            v -> {
              assertThat(v.code()).isEqualTo(code);
              assertThat(v.path()).isEqualTo(path);
            });
  }

  private static void assertSoft(FunctionSpecDraft draft, String code, String path) {
    var result = FunctionSpecEvaluator.evaluate(draft);
    assertThat(result.wrapperSupported()).isFalse();
    assertThat(result.violations()).isEmpty();
    assertThat(result.unsupportedReasons())
        .contains(new SpecEvaluation.UnsupportedReason(code, path));
  }
}
