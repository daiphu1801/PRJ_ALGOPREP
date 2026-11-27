package com.algoprep.harness.application.prepare;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.FunctionSpecEvaluator;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.domain.model.RenderedProgram;
import com.algoprep.harness.domain.signature.FunctionSignatureDeclarations;
import com.algoprep.harness.infrastructure.template.ClasspathStarterCodeTemplates;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** DD F3 10.2: fixed full-source, starter, learner region, spec and line-map snapshots. */
class RequiredGoldenMatrixTest {
  private static final Path ROOT = Path.of("src/test/resources/golden/required-matrix");
  private static final TypeNode INT = scalar(TypeKind.INT);

  record Fixture(String id, FunctionSpecDraft draft, String javaName, String pythonName) {}

  static Stream<Arguments> matrix() {
    return fixtures().stream()
        .flatMap(
            fixture ->
                Stream.of(Language.values()).map(language -> Arguments.of(fixture, language)));
  }

  static List<Fixture> fixtures() {
    var cases = new ArrayList<>(scalarFixtures());
    addContainerFixtures(cases);
    var array = new TypeNode(TypeKind.ARRAY, INT, 1);
    var two = List.of(new FunctionParameter("nums", array), new FunctionParameter("target", INT));
    cases.add(new Fixture("two-sum", draft("two_sum", array, two, Map.of()), "twoSum", "two_sum"));
    cases.add(
        new Fixture(
            "name-override",
            draft("two_sum", array, two, Map.of("JAVA", "solve")),
            "solve",
            "two_sum"));
    cases.add(
        new Fixture(
            "camel-in-two-sum", draft("twoSum", array, two, Map.of()), "twoSum", "two_sum"));
    cases.add(
        new Fixture(
            "camel-in-http-server",
            draft("HTTPServer", INT, List.of(new FunctionParameter("value", INT)), Map.of()),
            "httpServer",
            "http_server"));
    cases.add(fixture("no-param", "echo", INT, List.of()));
    cases.add(echo("adversarial-user", INT));
    cases.add(new Fixture("standard-io", null, null, null));
    return List.copyOf(cases);
  }

  private static List<Fixture> scalarFixtures() {
    var cases = new ArrayList<Fixture>();
    List<TypeNode> scalars =
        Stream.of(TypeKind.values())
            .filter(TypeKind::isScalar)
            .map(RequiredGoldenMatrixTest::scalar)
            .toList();
    var parameters =
        IntStream.range(0, scalars.size())
            .mapToObj(i -> new FunctionParameter("p" + i, scalars.get(i)))
            .toList();
    for (TypeNode type : scalars) {
      cases.add(
          fixture(
              "scalars-" + type.kind().name().toLowerCase(Locale.ROOT), "echo", type, parameters));
    }
    return cases;
  }

  private static void addContainerFixtures(List<Fixture> cases) {
    for (int dimension = 1; dimension <= 3; dimension++) {
      cases.add(echo("array" + dimension, new TypeNode(TypeKind.ARRAY, INT, dimension)));
    }
    for (int dimension = 1; dimension <= 2; dimension++) {
      cases.add(
          echo(
              "array-string" + dimension,
              new TypeNode(TypeKind.ARRAY, scalar(TypeKind.STRING), dimension)));
    }
    cases.add(
        echo(
            "list-nested",
            new TypeNode(TypeKind.LIST, new TypeNode(TypeKind.LIST, INT, null), null)));
    cases.add(echo("list-string", new TypeNode(TypeKind.LIST, scalar(TypeKind.STRING), null)));
    cases.add(echo("linked-list", new TypeNode(TypeKind.LINKED_LIST, INT, null)));
    cases.add(echo("binary-tree", new TypeNode(TypeKind.BINARY_TREE, INT, null)));
  }

  private static Fixture echo(String id, TypeNode type) {
    return fixture(id, "echo", type, List.of(new FunctionParameter("value", type)));
  }

  private static Fixture fixture(
      String id, String name, TypeNode type, List<FunctionParameter> parameters) {
    return new Fixture(id, draft(name, type, parameters, Map.of()), name, name);
  }

  private static FunctionSpecDraft draft(
      String name,
      TypeNode type,
      List<FunctionParameter> parameters,
      Map<String, String> overrides) {
    return new FunctionSpecDraft(name, type, parameters, overrides, MatchingStrategy.EXACT, null);
  }

  private static TypeNode scalar(TypeKind kind) {
    return new TypeNode(kind, null, null);
  }

  @ParameterizedTest(name = "{0} / {1}")
  @MethodSource("matrix")
  void everyRequiredCaseMatchesFixedBytesAndIndependentSourceRegionOracle(
      Fixture fixture, Language language) throws IOException {
    boolean standard = fixture.draft() == null;
    if (!standard) {
      var evaluation = FunctionSpecEvaluator.evaluate(fixture.draft());
      assertThat(evaluation.violations()).isEmpty();
      assertThat(evaluation.wrapperSupported()).isTrue();
    }
    var model = standard ? SubmissionModel.STANDARD_IO : SubmissionModel.FUNCTION_WRAPPER;
    String starter =
        ClasspathStarterCodeTemplates.create().render(language, model, fixture.draft());
    String user = standard ? standardUser(language) : wrapperUser(fixture, language);
    var program =
        standard
            ? new ProgramSourceAssembler().standardIo(language, user)
            : new FunctionWrapperRenderer().render(language, fixture.draft(), user);
    String extension =
        switch (language) {
          case JAVA -> "java";
          case CPP -> "cpp";
          case PYTHON -> "py";
        };
    Path folder = ROOT.resolve(language.name().toLowerCase(Locale.ROOT)).resolve(fixture.id());
    snapshot(ROOT.resolve("specs").resolve(fixture.id() + ".json"), specJson(fixture));
    snapshot(folder.resolve("user." + extension), user);
    snapshot(folder.resolve("wrapper." + extension), program.text());
    snapshot(folder.resolve("starter." + extension), starter);
    snapshot(folder.resolve("linemap.json"), lineMapJson(program));
    verifyIndependentRegions(fixture, language, user, program);
  }

  private String standardUser(Language language) {
    return switch (language) {
      case JAVA ->
          "public class Main { public static void main(String[] args) { System.out.println(7); } }\n";
      case CPP -> "#include <iostream>\nint main() { std::cout << 7 << '\\n'; }\n";
      case PYTHON -> "print(7)\n";
    };
  }

  private String wrapperUser(Fixture fixture, Language language) {
    var draft = fixture.draft();
    String declaration = FunctionSignatureDeclarations.render(draft).get(language);
    String result =
        draft.parameters().stream()
            .filter(parameter -> parameter.type().equals(draft.returnType()))
            .map(FunctionParameter::name)
            .findFirst()
            .orElse("7");
    String comment =
        fixture.id().equals("adversarial-user")
            ? (language == Language.PYTHON ? "#" : "//")
                + " @@APH_USER_CODE@@ {{READ_BLOCK}} {{CALL_BLOCK}} }\n"
            : "";
    String body =
        language == Language.PYTHON
            ? "class Solution:\n    " + declaration + "\n        return " + result + "\n"
            : "class Solution {\n"
                + (language == Language.CPP ? "public:\n" : "")
                + "  "
                + declaration
                + " { return "
                + result
                + "; }\n}"
                + (language == Language.CPP ? ";" : "")
                + "\n";
    return comment + body + (fixture.id().equals("adversarial-user") ? "}\n" : "");
  }

  private void verifyIndependentRegions(
      Fixture fixture, Language language, String user, RenderedProgram program) {
    var map = program.lineMap();
    assertThat(map.userLineCount()).isEqualTo(user.lines().count());
    assertThat(map.sourceFileName()).isEqualTo(program.sourceFileName());
    List<String> lines = program.text().lines().toList();
    assertThat(
            String.join(
                    "\n",
                    lines.subList(
                        map.prefixLineCount(), map.prefixLineCount() + map.userLineCount()))
                + "\n")
        .isEqualTo(user);
    if (fixture.draft() == null) {
      assertThat(map.prefixLineCount()).isZero();
      assertThat(map.callSiteStartLine()).isNull();
      assertThat(map.callSiteEndLine()).isNull();
      assertThat(program.text()).isEqualTo(user);
    } else {
      String expectedName =
          language == Language.PYTHON
              ? fixture.pythonName()
              : fixture.id().equals("name-override") && language == Language.CPP
                  ? "twoSum"
                  : fixture.javaName();
      String args =
          IntStream.range(0, fixture.draft().parameters().size())
              .mapToObj(i -> "apH_p" + i)
              .collect(Collectors.joining(", "));
      assertThat(map.callSiteStartLine())
          .isGreaterThan(map.prefixLineCount() + map.userLineCount());
      assertThat(map.callSiteEndLine()).isEqualTo(map.callSiteStartLine());
      if (language == Language.PYTHON) {
        assertThat(program.text())
            .contains("apH_resolve_call(\"" + expectedName + "\", [" + args + "])");
        assertThat(lines.get(map.callSiteStartLine() - 1))
            .isEqualTo("apH_r = apH_fn(" + args + ")");
      } else {
        assertThat(lines.get(map.callSiteStartLine() - 1))
            .contains("Solution()." + expectedName + "(" + args + ")");
      }
      assertThat(map.isUserLine(map.prefixLineCount())).isFalse();
      assertThat(map.isUserLine(map.prefixLineCount() + 1)).isTrue();
      assertThat(map.isUserLine(map.prefixLineCount() + map.userLineCount() + 1)).isFalse();
    }
  }

  private void snapshot(Path path, String value) throws IOException {
    if (Boolean.getBoolean("golden.update")) {
      Files.createDirectories(path.getParent());
      Files.writeString(path, value, StandardCharsets.UTF_8);
    }
    assertThat(value.getBytes(StandardCharsets.UTF_8))
        .as(path.toString())
        .isEqualTo(Files.readAllBytes(path));
  }

  private String lineMapJson(RenderedProgram program) {
    var map = program.lineMap();
    return "{\"prefixLineCount\":"
        + map.prefixLineCount()
        + ",\"userLineCount\":"
        + map.userLineCount()
        + ",\"callSiteStartLine\":"
        + map.callSiteStartLine()
        + ",\"callSiteEndLine\":"
        + map.callSiteEndLine()
        + ",\"sourceFileName\":\""
        + map.sourceFileName()
        + "\"}\n";
  }

  private String specJson(Fixture fixture) {
    if (fixture.draft() == null) {
      return "null\n";
    }
    var draft = fixture.draft();
    return "{\"functionName\":\""
        + draft.functionName()
        + "\",\"returnType\":"
        + typeJson(draft.returnType())
        + ",\"parameters\":["
        + draft.parameters().stream()
            .map(p -> "{\"name\":\"" + p.name() + "\",\"type\":" + typeJson(p.type()) + "}")
            .collect(Collectors.joining(","))
        + "],\"nameOverrides\":"
        + (draft.nameOverrides().isEmpty() ? "{}" : "{\"JAVA\":\"solve\"}")
        + ",\"matchingStrategy\":\"EXACT\",\"epsilon\":null}\n";
  }

  private String typeJson(TypeNode type) {
    return "{\"kind\":\""
        + type.kind()
        + "\",\"of\":"
        + (type.of() == null ? "null" : typeJson(type.of()))
        + ",\"dimensions\":"
        + type.dimensions()
        + "}";
  }
}
