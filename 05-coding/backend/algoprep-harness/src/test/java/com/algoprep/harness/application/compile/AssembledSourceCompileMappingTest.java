package com.algoprep.harness.application.compile;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.MapCompileErrorCommand;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.prepare.ProgramSourceAssembler;
import com.algoprep.harness.domain.model.CompileReportStatus;
import com.algoprep.harness.domain.model.WrapperSource;
import org.junit.jupiter.api.Test;

class AssembledSourceCompileMappingTest {
  private final ProgramSourceAssembler assembler = new ProgramSourceAssembler();
  private final MapCompileErrorHandler mapper = new MapCompileErrorHandler((id, raw, cut) -> {});

  @Test
  void wrapperRegionsAreAcceptedByP3AndUserLinesAreTranslated() {
    var program =
        assembler.functionWrapper(
            Language.JAVA,
            "class Solution {\n}\n",
            new WrapperSource("import java.util.*;\n", "class ApHRunner {\n", "call();\n", "}\n"));
    var result =
        mapper.map(
            new MapCompileErrorCommand(
                Language.JAVA,
                SubmissionModel.FUNCTION_WRAPPER,
                "Solution.java:3: error: illegal start of expression\n",
                program.lineMap(),
                null,
                null));
    assertThat(result.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
    assertThat(result.diagnostics())
        .singleElement()
        .satisfies(item -> assertThat(item.line()).isEqualTo(2));
    var call =
        mapper.map(
            new MapCompileErrorCommand(
                Language.JAVA,
                SubmissionModel.FUNCTION_WRAPPER,
                "Solution.java:5: error: cannot find symbol\n",
                program.lineMap(),
                "public int solve()",
                null));
    assertThat(call.fixedMessageKey()).isEqualTo("harness.compile.signatureMismatch");
    var internal =
        mapper.map(
            new MapCompileErrorCommand(
                Language.JAVA,
                SubmissionModel.FUNCTION_WRAPPER,
                "Solution.java:4: error: illegal start of expression\n",
                program.lineMap(),
                null,
                null));
    assertThat(internal.status()).isEqualTo(CompileReportStatus.HARNESS_INTERNAL);
    assertThat(internal.diagnostics()).isEmpty();
  }

  @Test
  void standardIoKeepsCompilerLineNumbers() {
    var program = assembler.standardIo(Language.CPP, "first\r\nsecond");
    var result =
        mapper.map(
            new MapCompileErrorCommand(
                Language.CPP,
                SubmissionModel.STANDARD_IO,
                "main.cpp:2:1: error: invalid declaration\n",
                program.lineMap(),
                null,
                null));
    assertThat(result.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
    assertThat(result.diagnostics())
        .singleElement()
        .satisfies(item -> assertThat(item.line()).isEqualTo(2));
  }
}
