package com.algoprep.harness.application.prepare;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.MatchOutputCommand;
import com.algoprep.harness.application.dto.MatchSpec;
import com.algoprep.harness.application.dto.MatchVerdict;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.query.MatchOutputMatcher;
import com.algoprep.harness.domain.signature.FunctionSignatureDeclarations;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WrapperControlStringFixturesTest {
  @ParameterizedTest
  @EnumSource(Language.class)
  void exportsControlStringRoundTripsAndVerifiesCanonicalOutputWithP4(Language language)
      throws IOException {
    TypeNode type = new TypeNode(TypeKind.LIST, new TypeNode(TypeKind.STRING, null, null), null);
    var draft =
        new FunctionSpecDraft(
            "solve",
            type,
            List.of(new FunctionParameter("value", type)),
            Map.of(),
            MatchingStrategy.UNORDERED_SET,
            null);
    String declaration = FunctionSignatureDeclarations.render(draft).get(language);
    String user =
        language == Language.PYTHON
            ? "class Solution:\n    " + declaration + "\n        return value\n"
            : "class Solution {\n"
                + (language == Language.CPP ? "public:\n" : "")
                + declaration
                + " { return value; }\n}"
                + (language == Language.CPP ? ";\n" : "\n");
    var program = new FunctionWrapperRenderer().render(language, draft, user);
    String expected = "[\"a\\tb\\u0001\\b\\f\"]\n";
    var matched =
        new MatchOutputMatcher()
            .match(
                new MatchOutputCommand(
                    SubmissionModel.FUNCTION_WRAPPER,
                    new MatchSpec(MatchingStrategy.UNORDERED_SET, null, type),
                    "@@APH-RESULT@@",
                    "\n@@APH-RESULT@@\n" + expected,
                    expected));
    assertThat(matched.verdict()).isEqualTo(MatchVerdict.MATCH);
    Path directory = Path.of("target", "f3-wrapper-probe", "control-string", language.name());
    Files.createDirectories(directory);
    Files.writeString(
        directory.resolve(program.sourceFileName()), program.text(), StandardCharsets.UTF_8);
    Files.writeString(
        directory.resolve("input.txt"), "1\na\tb\u0001\b\f\n", StandardCharsets.UTF_8);
    Files.writeString(directory.resolve("expected.txt"), expected, StandardCharsets.UTF_8);
  }
}
