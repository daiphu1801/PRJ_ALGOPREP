package com.algoprep.harness.application.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.SpecEvaluation.UnsupportedReason;
import com.algoprep.common.functionspec.SpecEvaluation.Violation;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.FunctionSignaturePreview;
import com.algoprep.harness.application.ports.in.MapCompileErrorUseCase;
import com.algoprep.harness.application.ports.in.MatchOutputUseCase;
import com.algoprep.harness.application.ports.in.PreviewFunctionSignatureUseCase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PreviewFunctionSignatureHandlerTest {
  private static final TypeNode INT = leaf(TypeKind.INT);
  private final PreviewFunctionSignatureUseCase preview = new PreviewFunctionSignatureHandler();

  @ParameterizedTest
  @MethodSource("scalarTypes")
  void formatsEveryScalarInReturnAndParameterPositions(
      TypeKind kind, String javaType, String cppType, String pythonType) {
    var result = preview.preview(spec(leaf(kind)));
    assertDeclarations(result, javaType, cppType, pythonType);
    assertThat(result.wrapperSupported()).isTrue();
    assertThat(result.unsupportedReasons()).isEmpty();
  }

  static Stream<Arguments> scalarTypes() {
    return Stream.of(
        Arguments.of(TypeKind.INT, "int", "int", "int"),
        Arguments.of(TypeKind.LONG, "long", "long long", "int"),
        Arguments.of(TypeKind.DOUBLE, "double", "double", "float"),
        Arguments.of(TypeKind.BOOLEAN, "boolean", "bool", "bool"),
        Arguments.of(TypeKind.CHAR, "char", "char", "str"),
        Arguments.of(TypeKind.STRING, "String", "std::string", "str"));
  }

  @ParameterizedTest
  @MethodSource("arrayTypes")
  void arraysKeepPrimitiveJavaElementsAndExplicitDimensions(
      TypeNode type, String javaType, String cppType, String pythonType) {
    assertDeclarations(preview.preview(spec(type)), javaType, cppType, pythonType);
  }

  static Stream<Arguments> arrayTypes() {
    return scalarTypes().flatMap(args -> arrayDimensions(args.get()));
  }

  private static Stream<Arguments> arrayDimensions(Object[] scalar) {
    return IntStream.rangeClosed(1, 3)
        .mapToObj(
            dimension ->
                Arguments.of(
                    new TypeNode(TypeKind.ARRAY, leaf((TypeKind) scalar[0]), dimension),
                    scalar[1] + "[]".repeat(dimension),
                    "vector<".repeat(dimension) + scalar[2] + ">".repeat(dimension),
                    "List[".repeat(dimension) + scalar[3] + "]".repeat(dimension)));
  }

  @ParameterizedTest
  @MethodSource("listTypes")
  void listsBoxJavaElementsAndPreserveNestedStructure(
      TypeNode type, String javaType, String cppType, String pythonType) {
    assertDeclarations(preview.preview(spec(type)), javaType, cppType, pythonType);
  }

  static Stream<Arguments> listTypes() {
    return Stream.of(
        Arguments.of(list(INT), "List<Integer>", "vector<int>", "List[int]"),
        Arguments.of(list(leaf(TypeKind.LONG)), "List<Long>", "vector<long long>", "List[int]"),
        Arguments.of(list(leaf(TypeKind.DOUBLE)), "List<Double>", "vector<double>", "List[float]"),
        Arguments.of(list(leaf(TypeKind.BOOLEAN)), "List<Boolean>", "vector<bool>", "List[bool]"),
        Arguments.of(list(leaf(TypeKind.CHAR)), "List<Character>", "vector<char>", "List[str]"),
        Arguments.of(
            list(leaf(TypeKind.STRING)), "List<String>", "vector<std::string>", "List[str]"),
        Arguments.of(
            list(list(INT)), "List<List<Integer>>", "vector<vector<int>>", "List[List[int]]"),
        Arguments.of(
            list(list(list(INT))),
            "List<List<List<Integer>>>",
            "vector<vector<vector<int>>>",
            "List[List[List[int]]]"));
  }

  @ParameterizedTest
  @MethodSource("nodeTypes")
  void nodesUseSharedClassNamesAndCppPointers(TypeKind kind, String className) {
    TypeNode type = new TypeNode(kind, INT, null);
    assertDeclarations(preview.preview(spec(type)), className, className + "*", className);
  }

  static Stream<Arguments> nodeTypes() {
    return Stream.of(
        Arguments.of(TypeKind.LINKED_LIST, "ListNode"),
        Arguments.of(TypeKind.BINARY_TREE, "TreeNode"));
  }

  @Test
  void namesFollowLanguageConventionsAndPreserveParameterOrder() {
    var draft =
        new FunctionSpecDraft(
            "HTTPServer",
            INT,
            List.of(
                new FunctionParameter("input_url", INT),
                new FunctionParameter("maxVal2", leaf(TypeKind.LONG))),
            Map.of(),
            MatchingStrategy.EXACT,
            null);
    var result = preview.preview(draft);
    assertThat(result.declarations())
        .containsExactly(
            Map.entry(Language.JAVA, "public int httpServer(int inputUrl, long maxVal2)"),
            Map.entry(Language.CPP, "int httpServer(int inputUrl, long long maxVal2)"),
            Map.entry(
                Language.PYTHON, "def http_server(self, input_url: int, max_val2: int) -> int:"));
    assertThat(draft.parameters().getFirst().name()).isEqualTo("input_url");
  }

  @Test
  void overridesChangeOnlyTheSpecifiedFunctionAndKeepSpelling() {
    var draft =
        new FunctionSpecDraft(
            "twoSum",
            INT,
            List.of(new FunctionParameter("max_val", INT)),
            Map.of("java", "MySolve"),
            MatchingStrategy.EXACT,
            null);
    assertThat(preview.preview(draft).declarations())
        .containsExactly(
            Map.entry(Language.JAVA, "public int MySolve(int maxVal)"),
            Map.entry(Language.CPP, "int twoSum(int maxVal)"),
            Map.entry(Language.PYTHON, "def two_sum(self, max_val: int) -> int:"));
  }

  @Test
  void noParametersHasNoDanglingCommaAndOnlyPythonSelf() {
    var draft =
        new FunctionSpecDraft("solve", INT, List.of(), Map.of(), MatchingStrategy.EXACT, null);
    assertThat(preview.preview(draft).declarations())
        .containsExactly(
            Map.entry(Language.JAVA, "public int solve()"),
            Map.entry(Language.CPP, "int solve()"),
            Map.entry(Language.PYTHON, "def solve(self) -> int:"));
  }

  @Test
  void supportsAllTenParametersWithoutChangingTheirOrder() {
    var parameters =
        IntStream.range(0, 10).mapToObj(i -> new FunctionParameter("value_" + i, INT)).toList();
    var draft =
        new FunctionSpecDraft("solve", INT, parameters, Map.of(), MatchingStrategy.EXACT, null);
    String java = preview.preview(draft).declarations().get(Language.JAVA);
    assertThat(java)
        .isEqualTo(
            "public int solve(int value0, int value1, int value2, int value3, "
                + "int value4, int value5, int value6, int value7, int value8, int value9)");
  }

  @Test
  void absentSignatureHasExplicitNullsAndTheOriginalUnsupportedReason() {
    var draft = new FunctionSpecDraft(null, null, null, Map.of(), MatchingStrategy.EXACT, null);
    var result = preview.preview(draft);
    assertUnsupported(result);
    assertThat(result.unsupportedReasons()).containsExactly(new UnsupportedReason("HRN-V07", ""));
  }

  @Test
  void unsupportedTypesHaveNoPartialDeclarations() {
    var result = preview.preview(spec(leaf(TypeKind.OTHER)));
    assertUnsupported(result);
    assertThat(result.unsupportedReasons())
        .contains(new UnsupportedReason("HRN-V01", "/returnType"));
    assertUnsupported(preview.preview(spec(new TypeNode(TypeKind.ARRAY, INT, 4))));
    assertUnsupported(preview.preview(spec(list(list(list(list(INT)))))));
  }

  @Test
  void hardFailuresWinEvenWhenAnotherFieldIsUnsupported() {
    var draft =
        new FunctionSpecDraft(
            "2bad", leaf(TypeKind.OTHER), List.of(), Map.of(), MatchingStrategy.EXACT, null);
    assertViolation(draft, "HRN-V11", "/functionName");
  }

  @Test
  void partialSignaturesCollisionsAndEpsilonUseTheRealEvaluator() {
    assertViolation(
        new FunctionSpecDraft("solve", null, List.of(), Map.of(), MatchingStrategy.EXACT, null),
        "HRN-V17",
        "");
    assertViolation(
        new FunctionSpecDraft(
            "solve",
            INT,
            List.of(new FunctionParameter("self", INT)),
            Map.of(),
            MatchingStrategy.EXACT,
            null),
        "HRN-V15",
        "/parameters/0/name");
    assertViolation(
        new FunctionSpecDraft("solve", INT, List.of(), Map.of(), MatchingStrategy.EPSILON, null),
        "HRN-V20",
        "/epsilonValue");
  }

  @Test
  void duplicateOverrideLanguagesRemainV18AndDoNotDependOnMapOrder() {
    Map<String, String> overrides = new LinkedHashMap<>();
    overrides.put("JAVA", "solve");
    overrides.put("java", "other");
    var draft =
        new FunctionSpecDraft("twoSum", INT, List.of(), overrides, MatchingStrategy.EXACT, null);
    assertViolation(draft, "HRN-V18", "/nameOverrides/java");
  }

  @Test
  void invalidDraftErrorsExposeOnlyStructuredViolationsAndHaveImmutableDetails() {
    var draft =
        new FunctionSpecDraft(
            "private-draft-secret", INT, List.of(), Map.of(), MatchingStrategy.EXACT, null);
    assertThatThrownBy(() -> preview.preview(draft))
        .isInstanceOfSatisfying(
            PreviewSpecInvalidException.class,
            ex -> {
              assertThat(ex.errorCode()).isEqualTo("HRN-060");
              assertThat(ex.getMessage()).isEqualTo("Invalid function signature draft");
              assertThat(ex).hasNoCause();
              assertThat(ex.clientErrorDetails().keySet()).containsExactly("violations");
              assertThatThrownBy(() -> ex.clientErrorDetails().clear())
                  .isInstanceOf(UnsupportedOperationException.class);
            });
    var source = new ArrayList<Violation>();
    source.add(new Violation("HRN-V11", "/functionName", "Invalid"));
    var exception = new PreviewSpecInvalidException(source);
    source.clear();
    assertThat((List<?>) exception.clientErrorDetails().get("violations")).hasSize(1);
  }

  @Test
  void resultCopiesItsCollectionsIncludingExplicitNulls() {
    EnumMap<Language, String> declarations = new EnumMap<>(Language.class);
    for (Language language : Language.values()) {
      declarations.put(language, null);
    }
    var reasons = new ArrayList<>(List.of(new UnsupportedReason("HRN-V07", "")));
    var result = new FunctionSignaturePreview(false, reasons, declarations);
    reasons.clear();
    declarations.put(Language.JAVA, "changed");
    assertUnsupported(result);
    assertThat(result.unsupportedReasons()).hasSize(1);
    assertThatThrownBy(() -> result.declarations().put(Language.JAVA, "changed"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> result.unsupportedReasons().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void missingInternalCommandHasASanitizedError() {
    assertThatThrownBy(() -> preview.preview(null))
        .isInstanceOfSatisfying(
            AlgoPrepException.class,
            ex -> {
              assertThat(ex.errorCode()).isEqualTo("HRN-031");
              assertThat(ex).hasNoCause();
            });
  }

  @Test
  void singletonIsDeterministicAcrossConcurrentCalls() throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<FunctionSignaturePreview> task = () -> preview.preview(spec(list(INT)));
      var expected = task.call();
      for (var result : executor.invokeAll(Collections.nCopies(30, task))) {
        assertThat(result.get()).isEqualTo(expected);
      }
    }
  }

  @Test
  void applicationScanProvidesAllThreeUseCasesWithoutBusinessOrInfrastructureDependencies() {
    try (var context = new AnnotationConfigApplicationContext("com.algoprep.harness")) {
      assertThat(context.getBean(PreviewFunctionSignatureUseCase.class).preview(spec(INT)))
          .isEqualTo(preview.preview(spec(INT)));
      assertThat(context.getBean(MapCompileErrorUseCase.class)).isNotNull();
      assertThat(context.getBean(MatchOutputUseCase.class)).isNotNull();
    }
  }

  private void assertViolation(FunctionSpecDraft draft, String code, String path) {
    assertThatThrownBy(() -> preview.preview(draft))
        .isInstanceOfSatisfying(
            PreviewSpecInvalidException.class,
            ex -> {
              assertThat(ex.errorCode()).isEqualTo("HRN-060");
              List<?> violations = (List<?>) ex.clientErrorDetails().get("violations");
              assertThat(violations.stream().map(Violation.class::cast))
                  .anySatisfy(
                      v -> {
                        assertThat(v.code()).isEqualTo(code);
                        assertThat(v.path()).isEqualTo(path);
                      });
            });
  }

  private static void assertUnsupported(FunctionSignaturePreview result) {
    assertThat(result.wrapperSupported()).isFalse();
    assertThat(result.declarations()).hasSize(3);
    assertThat(result.declarations().keySet()).containsExactly(Language.values());
    assertThat(result.declarations().values()).containsOnlyNulls();
    assertThat(result.unsupportedReasons()).isNotEmpty();
  }

  private static void assertDeclarations(
      FunctionSignaturePreview result, String javaType, String cppType, String pythonType) {
    assertThat(result.declarations())
        .containsExactly(
            Map.entry(Language.JAVA, "public " + javaType + " solve(" + javaType + " value)"),
            Map.entry(Language.CPP, cppType + " solve(" + cppType + " value)"),
            Map.entry(
                Language.PYTHON,
                "def solve(self, value: " + pythonType + ") -> " + pythonType + ":"));
  }

  private static FunctionSpecDraft spec(TypeNode type) {
    return new FunctionSpecDraft(
        "solve",
        type,
        List.of(new FunctionParameter("value", type)),
        Map.of(),
        MatchingStrategy.EXACT,
        null);
  }

  private static TypeNode leaf(TypeKind kind) {
    return new TypeNode(kind, null, null);
  }

  private static TypeNode list(TypeNode child) {
    return new TypeNode(TypeKind.LIST, child, null);
  }
}
