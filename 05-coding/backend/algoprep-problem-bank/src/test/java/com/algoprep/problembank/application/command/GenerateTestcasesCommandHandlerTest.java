package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.problembank.application.command.GeneratedTestcaseWriter.Kept;
import com.algoprep.problembank.application.command.GeneratedTestcaseWriter.Persisted;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.JudgeGradingDefaults;
import com.algoprep.problembank.application.dto.ReferenceRunReport;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.dto.TestcaseGeneration;
import com.algoprep.problembank.application.dto.TestcaseGeneration.GeneratorRun;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ReferenceOutput;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ReferenceStatus;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ScriptStatus;
import com.algoprep.problembank.application.ports.in.RunReferenceSolutionUseCase;
import com.algoprep.problembank.application.ports.out.GeneratorSandboxPort;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.application.ports.out.TestcaseGenerationLockPort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.ports.out.TestcaseScriptGenerationPort;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemTouch;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GenerateTestcasesCommandHandlerTest {
  private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");

  private final ProblemAuthoringReadRepository repository =
      mock(ProblemAuthoringReadRepository.class);
  private final RunReferenceSolutionUseCase reference = mock(RunReferenceSolutionUseCase.class);
  private final TestcaseObjectStorePort store = mock(TestcaseObjectStorePort.class);
  private final JudgeConfigReadPort config = mock(JudgeConfigReadPort.class);
  private final TestcaseScriptGenerationPort ai = mock(TestcaseScriptGenerationPort.class);
  private final GeneratorSandboxPort sandbox = mock(GeneratorSandboxPort.class);
  private final TestcaseGenerationLockPort lock = mock(TestcaseGenerationLockPort.class);
  private final GeneratedTestcaseWriter writer = mock(GeneratedTestcaseWriter.class);
  private final UUID actor = UUID.randomUUID();
  private final UUID problemId = UUID.randomUUID();
  private final List<AuthoringTestcase> cases = new ArrayList<>();
  private ProblemContentData.Solution solution =
      new ProblemContentData.Solution("PYTHON", "print(1)");
  private String constraints = "1 <= n <= 10";
  private UUID author = actor;
  private String status = "UNPUBLISHED";
  private int used = 0;
  private GenerateTestcasesCommandHandler handler;

  @BeforeEach
  void setup() {
    cases.add(testcase(1, "SAMPLE", "MANUAL", true, "in1"));
    cases.add(testcase(2, "SAMPLE", "MANUAL", true, "in2"));
    cases.add(testcase(3, "HIDDEN", "MANUAL", true, "secret-hidden"));
    when(config.getGradingDefaults()).thenReturn(new JudgeGradingDefaults(1000, 256, 64, 10));
    when(repository.find(eq(problemId), eq(true))).thenAnswer(c -> Optional.of(problem()));
    when(repository.aiGenerationLimit()).thenReturn(2);
    when(lock.acquire(problemId)).thenReturn("token");
    when(reference.run(any()))
        .thenReturn(
            new ReferenceRunReport.Report(
                null, new ReferenceRunReport.Summary(3, 3, true), List.of(), NOW));
    when(ai.generate(any()))
        .thenReturn(
            new TestcaseGeneration.Scripts(
                "gen", "val", List.of("TYPICAL"), "model-y", "prompt-1"));
    handler =
        new GenerateTestcasesCommandHandler(
            repository, reference, store, new GenerationPorts(ai, sandbox, lock, config), writer);
  }

  private AuthoringProblemRecord problem() {
    return new AuthoringProblemRecord(
        problemId,
        "C",
        "T",
        "statement",
        constraints,
        UUID.randomUUID(),
        status,
        false,
        new ProblemContentData.Limits(1000, 256, 64, null),
        1,
        used,
        "secret brief",
        false,
        false,
        author,
        author,
        null,
        NOW,
        null,
        List.of(),
        List.of(),
        solution,
        List.of(),
        cases,
        null);
  }

  private static AuthoringTestcase testcase(
      int position, String visibility, String origin, boolean approved, String in) {
    return new AuthoringTestcase(
        UUID.randomUUID(),
        position,
        visibility,
        origin,
        approved,
        null,
        "INLINE",
        in,
        null,
        "INLINE",
        "out" + position,
        null);
  }

  private TestcaseGeneration.Command command() {
    return new TestcaseGeneration.Command(new AuthoringScope(actor, false), problemId);
  }

  private AlgoPrepException failure(Runnable call) {
    try {
      call.run();
    } catch (AlgoPrepException e) {
      return e;
    }
    throw new AssertionError("no exception");
  }

  private AlgoPrepException rejected() {
    return failure(() -> handler.generate(command()));
  }

  private void generatorPrints(String stdout) {
    when(sandbox.runGenerator(anyString(), anyLong()))
        .thenReturn(new GeneratorRun(ScriptStatus.OK, stdout));
  }

  private void validatorSays(Boolean... flags) {
    when(sandbox.validate(anyString(), anyList())).thenReturn(List.of(flags));
  }

  private void referenceReturns(ReferenceOutput... outputs) {
    when(sandbox.runReference(any())).thenReturn(List.of(outputs));
  }

  private void persistReturns() {
    when(writer.persist(any(), eq(problemId), anyList(), any()))
        .thenReturn(new Persisted(List.of(), new ProblemTouch(NOW.plusSeconds(5), 1)));
  }

  @Test
  void theHappyPathKeepsOnlyTheRealOutputsAndCountsTheTurn() {
    generatorPrints("###CASE TYPICAL\n1\n###CASE EXTREME\n22\n###CASE DEGENERATE\n333\n");
    validatorSays(true, true, false);
    referenceReturns(
        new ReferenceOutput(ReferenceStatus.OK, "10"),
        new ReferenceOutput(ReferenceStatus.TIMEOUT, null));
    persistReturns();

    var result = handler.generate(command());

    assertThat(result.added()).isEqualTo(1);
    assertThat(result.aiGeneration()).isEqualTo(new TestcaseGeneration.Quota(1, 2));
    assertThat(result.problemUpdatedAt()).isEqualTo(NOW.plusSeconds(5));
    assertThat(result.dropped())
        .containsExactly(
            new TestcaseGeneration.Dropped("INVALID_CONSTRAINT", 1),
            new TestcaseGeneration.Dropped("TIMEOUT", 1));
    assertThat(result.warnings()).extracting("code").containsExactly("LARGEST_CASE_TIMEOUT");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Kept>> kept = ArgumentCaptor.forClass(List.class);
    verify(writer).persist(any(), eq(problemId), kept.capture(), any());
    assertThat(kept.getValue()).containsExactly(new Kept("TYPICAL", "1\n", "10\n"));
    verify(lock).release(problemId, "token");
  }

  @Test
  void theAiSeesTheStatementTheConstraintsAndTwoSamplesAndNothingElse() {
    generatorPrints("1\n");
    validatorSays(true);
    referenceReturns(new ReferenceOutput(ReferenceStatus.OK, "1"));
    persistReturns();

    handler.generate(command());

    var request = ArgumentCaptor.forClass(TestcaseGeneration.ScriptRequest.class);
    verify(ai).generate(request.capture());
    var sent = request.getValue();
    assertThat(sent.statementMarkdown()).isEqualTo("statement");
    assertThat(sent.constraintsMarkdown()).isEqualTo(constraints);
    assertThat(sent.userId()).isEqualTo(actor);
    assertThat(sent.samples())
        .containsExactly(
            new TestcaseGeneration.Sample("in1", "out1"),
            new TestcaseGeneration.Sample("in2", "out2"));
    assertThat(sent.toString() + sent.samples())
        .doesNotContain("print(1)", "secret brief", "secret-hidden");
  }

  @Test
  void aMinioSampleIsReadFromTheObjectStore() {
    cases.set(
        0,
        new AuthoringTestcase(
            cases.get(0).id(),
            1,
            "SAMPLE",
            "MANUAL",
            true,
            null,
            "MINIO",
            null,
            "k1",
            "INLINE",
            "out1",
            null));
    when(store.get("k1")).thenReturn("big-input".getBytes(StandardCharsets.UTF_8));
    generatorPrints("1\n");
    validatorSays(true);
    referenceReturns(new ReferenceOutput(ReferenceStatus.OK, "1"));
    persistReturns();

    handler.generate(command());

    var request = ArgumentCaptor.forClass(TestcaseGeneration.ScriptRequest.class);
    verify(ai).generate(request.capture());
    assertThat(request.getValue().samples().get(0).input()).isEqualTo("big-input");
  }

  @Test
  void aMissingSampleSolutionIsPrb040AndNothingElseRuns() {
    solution = null;

    assertThat(rejected().errorCode()).isEqualTo("PRB-040");
    verifyNoInteractions(ai, sandbox, lock);
  }

  @Test
  void everyMissingPrerequisiteIsListedAndTheAiIsNotCalled() {
    constraints = "  ";
    cases.remove(1);
    cases.set(0, testcase(1, "SAMPLE", "AI", true, "in1"));

    var failure = rejected();

    assertThat(failure.errorCode()).isEqualTo("PRB-041");
    assertThat(((ClientErrorDetails) failure).clientErrorDetails().get("reasons"))
        .isEqualTo(List.of("NO_CONSTRAINTS", "NOT_ENOUGH_MANUAL_SAMPLES"));
    verifyNoInteractions(ai, sandbox, lock);
  }

  @Test
  void aSampleThatIsNotApprovedDoesNotCount() {
    cases.set(1, testcase(2, "SAMPLE", "MANUAL", false, "in2"));

    var failure = rejected();

    assertThat(((ClientErrorDetails) failure).clientErrorDetails().get("reasons"))
        .isEqualTo(List.of("NOT_ENOUGH_MANUAL_SAMPLES"));
  }

  @Test
  void aSampleSolutionThatDoesNotPassIsPrb041BeforeTheAiIsCalledAndTheLockIsReleased() {
    when(reference.run(any()))
        .thenReturn(
            new ReferenceRunReport.Report(
                null, new ReferenceRunReport.Summary(2, 3, false), List.of(), NOW));

    var failure = rejected();

    assertThat(failure.errorCode()).isEqualTo("PRB-041");
    assertThat(((ClientErrorDetails) failure).clientErrorDetails().get("reasons"))
        .isEqualTo(List.of("REFERENCE_NOT_PASSING"));
    verifyNoInteractions(ai);
    verify(lock).release(problemId, "token");
  }

  @Test
  void theCapRefusesWithTheDetailsBeforeTheLockOrTheAi() {
    used = 2;

    var failure = rejected();

    assertThat(failure.errorCode()).isEqualTo("PRB-042");
    assertThat(((ClientErrorDetails) failure).clientErrorDetails())
        .isEqualTo(Map.of("used", 2, "limit", 2));
    verifyNoInteractions(ai, sandbox, lock);
  }

  @Test
  void oneBelowTheCapStillRuns() {
    used = 1;
    generatorPrints("1\n");
    validatorSays(true);
    referenceReturns(new ReferenceOutput(ReferenceStatus.OK, "1"));
    persistReturns();

    assertThat(handler.generate(command()).aiGeneration())
        .isEqualTo(new TestcaseGeneration.Quota(2, 2));
  }

  @Test
  void aSecondRunOnTheSameProblemIs409AndNeverReachesTheAi() {
    when(lock.acquire(problemId)).thenReturn(null);

    assertThat(rejected().errorCode()).isEqualTo("PRB-023");
    verifyNoInteractions(ai);
    verify(lock, never()).release(any(), any());
  }

  @Test
  void anAiFailureIsPrb051AndWritesNothing() {
    when(ai.generate(any())).thenThrow(new IllegalStateException("AIR-501"));

    assertThat(rejected().errorCode()).isEqualTo("PRB-051");
    verify(writer, never()).persist(any(), any(), anyList(), any());
    verify(lock).release(problemId, "token");
  }

  @Test
  void everyBadScriptStatusIsPrb043WithItsReasonAndWritesNothing() {
    for (ScriptStatus bad :
        List.of(ScriptStatus.TIMEOUT, ScriptStatus.OUTPUT_TOO_LARGE, ScriptStatus.SCRIPT_ERROR)) {
      when(sandbox.runGenerator(anyString(), anyLong())).thenReturn(new GeneratorRun(bad, null));

      var failure = rejected();

      assertThat(failure.errorCode()).isEqualTo("PRB-043");
      assertThat(((ClientErrorDetails) failure).clientErrorDetails().get("reason"))
          .isEqualTo(bad.name());
    }
    verify(writer, never()).persist(any(), any(), anyList(), any());
  }

  @Test
  void aSandboxFaultIsPrb050() {
    when(sandbox.runGenerator(anyString(), anyLong())).thenThrow(new IllegalStateException("down"));

    assertThat(rejected().errorCode()).isEqualTo("PRB-050");
    verify(writer, never()).persist(any(), any(), anyList(), any());
    verify(lock).release(problemId, "token");
  }

  @Test
  void aSandboxThatAnswersTheWrongNumberOfCasesIsPrb050() {
    generatorPrints("1\n");
    validatorSays();

    assertThat(rejected().errorCode()).isEqualTo("PRB-050");
  }

  @Test
  void duplicatesOfExistingOrEarlierInputsAreDropped() {
    generatorPrints("###CASE TYPICAL\nin1\n###CASE TYPICAL\nnew\n###CASE TYPICAL\nnew\n");
    validatorSays(true);
    referenceReturns(new ReferenceOutput(ReferenceStatus.OK, "5"));
    persistReturns();

    var result = handler.generate(command());

    assertThat(result.added()).isEqualTo(1);
    assertThat(result.dropped())
        .containsExactly(new TestcaseGeneration.Dropped("DUPLICATE_INPUT", 2));
  }

  @Test
  void aRunThatKeepsNothingAddsNoTestcaseAndDoesNotUseUpATurn() {
    generatorPrints("###CASE TYPICAL\n1\n###CASE TYPICAL\n22\n");
    validatorSays(true, true);
    referenceReturns(
        new ReferenceOutput(ReferenceStatus.FAILED, null),
        new ReferenceOutput(ReferenceStatus.OK, "  \n"));

    var result = handler.generate(command());

    assertThat(result.added()).isZero();
    assertThat(result.aiGeneration()).isEqualTo(new TestcaseGeneration.Quota(0, 2));
    assertThat(result.dropped())
        .containsExactly(new TestcaseGeneration.Dropped("REFERENCE_FAILED", 2));
    verify(writer, never()).persist(any(), any(), anyList(), any());
  }

  @Test
  void anOutputOverTheProblemLimitIsDropped() {
    generatorPrints("1\n");
    validatorSays(true);
    referenceReturns(new ReferenceOutput(ReferenceStatus.OK, "x".repeat(65 * 1024)));

    assertThat(handler.generate(command()).added()).isZero();
  }

  @Test
  void everyInputInvalidSkipsTheSampleSolutionRun() {
    generatorPrints("1\n");
    validatorSays(false);

    var result = handler.generate(command());

    assertThat(result.added()).isZero();
    verify(sandbox, never()).runReference(any());
  }

  @Test
  void theReferenceRunReceivesInputsOnlyWithTheProblemLimits() {
    generatorPrints("5\n");
    validatorSays(true);
    referenceReturns(new ReferenceOutput(ReferenceStatus.OK, "5"));
    persistReturns();

    handler.generate(command());

    var request = ArgumentCaptor.forClass(ReferenceSolutionRun.Request.class);
    verify(sandbox).runReference(request.capture());
    assertThat(request.getValue().cases()).hasSize(1);
    assertThat(request.getValue().limits().timeLimitMs()).isEqualTo(1000);
    assertThat(request.getValue().language()).isEqualTo("PYTHON");
  }

  @Test
  void theSeedIsStablePerAttemptAndDiffersBetweenAttempts() {
    assertThat(GenerateTestcasesCommandHandler.seed(problemId, 1))
        .isEqualTo(GenerateTestcasesCommandHandler.seed(problemId, 1))
        .isNotEqualTo(GenerateTestcasesCommandHandler.seed(problemId, 2));
  }

  @Test
  void anotherAuthorsDraftIs404AndTheirPublishedProblemIs403ButAnAdminMayRun() {
    author = UUID.randomUUID();
    assertThat(rejected().errorCode()).isEqualTo("PRB-010");
    status = "PUBLISHED";
    assertThat(rejected().errorCode()).isEqualTo("PRB-003");
    verifyNoInteractions(ai, sandbox, lock);

    generatorPrints("1\n");
    validatorSays(true);
    referenceReturns(new ReferenceOutput(ReferenceStatus.OK, "1"));
    persistReturns();
    var admin = new TestcaseGeneration.Command(new AuthoringScope(actor, true), problemId);
    assertThat(handler.generate(admin).added()).isEqualTo(1);
  }

  @Test
  void aMissingProblemIs404() {
    when(repository.find(eq(problemId), eq(true))).thenReturn(Optional.empty());

    assertThat(rejected().errorCode()).isEqualTo("PRB-010");
  }
}
