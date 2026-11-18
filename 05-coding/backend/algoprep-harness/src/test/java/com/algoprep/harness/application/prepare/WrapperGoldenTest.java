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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WrapperGoldenTest {
  @ParameterizedTest
  @EnumSource(Language.class)
  void matchesReviewedFullSourceSnapshotIncludingRuntimeAndCallSite(Language language)
      throws IOException {
    TypeNode integer = new TypeNode(TypeKind.INT, null, null);
    TypeNode array = new TypeNode(TypeKind.ARRAY, integer, 1);
    var draft =
        new FunctionSpecDraft(
            "twoSum",
            array,
            List.of(new FunctionParameter("nums", array), new FunctionParameter("target", integer)),
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
    Path golden =
        Path.of(
            "src/test/resources/golden/wrapper-two-sum", language.name(), program.sourceFileName());
    if (Boolean.getBoolean("golden.update")) {
      Files.writeString(golden, program.text(), StandardCharsets.UTF_8);
    }
    assertThat(program.text().getBytes(StandardCharsets.UTF_8))
        .isEqualTo(Files.readAllBytes(golden));
    assertThat(program.lineMap().userLineCount()).isEqualTo(user.lines().count());
  }
}
