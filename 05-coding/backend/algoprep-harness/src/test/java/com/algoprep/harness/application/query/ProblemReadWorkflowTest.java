package com.algoprep.harness.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.GetStarterCodeQuery;
import com.algoprep.harness.application.dto.PrepareSubmissionCommand;
import com.algoprep.harness.application.dto.ProblemSpecView;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.out.ProblemSpecReaderPort;
import com.algoprep.harness.application.ports.out.StarterCodeAccessPort;
import com.algoprep.harness.application.prepare.PrepareSubmissionCommandHandler;
import com.algoprep.harness.application.prepare.ProgramPackaging;
import com.algoprep.harness.infrastructure.gojudge.GoJudgePayloadAssembler;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ProblemReadWorkflowTest {
  private final UUID id = UUID.randomUUID();
  private final UUID actor = UUID.randomUUID();
  private final ProblemSpecReaderPort reader = mock(ProblemSpecReaderPort.class);
  private final StarterCodeAccessPort access = mock(StarterCodeAccessPort.class);
  private final CurrentProblemSpecification current = new CurrentProblemSpecification(reader);
  private final GetStarterCodeQueryHandler starter =
      new GetStarterCodeQueryHandler(access, current);
  private final PrepareSubmissionCommandHandler prepare =
      new PrepareSubmissionCommandHandler(
          current, new ProgramPackaging(new GoJudgePayloadAssembler()));
  private static final TypeNode INT = new TypeNode(TypeKind.INT, null, null);

  private FunctionSpecDraft draft(String name, TypeNode type) {
    return new FunctionSpecDraft(
        name,
        type,
        List.of(new FunctionParameter("value", INT)),
        Map.of(),
        MatchingStrategy.EPSILON,
        new BigDecimal("0.000000000001"));
  }

  private void read(FunctionSpecDraft signature, boolean supported) {
    when(reader.findSpec(id))
        .thenReturn(
            Optional.of(new ProblemSpecView(id, 130, signature, "input", "output", supported)));
    when(access.canRead(id, actor, "STUDENT")).thenReturn(true);
  }

  private PrepareSubmissionCommand command(
      Language language, SubmissionModel model, String source) {
    return new PrepareSubmissionCommand(id, language, model, source, 130, "private-correlation");
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void starterAndPreparedProgramUseTheReadVersionForBothModels(Language language) {
    read(draft("echo", INT), true);
    for (var model : SubmissionModel.values()) {
      var query = new GetStarterCodeQuery(id, language, model, actor, "STUDENT");
      var started = starter.getStarterCode(query);
      assertThat(started.specVersion()).isEqualTo(130);
      assertThat(started.submissionModel()).isEqualTo(model);
      assertThat(started.sourceCode()).isNotBlank();
      var cmd = command(language, model, "user-source");
      var result = prepare.prepare(cmd);
      assertThat(prepare.prepare(cmd)).isEqualTo(result);
      assertThat(result.specVersion()).isEqualTo(130);
      assertThat(result.harnessInputErrorExitCode()).isEqualTo(90);
      assertThat(result.matchSpec().epsilonValue()).isEqualByComparingTo("0.000000000001");
      assertThat(result.payload().body().toString()).doesNotContain("private-correlation");
      if (model == SubmissionModel.STANDARD_IO) {
        assertThat(result.resultSentinel()).isNull();
        assertThat(result.signatureText()).isNull();
        assertThat(result.matchSpec().returnType()).isNull();
        assertThat(result.lineMap().prefixLineCount()).isZero();
      } else {
        assertThat(result.resultSentinel()).isEqualTo("@@APH-RESULT@@");
        assertThat(result.signatureText()).contains("echo");
        assertThat(result.matchSpec().returnType()).isEqualTo(INT);
      }
    }
  }

  @Test
  void deniedAccessNeverReadsThePrivateSpecification() {
    assertCode(
        () ->
            starter.getStarterCode(
                new GetStarterCodeQuery(
                    id, Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, actor, "STUDENT")),
        "HRN-001");
    verifyNoInteractions(reader);
  }

  @Test
  void accessFailureIsUnavailableWithoutOriginalCause() {
    when(access.canRead(id, actor, "STUDENT"))
        .thenThrow(new IllegalStateException("private-query"));
    assertCode(
        () ->
            starter.getStarterCode(
                new GetStarterCodeQuery(
                    id, Language.PYTHON, SubmissionModel.STANDARD_IO, actor, "STUDENT")),
        "HRN-003");
    verifyNoInteractions(reader);
  }

  @Test
  void missingAndFailedReadsAreDistinctWithoutLeakingUpstreamErrors() {
    when(reader.findSpec(id)).thenReturn(Optional.empty());
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.STANDARD_IO, "x")),
        "HRN-001");
    when(reader.findSpec(id)).thenThrow(new IllegalStateException("private-query"));
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.STANDARD_IO, "x")),
        "HRN-003");
    doThrow(new DomainRuleViolationException("HRN-006", "private-json")).when(reader).findSpec(id);
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.STANDARD_IO, "x")),
        "HRN-006");
  }

  @Test
  void versionMismatchStopsBeforeRenderingAndReadsExactlyOnce() {
    read(draft("echo", INT), true);
    var cmd =
        new PrepareSubmissionCommand(
            id, Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, "x", 129, null);
    assertCode(() -> prepare.prepare(cmd), "HRN-002");
    verify(reader).findSpec(id);
  }

  @Test
  void absentSignatureTakesPrecedenceOverUnsupportedFlag() {
    read(draft(null, null), false);
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, "x")),
        "HRN-005");
    assertCode(
        () ->
            starter.getStarterCode(
                new GetStarterCodeQuery(
                    id, Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, actor, "STUDENT")),
        "HRN-005");
    assertThat(
            prepare
                .prepare(command(Language.PYTHON, SubmissionModel.STANDARD_IO, "x"))
                .specVersion())
        .isEqualTo(130);
  }

  @Test
  void standardIoWorksWhileWrapperFlagAndDefensiveEvaluationBlockWrapper() {
    read(draft("echo", INT), false);
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, "x")),
        "HRN-004");
    assertThat(
            prepare
                .prepare(command(Language.PYTHON, SubmissionModel.STANDARD_IO, "x"))
                .signatureText())
        .isNull();
    read(draft("echo", new TypeNode(TypeKind.OTHER, null, null)), true);
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, "x")),
        "HRN-006");
    read(draft("not an identifier", INT), true);
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, "x")),
        "HRN-006");
  }

  static Stream<Arguments> invalidSources() {
    return Stream.of(
        Arguments.of(null, "HRN-020"),
        Arguments.of(" \n", "HRN-020"),
        Arguments.of("\uFEFF", "HRN-020"),
        Arguments.of("x".repeat(65537), "HRN-021"),
        Arguments.of("x\0", "HRN-022"));
  }

  @ParameterizedTest(name = "invalid source: {1}")
  @MethodSource("invalidSources")
  void invalidSourceFailsBeforeAnyRead(String source, String error) {
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.STANDARD_IO, source)),
        error);
    verifyNoInteractions(reader);
  }

  @Test
  void acceptsExactUtf8ByteLimitWithoutCountingTheGeneratedFinalNewline() {
    read(draft("echo", INT), true);
    String source = "é".repeat(32768);
    var result = prepare.prepare(command(Language.PYTHON, SubmissionModel.STANDARD_IO, source));
    assertThat(result.lineMap().userLineCount()).isEqualTo(1);
    verify(reader).findSpec(id);
  }

  @Test
  void invalidRequestStructuresAndLanguageDoNotReadSpec() {
    assertCode(() -> prepare.prepare(null), "HRN-031");
    assertCode(() -> prepare.prepare(command(null, SubmissionModel.STANDARD_IO, "x")), "HRN-010");
    assertCode(() -> prepare.prepare(command(Language.PYTHON, null, "x")), "HRN-031");
    assertCode(() -> starter.getStarterCode(null), "HRN-031");
    assertCode(
        () ->
            starter.getStarterCode(
                new GetStarterCodeQuery(id, null, SubmissionModel.STANDARD_IO, actor, "STUDENT")),
        "HRN-010");
    assertCode(() -> current.load(null), "HRN-001");
    verifyNoInteractions(reader, access);
  }

  @Test
  void malformedReturnedIdentityOrVersionFailsClosed() {
    when(reader.findSpec(id))
        .thenReturn(
            Optional.of(
                new ProblemSpecView(UUID.randomUUID(), 130, draft("echo", INT), "", "", true)));
    assertCode(() -> current.load(id), "HRN-006");
    when(reader.findSpec(id))
        .thenReturn(Optional.of(new ProblemSpecView(id, 0, null, "", "", true)));
    assertCode(() -> current.load(id), "HRN-006");
    verify(reader, times(2)).findSpec(id);
  }

  private static void assertCode(Runnable action, String code) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            AlgoPrepException.class,
            error -> {
              assertThat(error.errorCode()).isEqualTo(code);
              assertThat(error.getCause()).isNull();
              assertThat(error.getMessage()).doesNotContain("private-");
            });
  }
}
