package com.algoprep.harness.application.compile;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.MapCompileErrorCommand;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.prepare.FunctionWrapperRenderer;
import com.algoprep.harness.domain.model.CompileReportStatus;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GeneratedWrapperCompileMappingTest {
  @TempDir Path directory;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void realJavacDiagnosticsRespectGeneratedUserAndCallSiteRegions(boolean signatureMismatch)
      throws IOException {
    TypeNode type = new TypeNode(TypeKind.INT, null, null);
    var draft =
        new FunctionSpecDraft(
            "solve",
            type,
            List.of(new FunctionParameter("x", type)),
            Map.of(),
            MatchingStrategy.EXACT,
            null);
    String user =
        signatureMismatch
            ? "class Solution { public int solve() { return 7; } }\n"
            : "class Solution { public int solve(int x) { return unknown; } }\n";
    var program = new FunctionWrapperRenderer().render(Language.JAVA, draft, user);
    Path source = directory.resolve("Solution.java");
    Files.writeString(source, program.text(), StandardCharsets.UTF_8);
    String raw = compileFailure(source);
    var report =
        new MapCompileErrorHandler((id, text, truncated) -> {})
            .map(
                new MapCompileErrorCommand(
                    Language.JAVA,
                    SubmissionModel.FUNCTION_WRAPPER,
                    raw,
                    program.lineMap(),
                    "public int solve(int x)",
                    null));
    if (signatureMismatch) {
      assertThat(report.fixedMessageKey()).isEqualTo("harness.compile.signatureMismatch");
    } else {
      assertThat(report.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
      assertThat(report.diagnostics())
          .singleElement()
          .satisfies(item -> assertThat(item.line()).isEqualTo(1));
    }
  }

  private String compileFailure(Path source) throws IOException {
    var compiler = ToolProvider.getSystemJavaCompiler();
    StringWriter raw = new StringWriter();
    try (var manager =
        compiler.getStandardFileManager(null, java.util.Locale.ROOT, StandardCharsets.UTF_8)) {
      boolean compiled =
          compiler
              .getTask(
                  raw,
                  manager,
                  null,
                  List.of(
                      "--release",
                      "21",
                      "-proc:none",
                      "-encoding",
                      "UTF-8",
                      "-Xmaxerrs",
                      "20",
                      "-d",
                      directory.toString()),
                  null,
                  manager.getJavaFileObjects(source))
              .call();
      assertThat(compiled).isFalse();
    }
    return raw.toString();
  }
}
