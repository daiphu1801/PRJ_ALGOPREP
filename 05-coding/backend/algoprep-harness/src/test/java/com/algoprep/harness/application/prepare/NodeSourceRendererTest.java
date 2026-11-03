package com.algoprep.harness.application.prepare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.infrastructure.template.ClasspathPythonSignatureGuard;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class NodeSourceRendererTest {
  private final NodeSourceRenderer renderer = new NodeSourceRenderer();

  @ParameterizedTest
  @MethodSource("nodeCases")
  void rendersBothNodeTypesOnceAndExportsTrustedToolchainFixture(Language language, TypeKind value)
      throws IOException {
    var draft = spec(node(TypeKind.LINKED_LIST, value), node(TypeKind.BINARY_TREE, value));
    String source = renderer.render(language, draft);
    assertThat(source).contains("ListNode", "TreeNode", "val", "next", "left", "right");
    assertThat(source).doesNotContain("{{", "\r");
    assertThat(source).endsWith("\n");
    assertThat(source).isEqualTo(renderer.render(language, draft));
    Path directory = Path.of("target", "f3-node-probe");
    Files.createDirectories(directory);
    Files.writeString(
        directory.resolve(language + "-" + value + ".txt"), source, StandardCharsets.UTF_8);
  }

  static Stream<Arguments> nodeCases() {
    return Stream.of(Language.values())
        .flatMap(
            language ->
                Stream.of(TypeKind.INT, TypeKind.LONG, TypeKind.DOUBLE, TypeKind.BOOLEAN)
                    .map(value -> Arguments.of(language, value)));
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void scalarAndListSpecsDoNotEmitNodeClasses(Language language) {
    var scalar = new TypeNode(TypeKind.INT, null, null);
    var list = new TypeNode(TypeKind.LIST, scalar, null);
    assertThat(renderer.render(language, spec(list, scalar))).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void parameterOnlyNodesAreDiscoveredAndDoNotEmitUnusedClasses(Language language) {
    var draft =
        spec(new TypeNode(TypeKind.INT, null, null), node(TypeKind.BINARY_TREE, TypeKind.LONG));
    assertThat(renderer.render(language, draft)).contains("TreeNode").doesNotContain("ListNode");
  }

  @Test
  void javaAndCppUseTheExactPrimitiveValueTypes() {
    String java =
        renderer.render(
            Language.JAVA,
            spec(
                node(TypeKind.LINKED_LIST, TypeKind.LONG),
                node(TypeKind.BINARY_TREE, TypeKind.BOOLEAN)));
    assertThat(java).contains("public long val", "public boolean val");
    String cpp =
        renderer.render(
            Language.CPP,
            spec(
                node(TypeKind.LINKED_LIST, TypeKind.DOUBLE),
                node(TypeKind.BINARY_TREE, TypeKind.LONG)));
    assertThat(cpp).contains("double val", "long long val", "ListNode* next", "TreeNode* left");
  }

  @Test
  void pythonConstructorsHaveTypedDefaults() {
    String source =
        renderer.render(
            Language.PYTHON,
            spec(
                node(TypeKind.LINKED_LIST, TypeKind.BOOLEAN),
                node(TypeKind.BINARY_TREE, TypeKind.DOUBLE)));
    assertThat(source).contains("val=False", "val=0.0");
  }

  @Test
  void deduplicatesRepeatedNodeTypes() {
    TypeNode list = node(TypeKind.LINKED_LIST, TypeKind.INT);
    String source = renderer.render(Language.JAVA, spec(list, list));
    assertThat(source.split("class ListNode", -1)).hasSize(2);
    assertThat(source).doesNotContain("TreeNode");
  }

  @Test
  void invalidInputUsesExistingDomainCodes() {
    var integer = new TypeNode(TypeKind.INT, null, null);
    assertCode(() -> renderer.render(null, spec(integer, integer)), "HRN-010");
    assertCode(() -> renderer.render(Language.JAVA, null), "HRN-005");
    assertCode(
        () ->
            renderer.render(
                Language.JAVA, spec(node(TypeKind.LINKED_LIST, TypeKind.STRING), integer)),
        "HRN-004");
    assertCode(
        () ->
            renderer.render(
                Language.JAVA,
                spec(
                    node(TypeKind.LINKED_LIST, TypeKind.INT),
                    node(TypeKind.LINKED_LIST, TypeKind.LONG))),
        "HRN-004");
    var invalidName =
        new FunctionSpecDraft(
            "bad name", integer, List.of(), Map.of(), MatchingStrategy.EXACT, null);
    assertCode(() -> renderer.render(Language.JAVA, invalidName), "HRN-006");
  }

  @Test
  void guardIsLoadedFromThePackagedResourceWithoutExecutingIt() throws IOException {
    String guard = ClasspathPythonSignatureGuard.load();
    try (var resource =
        getClass().getResourceAsStream("/harness-templates/python/signature-guard.py")) {
      assertThat(resource).isNotNull();
      assertThat(guard).isEqualTo(new String(resource.readAllBytes(), StandardCharsets.UTF_8));
    }
    assertThat(guard)
        .contains("@@APH-HARNESS-SIGNATURE-ERROR@@", "SystemExit(92)", ".bind(*apH_args)");
    assertThat(guard).doesNotContain("eval(", "exec(");
  }

  private static TypeNode node(TypeKind kind, TypeKind value) {
    return new TypeNode(kind, new TypeNode(value, null, null), null);
  }

  private static FunctionSpecDraft spec(TypeNode result, TypeNode parameter) {
    return new FunctionSpecDraft(
        "solve",
        result,
        List.of(new FunctionParameter("value", parameter)),
        Map.of(),
        MatchingStrategy.EXACT,
        null);
  }

  private static void assertCode(Runnable operation, String code) {
    assertThatThrownBy(operation::run)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo(code));
  }
}
