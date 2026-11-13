package com.algoprep.harness.application.prepare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.infrastructure.gojudge.GoJudgePayloadAssembler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProgramPackagingTest {
  private final ProgramPackaging packaging = new ProgramPackaging(new GoJudgePayloadAssembler());

  @ParameterizedTest
  @EnumSource(Language.class)
  void standardIoKeepsSourceInDataAndUsesFixedCommands(Language language) {
    String source = "// $(secret) `command` {{placeholder}}\n";
    var packaged = packaging.standardIo(language, source);
    assertThat(packaged.resultSentinel()).isNull();
    assertThat(packaged.signatureText()).isNull();
    assertThat(packaged.lineMap().prefixLineCount()).isZero();
    assertThat(packaged.payload().engine()).isEqualTo("go-judge");
    var compile = (Map<?, ?>) packaged.payload().body().get("compileCmd");
    assertThat(compile.toString()).contains(source.trim());
    assertThat(compile.get("args").toString()).doesNotContain("secret", "command");
    assertThat(compile.keySet().stream().map(Object::toString))
        .doesNotContain("files", "cpuLimit", "clockLimit", "memoryLimit", "fileIds");
    var run = (Map<?, ?>) packaged.payload().body().get("runCmd");
    assertThat(run.get("copyIn").toString())
        .isEqualTo(
            language == Language.PYTHON
                ? "{" + packaged.lineMap().sourceFileName() + "={content=" + source + "}}"
                : "{}");
    if (language == Language.JAVA) {
      assertThat(run.get("args").toString()).contains("Main");
    }
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void wrapperPackagesRenderedSourceAndControlledSignature(Language language) {
    var type = new TypeNode(TypeKind.INT, null, null);
    var draft =
        new FunctionSpecDraft(
            "echo",
            type,
            List.of(new FunctionParameter("value", type)),
            Map.of(),
            MatchingStrategy.EXACT,
            null);
    var packaged = packaging.functionWrapper(language, draft, "// source\n");
    assertThat(packaged.resultSentinel()).isEqualTo("@@APH-RESULT@@");
    assertThat(packaged.signatureText()).contains("echo", "value");
    assertThat(packaged.lineMap().callSiteStartLine()).isGreaterThan(0);
    assertThat(packaged.payload().body().toString()).doesNotContain("APH_MAX_RESULT_BYTES=");
    if (language == Language.JAVA) {
      assertThat(packaged.payload().body().toString()).contains("ApHRunner");
    }
  }

  @Test
  void rejectsInvalidSpecificationBeforeRendering() {
    var type = new TypeNode(TypeKind.INT, null, null);
    var draft =
        new FunctionSpecDraft("bad-name", type, List.of(), Map.of(), MatchingStrategy.EXACT, null);
    assertThatThrownBy(() -> packaging.functionWrapper(Language.JAVA, draft, "code"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid or unsupported function specification");
  }

  @Test
  void nullableCompilePlanStillCarriesSourceOnlyAsData() {
    var plan =
        new com.algoprep.harness.application.dto.ExecutionPlan(
            List.of(
                new com.algoprep.harness.application.dto.ExecutionPlan.SourceFile(
                    "main.py", "print(1)\n")),
            null,
            new com.algoprep.harness.application.dto.ExecutionPlan.Command(
                List.of("/usr/bin/python3", "main.py"), List.of("LC_ALL=C")),
            null);
    var payload = new GoJudgePayloadAssembler().assemble(plan);
    assertThat(payload.body().get("compileCmd")).isNull();
    assertThat(payload.body().get("runCmd").toString()).contains("print(1)");
    assertThatThrownBy(() -> payload.body().put("runCmd", "changed"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
