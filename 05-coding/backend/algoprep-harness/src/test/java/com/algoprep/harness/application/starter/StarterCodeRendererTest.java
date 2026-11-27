package com.algoprep.harness.application.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.domain.signature.FunctionSignatureDeclarations;
import com.algoprep.harness.domain.starter.StarterTemplates;
import com.algoprep.harness.infrastructure.template.ClasspathStarterCodeTemplates;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class StarterCodeRendererTest {
  private static final TypeNode INT = leaf(TypeKind.INT);
  private final StarterCodeRenderer renderer = ClasspathStarterCodeTemplates.create();
  @TempDir private Path temporary;

  @ParameterizedTest
  @MethodSource("goldenCases")
  void generatedStartersMatchIndependentGoldenFiles(Language language, SubmissionModel model)
      throws IOException {
    String actual = renderer.render(language, model, twoSum());
    String file =
        "/starter-golden/"
            + language.name().toLowerCase(Locale.ROOT)
            + "-"
            + (model == SubmissionModel.STANDARD_IO ? "standard-io" : "wrapper")
            + ".txt";
    assertThat(actual).isEqualTo(resource(file));
    assertThat(actual).doesNotContain("{{", "\r", "apH", "@@APH");
    assertThat(actual).endsWith("\n");
  }

  static Stream<Arguments> goldenCases() {
    return Stream.of(Language.values())
        .flatMap(
            language ->
                Stream.of(SubmissionModel.values()).map(model -> Arguments.of(language, model)));
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void wrapperContainsExactlyTheDeclarationUsedByPreview(Language language) {
    var draft =
        new FunctionSpecDraft(
            "HTTPServer",
            array(),
            List.of(
                new FunctionParameter("input_url", array()), new FunctionParameter("max_val", INT)),
            Map.of("JAVA", "MySolve"),
            MatchingStrategy.EXACT,
            null);
    String source = renderer.render(language, SubmissionModel.FUNCTION_WRAPPER, draft);
    String declaration = FunctionSignatureDeclarations.render(draft).get(language);
    assertThat(source)
        .containsOnlyOnce(declaration)
        .doesNotContain("oldName", "testcase", "stdin", "stdout");
    if (language != Language.JAVA) {
      assertThat(source).doesNotContain("MySolve");
    }
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void standardIoIgnoresMissingInvalidAndUnsupportedDrafts(Language language) {
    String expected = renderer.render(language, SubmissionModel.STANDARD_IO, null);
    assertThat(renderer.render(language, SubmissionModel.STANDARD_IO, spec(leaf(TypeKind.OTHER))))
        .isEqualTo(expected);
    var invalid =
        new FunctionSpecDraft("2bad", null, null, Map.of(), MatchingStrategy.EPSILON, null);
    assertThat(renderer.render(language, SubmissionModel.STANDARD_IO, invalid)).isEqualTo(expected);
    assertThat(expected).doesNotContain("Solution", "solve", "ListNode", "TreeNode");
  }

  @ParameterizedTest
  @MethodSource("javaDefaults")
  void javaDefaultReturnsFollowTheDd(TypeNode type, String expression) {
    assertThat(renderer.render(Language.JAVA, SubmissionModel.FUNCTION_WRAPPER, spec(type)))
        .contains("return " + expression + ";");
  }

  static Stream<Arguments> javaDefaults() {
    return Stream.of(
        Arguments.of(INT, "0"),
        Arguments.of(leaf(TypeKind.LONG), "0L"),
        Arguments.of(leaf(TypeKind.DOUBLE), "0.0"),
        Arguments.of(leaf(TypeKind.BOOLEAN), "false"),
        Arguments.of(leaf(TypeKind.CHAR), "' '"),
        Arguments.of(leaf(TypeKind.STRING), "null"),
        Arguments.of(array(), "null"),
        Arguments.of(list(INT), "null"),
        Arguments.of(node(TypeKind.LINKED_LIST), "null"),
        Arguments.of(node(TypeKind.BINARY_TREE), "null"));
  }

  @ParameterizedTest
  @MethodSource("javaDefaults")
  void generatedJavaCompilesWithRelease21(TypeNode type, String ignoredExpression)
      throws IOException {
    String source = renderer.render(Language.JAVA, SubmissionModel.FUNCTION_WRAPPER, spec(type));
    // Node declarations are test-only trusted context, not a claimed implementation of P2.
    String nodeContext =
        "\nclass ListNode { int val; ListNode next; }\n"
            + "class TreeNode { int val; TreeNode left; TreeNode right; }\n";
    assertJavaCompiles("Solution.java", source + nodeContext);
  }

  @Test
  void generatedJavaStandardIoCompilesWithoutAnyFunctionSpecification() throws IOException {
    assertJavaCompiles(
        "Main.java", renderer.render(Language.JAVA, SubmissionModel.STANDARD_IO, null));
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void nodeDocumentationIsDeterministicAndNeverDefinesNodeClasses(Language language) {
    var draft =
        new FunctionSpecDraft(
            "solve",
            node(TypeKind.BINARY_TREE),
            List.of(
                new FunctionParameter("head", node(TypeKind.LINKED_LIST)),
                new FunctionParameter("another_head", node(TypeKind.LINKED_LIST))),
            Map.of(),
            MatchingStrategy.EXACT,
            null);
    String source = renderer.render(language, SubmissionModel.FUNCTION_WRAPPER, draft);
    assertThat(source)
        .containsOnlyOnce("ListNode fields: val (INT), next (ListNode).")
        .containsOnlyOnce("TreeNode fields: val (INT), left/right (TreeNode).")
        .doesNotContain("class ListNode", "class TreeNode", "struct ListNode", "struct TreeNode");
    assertThat(source.indexOf("ListNode fields")).isLessThan(source.indexOf("TreeNode fields"));
    String prefix = language == Language.PYTHON ? "#" : "//";
    assertThat(source).contains(prefix + " ListNode fields", prefix + " TreeNode fields");
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void nodeDocumentationAlsoWorksWhenOnlyTheReturnTypeUsesANode(Language language) {
    var draft =
        new FunctionSpecDraft(
            "solve", node(TypeKind.LINKED_LIST), List.of(), Map.of(), MatchingStrategy.EXACT, null);
    assertThat(renderer.render(language, SubmissionModel.FUNCTION_WRAPPER, draft))
        .containsOnlyOnce("ListNode fields:")
        .doesNotContain("TreeNode fields:");
  }

  @Test
  void missingAndPartialSignaturesFailBeforeOtherDraftChecks() {
    assertCode(
        () -> renderer.render(Language.JAVA, SubmissionModel.FUNCTION_WRAPPER, null), "HRN-005");
    var partial =
        new FunctionSpecDraft("solve", null, List.of(), Map.of(), MatchingStrategy.EPSILON, null);
    assertCode(
        () -> renderer.render(Language.JAVA, SubmissionModel.FUNCTION_WRAPPER, partial), "HRN-005");
  }

  @Test
  void unsupportedAndHardInvalidDraftsHaveDifferentSanitizedErrors() {
    assertCode(
        () ->
            renderer.render(
                Language.CPP, SubmissionModel.FUNCTION_WRAPPER, spec(leaf(TypeKind.OTHER))),
        "HRN-004");
    var invalid =
        new FunctionSpecDraft(
            "private-secret", INT, List.of(), Map.of(), MatchingStrategy.EXACT, null);
    assertCode(
        () -> renderer.render(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, invalid),
        "HRN-006");
    assertThatThrownBy(
            () -> renderer.render(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, invalid))
        .hasMessage("Invalid stored function specification")
        .hasNoCause();
  }

  @Test
  void nullLanguageAndModelDoNotSelectDefaultPlugins() {
    assertCode(() -> renderer.render(null, SubmissionModel.STANDARD_IO, null), "HRN-010");
    assertCode(() -> renderer.render(Language.JAVA, null, twoSum()), "HRN-031");
  }

  @ParameterizedTest
  @MethodSource("invalidTemplates")
  void brokenTemplateSetsFailBeforeAnySourceIsRendered(Map<Language, StarterTemplates> templates) {
    assertCode(() -> new StarterCodeRenderer(templates), "HRN-030");
  }

  static Stream<Arguments> invalidTemplates() throws IOException {
    var original = templates();
    var missing = new EnumMap<>(original);
    missing.remove(Language.PYTHON);
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(missing),
        Arguments.of(replaceJava(original, null)),
        Arguments.of(replaceJava(original, new StarterTemplates(null, "main"))),
        Arguments.of(replaceJava(original, new StarterTemplates("missing markers", "main"))),
        Arguments.of(
            replaceJava(
                original,
                new StarterTemplates(original.get(Language.JAVA).functionWrapper(), " "))),
        Arguments.of(
            replaceJava(
                original,
                new StarterTemplates(original.get(Language.JAVA).functionWrapper(), "{{bad}}"))),
        Arguments.of(
            replaceJava(
                original,
                new StarterTemplates(
                    original.get(Language.JAVA).functionWrapper() + "{{DECLARATION}}", "main"))),
        Arguments.of(
            replaceJava(
                original,
                new StarterTemplates(
                    original.get(Language.JAVA).functionWrapper() + "{{bad}}", "main"))));
  }

  @Test
  void copiedTemplateMapAndConcurrentCallsCannotChangeTheOutput() throws Exception {
    var sourceTemplates = templates();
    var copy = new StarterCodeRenderer(sourceTemplates);
    String expected = copy.render(Language.CPP, SubmissionModel.FUNCTION_WRAPPER, twoSum());
    sourceTemplates.clear();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<String> task =
          () -> copy.render(Language.CPP, SubmissionModel.FUNCTION_WRAPPER, twoSum());
      for (var output : executor.invokeAll(Collections.nCopies(30, task))) {
        assertThat(output.get()).isEqualTo(expected);
      }
    }
  }

  private void assertJavaCompiles(String fileName, String source) throws IOException {
    Path file = temporary.resolve(fileName);
    Files.writeString(file, source, StandardCharsets.UTF_8);
    var compiler = ToolProvider.getSystemJavaCompiler();
    assertThat(compiler).as("Maven runs on a full JDK").isNotNull();
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    try (var manager =
        compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
      var task =
          compiler.getTask(
              null,
              manager,
              diagnostics,
              List.of("--release", "21", "-proc:none", "-d", temporary.toString()),
              null,
              manager.getJavaFileObjects(file));
      assertThat(task.call())
          .as("Generated Java diagnostics: %s", diagnostics.getDiagnostics())
          .isTrue();
    }
  }

  private static EnumMap<Language, StarterTemplates> templates() throws IOException {
    EnumMap<Language, StarterTemplates> result = new EnumMap<>(Language.class);
    for (Language language : Language.values()) {
      String base = "/harness-templates/" + language.name().toLowerCase(Locale.ROOT) + "/starter-";
      result.put(
          language,
          new StarterTemplates(resource(base + "wrapper.txt"), resource(base + "standard-io.txt")));
    }
    return result;
  }

  private static Map<Language, StarterTemplates> replaceJava(
      EnumMap<Language, StarterTemplates> original, StarterTemplates replacement) {
    var result = new EnumMap<>(original);
    result.put(Language.JAVA, replacement);
    return result;
  }

  private static String resource(String path) throws IOException {
    try (var stream = StarterCodeRendererTest.class.getResourceAsStream(path)) {
      assertThat(stream).as("Resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
  }

  private static void assertCode(Runnable operation, String code) {
    assertThatThrownBy(operation::run)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo(code));
  }

  private static FunctionSpecDraft twoSum() {
    return new FunctionSpecDraft(
        "two_sum",
        array(),
        List.of(new FunctionParameter("nums", array()), new FunctionParameter("target", INT)),
        Map.of(),
        MatchingStrategy.EXACT,
        null);
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

  private static TypeNode array() {
    return new TypeNode(TypeKind.ARRAY, INT, 1);
  }

  private static TypeNode list(TypeNode of) {
    return new TypeNode(TypeKind.LIST, of, null);
  }

  private static TypeNode node(TypeKind kind) {
    return new TypeNode(kind, INT, null);
  }
}
