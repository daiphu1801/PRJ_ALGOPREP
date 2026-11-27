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
import com.algoprep.harness.domain.codegen.CppTypeCodeEmitter;
import com.algoprep.harness.domain.codegen.JavaTypeCodeEmitter;
import com.algoprep.harness.domain.model.RenderedProgram;
import com.algoprep.harness.domain.signature.FunctionSignatureDeclarations;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class FunctionWrapperRendererTest {
  private final FunctionWrapperRenderer renderer = new FunctionWrapperRenderer();

  record Fixture(String id, TypeNode type, String input, String output) {}

  static TypeNode scalar(TypeKind kind) {
    return new TypeNode(kind, null, null);
  }

  static FunctionSpecDraft spec(TypeNode type) {
    return new FunctionSpecDraft(
        "echoValue",
        type,
        List.of(new FunctionParameter("value", type)),
        Map.of(),
        MatchingStrategy.EXACT,
        null);
  }

  static Stream<Arguments> cases() {
    return fixtures().stream()
        .flatMap(
            fixture ->
                Stream.of(Language.values()).map(language -> Arguments.of(language, fixture)));
  }

  static List<Fixture> fixtures() {
    List<Fixture> cases = new ArrayList<>();
    addScalars(cases);
    addContainers(cases);
    addNodes(cases);
    cases.add(new Fixture("empty-string", scalar(TypeKind.STRING), "\n", "\n"));
    cases.add(
        new Fixture(
            "unicode-string", scalar(TypeKind.STRING), "Tiếng Việt 😀\n", "Tiếng Việt 😀\n"));
    cases.add(
        new Fixture(
            "long-min", scalar(TypeKind.LONG), "-9223372036854775808\n", "-9223372036854775808\n"));
    cases.add(
        new Fixture(
            "empty-array", new TypeNode(TypeKind.ARRAY, scalar(TypeKind.INT), 1), "0\n\n", "\n"));
    cases.add(
        new Fixture(
            "empty-list",
            new TypeNode(TypeKind.LIST, scalar(TypeKind.INT), null),
            "0\n\n",
            "[]\n"));
    cases.add(
        new Fixture(
            "empty-link",
            new TypeNode(TypeKind.LINKED_LIST, scalar(TypeKind.INT), null),
            "0\n\n",
            "0\n\n"));
    cases.add(
        new Fixture(
            "empty-tree",
            new TypeNode(TypeKind.BINARY_TREE, scalar(TypeKind.INT), null),
            "#\n",
            "#\n"));
    cases.add(
        new Fixture(
            "short-tree",
            new TypeNode(TypeKind.BINARY_TREE, scalar(TypeKind.INT), null),
            "1 2\n",
            "1 2\n"));
    return cases;
  }

  static void addScalars(List<Fixture> cases) {
    cases.add(new Fixture("INT", scalar(TypeKind.INT), " +2147483647 \r\n", "2147483647\n"));
    cases.add(
        new Fixture(
            "LONG", scalar(TypeKind.LONG), "9223372036854775807\n", "9223372036854775807\n"));
    cases.add(new Fixture("DOUBLE", scalar(TypeKind.DOUBLE), "-0.0\n", "0.000000000000\n"));
    cases.add(new Fixture("BOOLEAN", scalar(TypeKind.BOOLEAN), "true\n", "true\n"));
    cases.add(new Fixture("CHAR", scalar(TypeKind.CHAR), "~\n", "~\n"));
    cases.add(new Fixture("STRING", scalar(TypeKind.STRING), "  a\\\"b  \n", "  a\\\"b  \n"));
  }

  static void addContainers(List<Fixture> cases) {
    for (TypeKind kind :
        List.of(
            TypeKind.INT,
            TypeKind.LONG,
            TypeKind.DOUBLE,
            TypeKind.BOOLEAN,
            TypeKind.CHAR,
            TypeKind.STRING)) {
      String token =
          kind == TypeKind.BOOLEAN
              ? "false"
              : kind == TypeKind.CHAR ? "x" : kind == TypeKind.STRING ? "a\\\"b" : "2";
      String printed = kind == TypeKind.DOUBLE ? "2.000000000000" : token;
      String quoted =
          kind == TypeKind.STRING ? "\"a\\\\\\\"b\"" : kind == TypeKind.CHAR ? "\"x\"" : printed;
      TypeNode nested = scalar(kind);
      for (int depth = 1; depth <= 3; depth++) {
        TypeNode array = new TypeNode(TypeKind.ARRAY, scalar(kind), depth);
        String input = ("1 ".repeat(depth).trim()) + "\n" + token + "\n";
        String output =
            kind == TypeKind.STRING ? "[".repeat(depth) + quoted + "]".repeat(depth) : printed;
        cases.add(new Fixture("array-" + kind + "-" + depth, array, input, output + "\n"));
        nested = new TypeNode(TypeKind.LIST, nested, null);
        cases.add(
            new Fixture(
                "list-" + kind + "-" + depth,
                nested,
                "1\n".repeat(depth) + token + "\n",
                "[".repeat(depth) + quoted + "]".repeat(depth) + "\n"));
      }
    }
    cases.add(
        new Fixture(
            "array3-blocks",
            new TypeNode(TypeKind.ARRAY, scalar(TypeKind.INT), 3),
            "2 2 2\n1 2\n3 4\n5 6\n7 8\n",
            "1 2\n3 4\n\n5 6\n7 8\n"));
    TypeNode list = new TypeNode(TypeKind.LIST, scalar(TypeKind.INT), null);
    cases.add(
        new Fixture(
            "ragged-list",
            new TypeNode(TypeKind.LIST, list, null),
            "3\n2\n1 2\n0\n\n1\n3\n",
            "[[1,2],[],[3]]\n"));
  }

  static void addNodes(List<Fixture> cases) {
    for (TypeKind kind : List.of(TypeKind.INT, TypeKind.LONG, TypeKind.DOUBLE, TypeKind.BOOLEAN)) {
      String value = kind == TypeKind.BOOLEAN ? "true" : "2";
      String printed = kind == TypeKind.DOUBLE ? "2.000000000000" : value;
      cases.add(
          new Fixture(
              "link-" + kind,
              new TypeNode(TypeKind.LINKED_LIST, scalar(kind), null),
              "2\n" + value + " " + value + "\n",
              "2\n" + printed + " " + printed + "\n"));
      cases.add(
          new Fixture(
              "tree-" + kind,
              new TypeNode(TypeKind.BINARY_TREE, scalar(kind), null),
              value + " # " + value + " # #\n",
              printed + " # " + printed + "\n"));
    }
  }

  @ParameterizedTest
  @MethodSource("cases")
  void emitsDeterministicProgramsAndExportsActualRoundTripFixtures(
      Language language, Fixture fixture) throws IOException {
    var draft = spec(fixture.type());
    String user = echoUser(language, draft);
    RenderedProgram program = renderer.render(language, draft, user);
    assertThat(program).isEqualTo(renderer.render(language, draft, user));
    assertThat(program.text()).doesNotContain("{{", "\r");
    assertThat(
            program
                .text()
                .lines()
                .skip(program.lineMap().prefixLineCount())
                .limit(program.lineMap().userLineCount())
                .toList())
        .containsExactlyElementsOf(user.lines().toList());
    assertThat(program.lineMap().callSiteEndLine())
        .isEqualTo(program.lineMap().callSiteStartLine());
    Path directory = Path.of("target", "f3-wrapper-probe", fixture.id(), language.name());
    Files.createDirectories(directory);
    Files.writeString(
        directory.resolve(program.sourceFileName()), program.text(), StandardCharsets.UTF_8);
    Files.writeString(directory.resolve("input.txt"), fixture.input(), StandardCharsets.UTF_8);
    Files.writeString(directory.resolve("expected.txt"), fixture.output(), StandardCharsets.UTF_8);
  }

  static String echoUser(Language language, FunctionSpecDraft draft) {
    String declaration = FunctionSignatureDeclarations.render(draft).get(language);
    if (language == Language.PYTHON) {
      return "class Solution:\n    " + declaration + "\n        return value\n";
    }
    return "class Solution {\npublic:\n"
            .replace("public:\n", language == Language.CPP ? "public:\n" : "")
        + "    "
        + declaration
        + " { return value; }\n}\n"
        + (language == Language.CPP ? ";\n" : "");
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void supportsNoArgumentsAndLanguageOverrides(Language language) throws IOException {
    var draft =
        new FunctionSpecDraft(
            "unusedName",
            scalar(TypeKind.INT),
            List.of(),
            Map.of("JAVA", "solve", "CPP", "solve", "PYTHON", "solve"),
            MatchingStrategy.EXACT,
            null);
    String user =
        language == Language.PYTHON
            ? "class Solution:\n def solve(self): return 7\n"
            : language == Language.JAVA
                ? "class Solution { public int solve() { return 7; } }\n"
                : "class Solution { public: int solve() { return 7; } };\n";
    var result = renderer.render(language, draft, user);
    assertThat(result.text()).contains("solve").doesNotContain("unusedName", "apH_p0");
    Path directory = Path.of("target", "f3-wrapper-probe", "no-param", language.name());
    Files.createDirectories(directory);
    Files.writeString(
        directory.resolve(result.sourceFileName()), result.text(), StandardCharsets.UTF_8);
    Files.writeString(directory.resolve("input.txt"), "", StandardCharsets.UTF_8);
    Files.writeString(directory.resolve("expected.txt"), "7\n", StandardCharsets.UTF_8);
  }

  @Test
  void validatesBeforeEmittingAndRejectsUnsupportedEmitterTypes() {
    var valid = spec(scalar(TypeKind.INT));
    assertCode(() -> renderer.render(null, valid, "x"), "HRN-010");
    assertCode(() -> renderer.render(Language.JAVA, null, "x"), "HRN-005");
    assertCode(() -> renderer.render(Language.JAVA, spec(scalar(TypeKind.OTHER)), "x"), "HRN-004");
    var hard =
        new FunctionSpecDraft(
            "apHReserved",
            valid.returnType(),
            valid.parameters(),
            Map.of(),
            MatchingStrategy.EXACT,
            null);
    assertCode(() -> renderer.render(Language.JAVA, hard, "x"), "HRN-006");
    assertCode(() -> renderer.render(Language.JAVA, valid, " "), "HRN-020");
    assertThatThrownBy(() -> new JavaTypeCodeEmitter().type(scalar(TypeKind.OTHER)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new CppTypeCodeEmitter().type(scalar(TypeKind.OTHER)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  static void assertCode(Runnable call, String code) {
    assertThatThrownBy(call::run)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo(code));
  }
}
