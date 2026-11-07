package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.JudgeGradingDefaults;
import com.algoprep.problembank.application.dto.ReferenceRunReport.Command;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.application.ports.out.ReferenceRunQuotaPort;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RunReferenceSolutionQueryHandlerTest {
  private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

  private final ProblemAuthoringReadRepository repository =
      mock(ProblemAuthoringReadRepository.class);
  private final TestcaseObjectStorePort store = mock(TestcaseObjectStorePort.class);
  private final JudgeConfigReadPort config = mock(JudgeConfigReadPort.class);
  private final SampleSolutionRunnerPort runner = mock(SampleSolutionRunnerPort.class);
  private final ReferenceRunQuotaPort quota = mock(ReferenceRunQuotaPort.class);
  private final UUID actor = UUID.randomUUID();
  private final UUID problemId = UUID.randomUUID();
  private final List<AuthoringTestcase> cases = new ArrayList<>();
  private ProblemContentData.Solution stored =
      new ProblemContentData.Solution("PYTHON", "print(1)");
  private UUID author = actor;
  private String status = "UNPUBLISHED";
  private RunReferenceSolutionQueryHandler handler;

  @BeforeEach
  void setup() {
    when(config.getGradingDefaults()).thenReturn(new JudgeGradingDefaults(1000, 256, 64, 10));
    when(repository.find(eq(problemId), eq(true))).thenAnswer(c -> Optional.of(problem()));
    handler =
        new RunReferenceSolutionQueryHandler(
            repository, new ReferenceRunRequests(store, config), runner, quota, () -> NOW);
  }

  private AuthoringProblemRecord problem() {
    return new AuthoringProblemRecord(
        problemId,
        "C",
        "T",
        "s",
        null,
        UUID.randomUUID(),
        status,
        false,
        new ProblemContentData.Limits(1000, 256, 64, null),
        1,
        0,
        null,
        false,
        false,
        author,
        author,
        null,
        NOW,
        null,
        List.of(),
        List.of(),
        stored,
        List.of(),
        cases,
        null);
  }

  private AuthoringTestcase testcase(int position, String visibility, boolean approved) {
    return new AuthoringTestcase(
        UUID.randomUUID(),
        position,
        visibility,
        "MANUAL",
        approved,
        null,
        "INLINE",
        "in" + position,
        null,
        "INLINE",
        "out" + position,
        null);
  }

  private Command command(UUID testcaseId, String language, String source) {
    return new Command(new AuthoringScope(actor, false), problemId, testcaseId, language, source);
  }

  private String code(Command command) {
    return ((AlgoPrepException)
            org.junit.jupiter.api.Assertions.assertThrows(
                AlgoPrepException.class, () -> handler.run(command)))
        .errorCode();
  }

  private void runnerAnswersEveryCase(boolean passed, String verdict) {
    when(runner.run(any()))
        .thenAnswer(
            call ->
                new ReferenceSolutionRun.Result(
                    false,
                    ((ReferenceSolutionRun.Request) call.getArgument(0))
                        .cases().stream()
                            .map(
                                c ->
                                    new ReferenceSolutionRun.CaseResult(
                                        c.testcaseId(), passed, verdict, 12L, 3400L))
                            .toList()));
  }

  @Test
  void runsEveryApprovedCaseInGradingOrderAndReportsVerdictsOnly() {
    var second = testcase(2, "HIDDEN", true);
    var first = testcase(1, "SAMPLE", true);
    cases.addAll(List.of(second, first, testcase(3, "HIDDEN", false)));
    runnerAnswersEveryCase(true, "AC");

    var report = handler.run(command(null, null, null));

    assertThat(report.compileError()).isNull();
    assertThat(report.summary().total()).isEqualTo(2);
    assertThat(report.summary().passed()).isEqualTo(2);
    assertThat(report.summary().allPass()).isTrue();
    assertThat(report.results())
        .extracting(r -> r.testcaseId())
        .containsExactly(first.id(), second.id());
    assertThat(report.results().get(0).verdict()).isEqualTo("PASS");
    assertThat(report.results().get(0).failure()).isNull();
    assertThat(report.results().get(0).runtimeMs()).isEqualTo(12);
    assertThat(report.results().get(0).memoryKb()).isEqualTo(3400);
    assertThat(report.ranAt()).isEqualTo(NOW);
    var request = ArgumentCaptor.forClass(ReferenceSolutionRun.Request.class);
    verify(runner).run(request.capture());
    assertThat(request.getValue().language()).isEqualTo("PYTHON");
    assertThat(request.getValue().sourceCode()).isEqualTo("print(1)");
    assertThat(request.getValue().cases()).hasSize(2);
    verify(quota, never()).refund(any(), any());
  }

  @Test
  void aNamedCaseRunsEvenWhenItIsAnUnapprovedDraftAndAnOverrideReplacesTheStoredCode() {
    var draft = testcase(5, "HIDDEN", false);
    cases.addAll(List.of(testcase(1, "SAMPLE", true), draft));
    stored = null;
    runnerAnswersEveryCase(true, "AC");

    var report = handler.run(command(draft.id(), "CPP", "int main(){}"));

    assertThat(report.results()).hasSize(1);
    assertThat(report.results().get(0).testcaseId()).isEqualTo(draft.id());
    var request = ArgumentCaptor.forClass(ReferenceSolutionRun.Request.class);
    verify(runner).run(request.capture());
    assertThat(request.getValue().language()).isEqualTo("CPP");
  }

  @Test
  void aFailingCaseNamesItsReasonAndNoOutputIsReported() {
    cases.add(testcase(1, "HIDDEN", true));
    runnerAnswersEveryCase(false, "WA");

    var report = handler.run(command(null, null, null));

    assertThat(report.summary().allPass()).isFalse();
    assertThat(report.results().get(0).failure()).isEqualTo("WRONG_ANSWER");
    assertThat(report.results().get(0).verdict()).isEqualTo("FAIL");
    assertThat(RunReferenceSolutionQueryHandler.failure("TLE")).isEqualTo("TIME_LIMIT_EXCEEDED");
    assertThat(RunReferenceSolutionQueryHandler.failure("MLE")).isEqualTo("MEMORY_LIMIT_EXCEEDED");
    assertThat(RunReferenceSolutionQueryHandler.failure("OLE")).isEqualTo("OUTPUT_LIMIT_EXCEEDED");
    assertThat(RunReferenceSolutionQueryHandler.failure("RE")).isEqualTo("RUNTIME_ERROR");
    assertThat(RunReferenceSolutionQueryHandler.failure("COMPILE_ERROR"))
        .isEqualTo("COMPILE_ERROR");
    assertThat(RunReferenceSolutionQueryHandler.failure("???")).isEqualTo("RUNTIME_ERROR");
  }

  @Test
  void aCompileErrorMarksEveryRowAndCarriesTheMessage() {
    cases.addAll(List.of(testcase(1, "SAMPLE", true), testcase(2, "HIDDEN", true)));
    when(runner.run(any()))
        .thenReturn(new ReferenceSolutionRun.Result(true, List.of(), "line 3: error"));

    var report = handler.run(command(null, null, null));

    assertThat(report.compileError()).isEqualTo("line 3: error");
    assertThat(report.results())
        .allSatisfy(r -> assertThat(r.failure()).isEqualTo("COMPILE_ERROR"));
    assertThat(report.summary().passed()).isZero();
    assertThat(report.summary().total()).isEqualTo(2);
    when(runner.run(any())).thenReturn(new ReferenceSolutionRun.Result(true, List.of(), " "));
    assertThat(handler.run(command(null, null, null)).compileError()).isNotBlank();
  }

  @Test
  void noApprovedCaseMeansAnEmptyReportWithoutCallingTheSandboxOrSpendingQuota() {
    cases.add(testcase(1, "HIDDEN", false));

    var report = handler.run(command(null, null, null));

    assertThat(report.summary().total()).isZero();
    assertThat(report.summary().allPass()).isFalse();
    verify(runner, never()).run(any());
    verify(quota, never()).reserve(any(), any());
  }

  @Test
  void aMissingSampleSolutionWithoutOverrideIsPrb040() {
    cases.add(testcase(1, "HIDDEN", true));
    stored = null;

    assertThat(code(command(null, null, null))).isEqualTo("PRB-040");
    verify(runner, never()).run(any());
  }

  @Test
  void badOverridesAreRefusedWith400() {
    cases.add(testcase(1, "HIDDEN", true));
    var tooBig = "x".repeat(RunReferenceSolutionQueryHandler.MAX_SOURCE_BYTES + 1);

    assertThat(code(command(null, "PYTHON", null))).isEqualTo("PRB-001");
    assertThat(code(command(null, null, "code"))).isEqualTo("PRB-001");
    assertThat(code(command(null, "RUBY", "code"))).isEqualTo("PRB-001");
    assertThat(code(command(null, "PYTHON", "  "))).isEqualTo("PRB-001");
    assertThat(code(command(null, "PYTHON", tooBig))).isEqualTo("PRB-001");
    verify(runner, never()).run(any());
  }

  @Test
  void aCaseOfAnotherProblemIs404Prb011() {
    cases.add(testcase(1, "HIDDEN", true));

    assertThat(code(command(UUID.randomUUID(), null, null))).isEqualTo("PRB-011");
  }

  @Test
  void authorshipDecidesWhoMayRunAndAnAdminMay() {
    cases.add(testcase(1, "HIDDEN", true));
    runnerAnswersEveryCase(true, "AC");
    author = UUID.randomUUID();

    assertThat(code(command(null, null, null))).isEqualTo("PRB-010");
    status = "PUBLISHED";
    assertThat(code(command(null, null, null))).isEqualTo("PRB-003");
    var admin =
        handler.run(
            new Command(new AuthoringScope(UUID.randomUUID(), true), problemId, null, null, null));
    assertThat(admin.summary().allPass()).isTrue();
    when(repository.find(any(), eq(true))).thenReturn(Optional.empty());
    assertThat(code(command(null, null, null))).isEqualTo("PRB-010");
  }

  @Test
  void theEleventhRunIsRefusedWithTheRetryTimeAndNothingRuns() {
    cases.add(testcase(1, "HIDDEN", true));
    when(quota.reserve(any(), any())).thenReturn(17L);

    var thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            AlgoPrepException.class, () -> handler.run(command(null, null, null)));

    assertThat(thrown.errorCode()).isEqualTo("PRB-055");
    assertThat(((ClientErrorDetails) thrown).clientErrorDetails())
        .containsEntry("retryAfterSeconds", 17L);
    verify(runner, never()).run(any());
    verify(quota, never()).refund(any(), any());
  }

  @Test
  void aSandboxFailureIs503Prb050AndGivesTheSlotBack() {
    cases.add(testcase(1, "HIDDEN", true));
    when(runner.run(any())).thenThrow(new IllegalStateException("down"));

    assertThat(code(command(null, null, null))).isEqualTo("PRB-050");
    verify(quota).refund(eq(actor), any());
  }

  @Test
  void aBrokenAnswerShapeCountsAsASandboxFailure() {
    cases.addAll(List.of(testcase(1, "HIDDEN", true), testcase(2, "HIDDEN", true)));
    when(runner.run(any()))
        .thenReturn(
            new ReferenceSolutionRun.Result(
                false,
                List.of(new ReferenceSolutionRun.CaseResult(UUID.randomUUID(), true, "AC"))));

    assertThat(code(command(null, null, null))).isEqualTo("PRB-050");
    verify(quota).refund(eq(actor), any());
  }

  @Test
  void aStorageFailureWhileReadingABodyKeepsItsOwnCodeAndGivesTheSlotBack() {
    var big =
        new AuthoringTestcase(
            UUID.randomUUID(),
            1,
            "HIDDEN",
            "MANUAL",
            true,
            null,
            "MINIO",
            null,
            "problems/p/k",
            "INLINE",
            "o",
            null);
    cases.add(big);
    doThrow(new TestcaseStorageUnavailableException()).when(store).get("problems/p/k");

    assertThat(code(command(null, null, null))).isEqualTo("PRB-052");
    verify(quota).refund(eq(actor), any());
    verify(runner, never()).run(any());
  }

  @Test
  void aLargeSideIsReadFromTheStoreAsUtf8() {
    cases.add(
        new AuthoringTestcase(
            UUID.randomUUID(),
            1,
            "HIDDEN",
            "MANUAL",
            true,
            null,
            "MINIO",
            null,
            "problems/p/k",
            "INLINE",
            "o",
            null));
    when(store.get("problems/p/k")).thenReturn("tiếng".getBytes(StandardCharsets.UTF_8));
    runnerAnswersEveryCase(true, "AC");

    handler.run(command(null, null, null));

    var request = ArgumentCaptor.forClass(ReferenceSolutionRun.Request.class);
    verify(runner).run(request.capture());
    assertThat(request.getValue().cases().get(0).input()).isEqualTo("tiếng");
  }

  @Test
  void aTextualCommandNeverPrintsTheSource() {
    assertThat(command(null, "PYTHON", "secret").toString()).doesNotContain("secret");
  }
}
