package com.algoprep.harness.application.prepare;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.domain.signature.FunctionSignatureDeclarations;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class WrapperBehaviorFixturesTest {
  record Behavior(
      String name, TypeNode type, Map<Language, String> body, String output, int exit) {}

  static TypeNode scalar(TypeKind kind) {
    return new TypeNode(kind, null, null);
  }

  static Map<Language, String> bodies(String java, String cpp, String python) {
    return Map.of(Language.JAVA, java, Language.CPP, cpp, Language.PYTHON, python);
  }

  static Stream<Arguments> behaviors() {
    return Stream.concat(Stream.concat(numberBehaviors(), valueBehaviors()), nodeBehaviors())
        .flatMap(
            behavior ->
                Stream.of(Language.values()).map(language -> Arguments.of(language, behavior)));
  }

  static Stream<Behavior> numberBehaviors() {
    return Stream.of(
        new Behavior(
            "nan-result",
            scalar(TypeKind.DOUBLE),
            bodies("return Double.NaN;", "return NAN;", "return float('nan')"),
            "NaN\n",
            0),
        new Behavior(
            "inf-result",
            scalar(TypeKind.DOUBLE),
            bodies("return Double.POSITIVE_INFINITY;", "return INFINITY;", "return float('inf')"),
            "Infinity\n",
            0),
        new Behavior(
            "negative-inf-result",
            scalar(TypeKind.DOUBLE),
            bodies("return Double.NEGATIVE_INFINITY;", "return -INFINITY;", "return -float('inf')"),
            "-Infinity\n",
            0),
        new Behavior(
            "rounded-zero",
            scalar(TypeKind.DOUBLE),
            bodies("return -1e-15;", "return -1e-15;", "return -1e-15"),
            "0.000000000000\n",
            0));
  }

  static Stream<Behavior> valueBehaviors() {
    return Stream.of(
        new Behavior(
            "escaped-result",
            new TypeNode(TypeKind.LIST, scalar(TypeKind.STRING), null),
            bodies(
                "return List.of(\"a\\nb\", \"\\\\\\\"😀\");",
                "return {\"a\\nb\", \"\\\\\\\"😀\"};",
                "return ['a\\nb', '\\\\\"😀']"),
            "[\"a\\nb\",\"\\\\\\\"😀\"]\n",
            0),
        new Behavior(
            "debug-result",
            scalar(TypeKind.INT),
            bodies(
                "System.out.print(\"debug\"); return 7;",
                "std::cout << \"debug\"; return 7;",
                "print('debug', end=''); return 7"),
            "7\n",
            0),
        new Behavior(
            "null-result",
            scalar(TypeKind.STRING),
            bodies("return null;", "throw std::runtime_error(\"body error\");", "return None"),
            "",
            1),
        new Behavior(
            "body-error",
            scalar(TypeKind.INT),
            bodies(
                "throw new IllegalArgumentException(\"body error\");",
                "throw std::runtime_error(\"body error\");",
                "raise TypeError('body error')"),
            "",
            1));
  }

  static Stream<Behavior> nodeBehaviors() {
    return Stream.of(
        new Behavior(
            "link-cycle",
            new TypeNode(TypeKind.LINKED_LIST, scalar(TypeKind.INT), null),
            bodies(
                "ListNode n = new ListNode(1); n.next = n; return n;",
                "auto n = new ListNode(1); n->next = n; return n;",
                "n = ListNode(1); n.next = n; return n"),
            "",
            91),
        new Behavior(
            "tree-cycle",
            new TypeNode(TypeKind.BINARY_TREE, scalar(TypeKind.INT), null),
            bodies(
                "TreeNode n = new TreeNode(1); n.left = n; return n;",
                "auto n = new TreeNode(1); n->left = n; return n;",
                "n = TreeNode(1); n.left = n; return n"),
            "",
            91));
  }

  @ParameterizedTest
  @MethodSource("behaviors")
  void exportsBehaviorPrograms(Language language, Behavior behavior) throws IOException {
    var draft =
        new FunctionSpecDraft(
            "solve", behavior.type(), List.of(), Map.of(), MatchingStrategy.EXACT, null);
    String declaration = FunctionSignatureDeclarations.render(draft).get(language);
    String user =
        language == Language.PYTHON
            ? "class Solution:\n    "
                + declaration
                + "\n        "
                + behavior.body().get(language)
                + "\n"
            : "class Solution {\n"
                + (language == Language.CPP ? "public:\n" : "")
                + declaration
                + " { "
                + behavior.body().get(language)
                + " }\n}"
                + (language == Language.CPP ? ";\n" : "\n");
    var program = new FunctionWrapperRenderer().render(language, draft, user);
    Path directory = Path.of("target", "f3-wrapper-probe", behavior.name(), language.name());
    Files.createDirectories(directory);
    Files.writeString(
        directory.resolve(program.sourceFileName()), program.text(), StandardCharsets.UTF_8);
    Files.writeString(directory.resolve("input.txt"), "", StandardCharsets.UTF_8);
    Files.writeString(directory.resolve("expected.txt"), behavior.output(), StandardCharsets.UTF_8);
    Files.writeString(
        directory.resolve("exit.txt"), Integer.toString(behavior.exit()), StandardCharsets.UTF_8);
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void exportsTwoSumWithPositionalArgumentsAndPreservesAdversarialUserComments(Language language)
      throws IOException {
    TypeNode array = new TypeNode(TypeKind.ARRAY, scalar(TypeKind.INT), 1);
    var draft =
        new FunctionSpecDraft(
            "twoSum",
            array,
            List.of(
                new FunctionParameter("nums", array),
                new FunctionParameter("target", scalar(TypeKind.INT))),
            Map.of(),
            MatchingStrategy.EXACT,
            null);
    String declaration = FunctionSignatureDeclarations.render(draft).get(language);
    String user =
        language == Language.PYTHON
            ? "# {{READ_BLOCK}} @@APH_USER_CODE@@ }\nclass Solution:\n    "
                + declaration
                + "\n        return [nums[0], target]\n"
            : "// {{READ_BLOCK}} @@APH_USER_CODE@@ }\nclass Solution {\n"
                + (language == Language.CPP ? "public:\n" : "")
                + declaration
                + " { return "
                + (language == Language.JAVA ? "new int[]" : "")
                + "{nums[0], target}; }\n}"
                + (language == Language.CPP ? ";\n" : "\n");
    var program = new FunctionWrapperRenderer().render(language, draft, user);
    assertThat(program.text()).contains(user);
    assertThat(
            program
                .text()
                .lines()
                .skip(program.lineMap().callSiteStartLine() - 1)
                .findFirst()
                .orElseThrow())
        .contains("apH_p0, apH_p1");
    Path directory = Path.of("target", "f3-wrapper-probe", "two-sum", language.name());
    Files.createDirectories(directory);
    Files.writeString(
        directory.resolve(program.sourceFileName()), program.text(), StandardCharsets.UTF_8);
    Files.writeString(directory.resolve("input.txt"), "4\n2 7 11 15\n9\n", StandardCharsets.UTF_8);
    Files.writeString(directory.resolve("expected.txt"), "2 9\n", StandardCharsets.UTF_8);
  }
}
