package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.problembank.application.dto.JudgeGradingDefaults;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.dto.SnapshotPublicationResult;
import com.algoprep.problembank.application.dto.UpdateProblemStatusCommand;
import com.algoprep.problembank.application.policy.PublishChecklistPolicy;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.model.ProblemSpecData;
import com.algoprep.problembank.domain.model.ProblemStatusWrite;
import com.algoprep.problembank.domain.model.StoredGradingTestcase;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.model.TestcaseDescriptor;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import com.algoprep.problembank.domain.ports.out.ProblemPublishRepository;
import com.algoprep.problembank.domain.ports.out.SnapshotPublicationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

class UpdateProblemPublishStatusCommandHandlerTest {
  private final ProblemContentRepository content = mock(ProblemContentRepository.class);
  private final ProblemAuthoringReadRepository reads = mock(ProblemAuthoringReadRepository.class);
  private final SnapshotPublicationRepository approved = mock(SnapshotPublicationRepository.class);
  private final ProblemPublishRepository writes = mock(ProblemPublishRepository.class);
  private final PublishTestcaseSnapshotUseCase snapshots =
      mock(PublishTestcaseSnapshotUseCase.class);
  private final SampleSolutionRunnerPort runner = mock(SampleSolutionRunnerPort.class);
  private final TestcaseObjectStorePort store = mock(TestcaseObjectStorePort.class);
  private final JudgeConfigReadPort config = mock(JudgeConfigReadPort.class);
  private final ProblemAuditPort audit = mock(ProblemAuditPort.class);
  private final ProblemListVersionPort cache = mock(ProblemListVersionPort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final UpdateProblemPublishStatusCommandHandler handler =
      new UpdateProblemPublishStatusCommandHandler(
          content,
          reads,
          snapshots,
          new PublishGate(
              new PublishChecklistPolicy(new MockEnvironment()), approved, store, config, runner),
          new StatusChangeRecorder(writes, audit, cache, ids));

  private final UUID problem = UUID.randomUUID();
  private final UUID author = UUID.randomUUID();
  private final UUID other = UUID.randomUUID();
  private final Instant stamp = Instant.parse("2026-11-05T01:00:00Z");
  private final Instant after = Instant.parse("2026-11-05T02:00:00Z");

  private UpdateProblemStatusCommand command(
      UUID actor, boolean admin, String status, Instant expected) {
    return new UpdateProblemStatusCommand(problem, actor, admin, status, expected);
  }

  private void locked(String status, UUID authorId) {
    when(content.lock(problem))
        .thenReturn(
            Optional.of(
                new ProblemContentSnapshot(problem, "CODE", authorId, status, null, stamp)));
  }

  @Test
  void anUnknownStatusIsRejectedBeforeAnyRead() {
    assertThatThrownBy(() -> handler.update(command(author, false, "ARCHIVED", stamp)))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-001"));
    verifyNoInteractions(content);
  }

  @Test
  void aMissingProblemAndSomeoneElsesDraftAreNotFound() {
    when(content.lock(problem)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.update(command(author, false, "PUBLISHED", stamp)))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-010"));

    locked("UNPUBLISHED", author);
    assertThatThrownBy(() -> handler.update(command(other, false, "PUBLISHED", stamp)))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-010"));
  }

  @Test
  void someoneElsesPublishedProblemIsForbiddenButAnAdminMayAct() {
    locked("PUBLISHED", author);
    assertThatThrownBy(() -> handler.update(command(other, false, "UNPUBLISHED", stamp)))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-003"));
    verify(writes, never()).setStatus(any(), any(), anyString());

    when(reads.find(problem, false)).thenReturn(Optional.of(record("PUBLISHED", 8, 2, 2, true)));
    when(writes.setStatus(problem, other, "UNPUBLISHED"))
        .thenReturn(new ProblemStatusWrite(after, stamp, 1));
    when(ids.newId()).thenReturn(UUID.randomUUID());
    assertThat(handler.update(command(other, true, "UNPUBLISHED", stamp)).status())
        .isEqualTo("UNPUBLISHED");
  }

  @Test
  void aMissingOrStaleTimestampIsRejected() {
    locked("UNPUBLISHED", author);
    assertThatThrownBy(() -> handler.update(command(author, false, "PUBLISHED", null)))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-001"));
    assertThatThrownBy(
            () -> handler.update(command(author, false, "PUBLISHED", stamp.minusSeconds(1))))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-020"));
  }

  @Test
  void aProblemAlreadyInTheTargetStatusChangesNothing() {
    locked("PUBLISHED", author);
    when(reads.find(problem, false)).thenReturn(Optional.of(record("PUBLISHED", 8, 2, 2, true)));

    var result = handler.update(command(author, false, "PUBLISHED", stamp));

    assertThat(result.status()).isEqualTo("PUBLISHED");
    assertThat(result.problemUpdatedAt()).isEqualTo(stamp);
    verifyNoInteractions(writes, runner, snapshots, audit, cache);
  }

  @Test
  void withdrawingNeedsNoChecklistAndWritesAuditEventAndCache() {
    locked("PUBLISHED", author);
    when(reads.find(problem, false)).thenReturn(Optional.of(record("PUBLISHED", 0, 0, 0, false)));
    when(writes.setStatus(problem, author, "UNPUBLISHED"))
        .thenReturn(new ProblemStatusWrite(after, stamp, 3));
    when(ids.newId()).thenReturn(UUID.randomUUID());

    var result = handler.update(command(author, false, "UNPUBLISHED", stamp));

    assertThat(result.status()).isEqualTo("UNPUBLISHED");
    assertThat(result.currentTestcaseSetVersion()).isEqualTo(3);
    verifyNoInteractions(runner, snapshots);
    var entry = ArgumentCaptor.forClass(CatalogAuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("PROBLEM_UNPUBLISHED");
    verify(writes)
        .appendEvent(
            eq(problem), eq("ProblemUnpublished"), eq("problem.problem.unpublished"), anyString());
    verify(cache).incrementAfterCommit();
  }

  @Test
  void publishingRunsTheSolutionOnEveryApprovedCaseThenSnapshotsThenFlipsTheStatus() {
    locked("UNPUBLISHED", author);
    var rec = record("UNPUBLISHED", 8, 2, 2, true);
    when(reads.find(problem, false)).thenReturn(Optional.of(rec));
    var inline = inline(UUID.randomUUID(), "SAMPLE", 1, "1 2", "3");
    var big = minio(UUID.randomUUID(), 2, "k/in", "9");
    when(approved.findApproved(problem)).thenReturn(List.of(inline, big));
    when(store.get("k/in"))
        .thenReturn("big input".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    when(config.getGradingDefaults()).thenReturn(new JudgeGradingDefaults(1000, 256, 64, 10));
    when(runner.run(any()))
        .thenAnswer(
            call -> {
              var request = (ReferenceSolutionRun.Request) call.getArgument(0);
              return new ReferenceSolutionRun.Result(
                  false,
                  request.cases().stream()
                      .map(c -> new ReferenceSolutionRun.CaseResult(c.testcaseId(), true, "AC"))
                      .toList());
            });
    when(snapshots.publishSnapshot(problem))
        .thenReturn(new SnapshotPublicationResult(problem, 1, 2, true, false));
    when(writes.setStatus(problem, author, "PUBLISHED"))
        .thenReturn(new ProblemStatusWrite(after, after, 1));
    when(ids.newId()).thenReturn(UUID.randomUUID());

    var result = handler.update(command(author, false, "PUBLISHED", stamp));

    assertThat(result.status()).isEqualTo("PUBLISHED");
    assertThat(result.publishedAt()).isEqualTo(after);
    assertThat(result.checklist().sampleSolutionAllPass()).isTrue();
    var sent = ArgumentCaptor.forClass(ReferenceSolutionRun.Request.class);
    verify(runner).run(sent.capture());
    var request = sent.getValue();
    assertThat(request.language()).isEqualTo("PYTHON");
    assertThat(request.functionWrapper()).isTrue();
    assertThat(request.cases()).hasSize(2);
    assertThat(request.cases().get(0).input()).isEqualTo("1 2");
    assertThat(request.cases().get(1).input()).isEqualTo("big input");
    assertThat(request.limits().compileTimeoutSeconds()).isEqualTo(10);
    var order = inOrder(snapshots, writes, audit);
    order.verify(snapshots).publishSnapshot(problem);
    order.verify(writes).setStatus(problem, author, "PUBLISHED");
    order.verify(audit).record(any(CatalogAuditEntry.class));
    verify(writes)
        .appendEvent(
            eq(problem), eq("ProblemPublished"), eq("problem.problem.published"), anyString());
    verify(cache).incrementAfterCommit();
  }

  @Test
  void aShortChecklistListsEveryMissingConditionAndChangesNothing() {
    locked("UNPUBLISHED", author);
    // 7 approved cases (needs 8), 1 sample (needs 2), 1 example (needs 2), no spec.
    when(reads.find(problem, false)).thenReturn(Optional.of(record("UNPUBLISHED", 7, 1, 1, false)));
    when(approved.findApproved(problem))
        .thenReturn(List.of(inline(UUID.randomUUID(), "SAMPLE", 1, "1", "1")));
    when(config.getGradingDefaults()).thenReturn(new JudgeGradingDefaults(1000, 256, 64, 10));
    when(runner.run(any()))
        .thenReturn(
            new ReferenceSolutionRun.Result(
                false,
                List.of(new ReferenceSolutionRun.CaseResult(UUID.randomUUID(), true, "AC"))));

    var thrown = catchThrown(() -> handler.update(command(author, false, "PUBLISHED", stamp)));

    assertThat(thrown).isInstanceOf(AlgoPrepException.class);
    assertThat(((AlgoPrepException) thrown).errorCode()).isEqualTo("PRB-030");
    @SuppressWarnings("unchecked")
    var items =
        (List<Map<String, Object>>) ((ClientErrorDetails) thrown).clientErrorDetails().get("items");
    assertThat(items)
        .extracting(i -> i.get("code"))
        .containsExactly(
            "MIN_APPROVED_TESTCASES",
            "MIN_APPROVED_SAMPLES",
            "MIN_WORKED_EXAMPLES",
            "SPEC_DECLARED");
    verifyNoInteractions(snapshots, audit, cache);
    verify(writes, never()).setStatus(any(), any(), anyString());
  }

  @Test
  void aFailingSolutionBlocksPublishingAndNoRunnableInputSkipsTheSandbox() {
    locked("UNPUBLISHED", author);
    when(reads.find(problem, false)).thenReturn(Optional.of(record("UNPUBLISHED", 8, 2, 2, true)));
    when(approved.findApproved(problem))
        .thenReturn(List.of(inline(UUID.randomUUID(), "SAMPLE", 1, "1", "1")));
    when(config.getGradingDefaults()).thenReturn(new JudgeGradingDefaults(1000, 256, 64, 10));
    when(runner.run(any()))
        .thenReturn(
            new ReferenceSolutionRun.Result(
                false,
                List.of(new ReferenceSolutionRun.CaseResult(UUID.randomUUID(), false, "WA"))));

    var failing = catchThrown(() -> handler.update(command(author, false, "PUBLISHED", stamp)));
    @SuppressWarnings("unchecked")
    var items =
        (List<Map<String, Object>>)
            ((ClientErrorDetails) failing).clientErrorDetails().get("items");
    assertThat(items)
        .extracting(i -> i.get("code"))
        .containsExactly("REFERENCE_SOLUTION_PASSES_ALL");

    // No reference solution at all: the sandbox is never called and the condition is unmet.
    when(reads.find(problem, false)).thenReturn(Optional.of(withoutSolution()));
    org.mockito.Mockito.clearInvocations(runner);
    var none = catchThrown(() -> handler.update(command(author, false, "PUBLISHED", stamp)));
    assertThat(((AlgoPrepException) none).errorCode()).isEqualTo("PRB-030");
    verifyNoInteractions(runner);
  }

  @Test
  void anUnavailableSandboxIs503Prb050AndChangesNothing() {
    locked("UNPUBLISHED", author);
    when(reads.find(problem, false)).thenReturn(Optional.of(record("UNPUBLISHED", 8, 2, 2, true)));
    when(approved.findApproved(problem))
        .thenReturn(List.of(inline(UUID.randomUUID(), "SAMPLE", 1, "1", "1")));
    when(config.getGradingDefaults()).thenReturn(new JudgeGradingDefaults(1000, 256, 64, 10));
    when(runner.run(any())).thenThrow(new IllegalStateException("sandbox down"));

    assertThatThrownBy(() -> handler.update(command(author, false, "PUBLISHED", stamp)))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-050"));
    verifyNoInteractions(snapshots, audit, cache);
    verify(writes, never()).setStatus(any(), any(), anyString());
  }

  // ---- fixtures

  private Throwable catchThrown(Runnable action) {
    try {
      action.run();
    } catch (RuntimeException e) {
      return e;
    }
    throw new AssertionError("expected an exception");
  }

  private StoredGradingTestcase inline(
      UUID id, String visibility, int position, String in, String out) {
    return new StoredGradingTestcase(
        new TestcaseDescriptor(id, visibility, position),
        new StoredGradingTestcase.Content("INLINE", in, null),
        new StoredGradingTestcase.Content("INLINE", out, null));
  }

  private StoredGradingTestcase minio(UUID id, int position, String inputKey, String out) {
    return new StoredGradingTestcase(
        new TestcaseDescriptor(id, "HIDDEN", position),
        new StoredGradingTestcase.Content("MINIO", null, inputKey),
        new StoredGradingTestcase.Content("INLINE", out, null));
  }

  /**
   * {@code approved} cases of which {@code samples} are SAMPLE, {@code examples} worked examples.
   */
  private AuthoringProblemRecord record(
      String status, int approved, int samples, int examples, boolean spec) {
    var cases = new ArrayList<AuthoringTestcase>();
    for (int i = 0; i < approved; i++) {
      cases.add(
          new AuthoringTestcase(
              UUID.randomUUID(),
              i + 1,
              i < samples ? "SAMPLE" : "HIDDEN",
              "MANUAL",
              true,
              null,
              "INLINE",
              null,
              null,
              "INLINE",
              null,
              null));
    }
    var exampleList = new ArrayList<ProblemContentData.Example>();
    for (int i = 0; i < examples; i++) {
      exampleList.add(new ProblemContentData.Example(UUID.randomUUID(), i + 1, "i", "o", null));
    }
    return build(
        status,
        cases,
        exampleList,
        new ProblemContentData.Solution("PYTHON", "def f(): pass"),
        spec
            ? new StoredProblemSpec(
                new ProblemSpecData("EXACT", null, "in", "out", "f", "{}", "[]", null), 2)
            : null);
  }

  private AuthoringProblemRecord withoutSolution() {
    var base = record("UNPUBLISHED", 8, 2, 2, true);
    return build("UNPUBLISHED", base.testcases(), base.workedExamples(), null, base.spec());
  }

  private AuthoringProblemRecord build(
      String status,
      List<AuthoringTestcase> cases,
      List<ProblemContentData.Example> examples,
      ProblemContentData.Solution solution,
      StoredProblemSpec spec) {
    return new AuthoringProblemRecord(
        problem,
        "CODE",
        "Title",
        "statement",
        "",
        UUID.randomUUID(),
        status,
        true,
        new ProblemContentData.Limits(1000, 256, 64, null),
        1,
        0,
        null,
        true,
        true,
        author,
        null,
        null,
        stamp,
        null,
        List.of(),
        List.of(),
        solution,
        examples,
        cases,
        spec);
  }
}
