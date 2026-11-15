package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseView;
import com.algoprep.problembank.application.dto.TestcaseCommands;
import com.algoprep.problembank.application.policy.PublishChecklistPolicy;
import com.algoprep.problembank.application.policy.TestcaseViewMapper;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.model.ApprovalCounts;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.model.ProblemTouch;
import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Manual testcase authoring (DD F2 testcase endpoints): validation codes PRB-001 / 046 / 048 / 049
 * / 054, author scope PRB-003 / 010, snapshot republishing on a published problem, storage cleanup.
 */
class AuthorTestcasesCommandHandlerTest {
  private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
  private static final Instant STAMP = Instant.parse("2026-11-06T10:05:00Z");

  private final ProblemContentRepository content = mock(ProblemContentRepository.class);
  private final TestcaseWriteRepository testcases = mock(TestcaseWriteRepository.class);
  private final PublishTestcaseSnapshotUseCase snapshots =
      mock(PublishTestcaseSnapshotUseCase.class);
  private final TestcaseObjectStorePort store = mock(TestcaseObjectStorePort.class);
  private final AtomicLong counter = new AtomicLong();
  private final IdGeneratorPort ids = () -> new UUID(0, counter.incrementAndGet());
  private final UUID actor = UUID.randomUUID();
  private final UUID problemId = UUID.randomUUID();
  private final UUID testcaseId = UUID.randomUUID();
  private final AuthoringScope scope = new AuthoringScope(actor, false);
  private AuthorTestcasesCommandHandler handler;
  private String status = "UNPUBLISHED";
  private UUID author = actor;

  @BeforeEach
  void setup() {
    TransactionSynchronizationManager.initSynchronization();
    when(content.lock(problemId))
        .thenAnswer(
            call ->
                Optional.of(
                    new ProblemContentSnapshot(
                        problemId, "CODE", author, status, ContentTestData.valid(), NOW)));
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(10, 10, 3));
    when(testcases.nextPosition(problemId)).thenReturn(11);
    when(testcases.touch(problemId, actor)).thenReturn(new ProblemTouch(STAMP, 7));
    var views = mock(TestcaseViewMapper.class);
    when(views.view(any()))
        .thenAnswer(
            c -> {
              var t =
                  (com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase)
                      c.getArgument(0);
              return new TestcaseView(
                  t.id(),
                  t.position(),
                  t.visibility(),
                  t.origin(),
                  t.approved(),
                  t.category(),
                  t.inputInline(),
                  t.inputStorage(),
                  0,
                  null,
                  t.expectedInline(),
                  t.expectedStorage(),
                  0,
                  null);
            });
    var support =
        new TestcaseAuthoringSupport(
            content, testcases, snapshots, mock(ProblemListVersionPort.class), views);
    handler =
        new AuthorTestcasesCommandHandler(
            support,
            testcases,
            new TestcaseContentStore(store, ids),
            new PublishChecklistPolicy(new MockEnvironment()));
  }

  @AfterEach
  void cleanup() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private TestcaseCommands.Create create(String visibility, String in, String out, String cat) {
    return new TestcaseCommands.Create(scope, problemId, visibility, in, out, cat);
  }

  private StoredTestcase stored(boolean approved, String visibility, Content in, Content out) {
    return new StoredTestcase(testcaseId, visibility, "MANUAL", approved, "TYPICAL", 2, in, out);
  }

  private void existing(StoredTestcase row) {
    when(testcases.lock(problemId, testcaseId)).thenReturn(Optional.of(row));
  }

  private TestcaseCommands.Update update(TestcaseCommands.Patch patch) {
    return new TestcaseCommands.Update(scope, problemId, testcaseId, patch);
  }

  private static TestcaseCommands.Patch patch(
      String visibility, String in, String out, Boolean approved) {
    return new TestcaseCommands.Patch(visibility, in, out, false, null, approved);
  }

  private void runCallbacks(int completion) {
    for (TransactionSynchronization sync :
        TransactionSynchronizationManager.getSynchronizations()) {
      sync.afterCompletion(completion);
    }
  }

  // ---- create ----

  @Test
  void createDefaultsToHiddenManualApprovedAtNextPositionWithInlineContent() {
    var saved = handler.create(create(null, "1 2", "3", null));

    var inserted = ArgumentCaptor.forClass(StoredTestcase.class);
    verify(testcases).insert(org.mockito.ArgumentMatchers.eq(problemId), inserted.capture());
    var row = inserted.getValue();
    assertThat(row.visibility()).isEqualTo("HIDDEN");
    assertThat(row.origin()).isEqualTo("MANUAL");
    assertThat(row.approved()).isTrue();
    assertThat(row.position()).isEqualTo(11);
    assertThat(row.input()).isEqualTo(new Content("INLINE", "1 2", null));
    assertThat(saved.currentTestcaseSetVersion()).isEqualTo(7);
    assertThat(saved.problemUpdatedAt()).isEqualTo(STAMP);
    verify(snapshots, never()).publishSnapshot(any());
  }

  @Test
  void createOnPublishedProblemRepublishesTheSnapshot() {
    status = "PUBLISHED";

    handler.create(create("SAMPLE", "1", "2", "BOUNDARY_LOW"));

    verify(snapshots).publishSnapshot(problemId);
  }

  @Test
  void createStoresLargeSidesInObjectStorageAndRemovesThemWhenRolledBack() {
    String large = "x".repeat(9000);

    handler.create(create("HIDDEN", large, "2", null));

    var inserted = ArgumentCaptor.forClass(StoredTestcase.class);
    verify(testcases).insert(org.mockito.ArgumentMatchers.eq(problemId), inserted.capture());
    assertThat(inserted.getValue().input().storage()).isEqualTo("MINIO");
    verify(store).put(anyString(), any());
    runCallbacks(TransactionSynchronization.STATUS_ROLLED_BACK);
    verify(store).delete(inserted.getValue().input().objectKey());
  }

  @Test
  void createRejectsInvalidVisibilityCategoryAndEmptyContent() {
    assertThatThrownBy(() -> handler.create(create("PRIVATE", "1", "2", null)))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> handler.create(create("HIDDEN", "1", "2", "WEIRD")))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> handler.create(create("HIDDEN", null, "2", null)))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> handler.create(create("HIDDEN", "1", "", null)))
        .isInstanceOf(InvalidRequestException.class);
    verify(testcases, never()).insert(any(), any());
  }

  @Test
  void createRejectsContentOverOneMegabyteWithPrb048() {
    String huge = "y".repeat(1_048_577);

    assertThatThrownBy(() -> handler.create(create("HIDDEN", huge, "2", null)))
        .isInstanceOf(DomainRuleViolationException.class)
        .hasMessageContaining("1 MB");
  }

  @Test
  void createRejectsAtFiveHundredTestcases() {
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(500, 500, 3));

    assertThatThrownBy(() -> handler.create(create("HIDDEN", "1", "2", null)))
        .isInstanceOf(DomainRuleViolationException.class)
        .hasMessageContaining("500");
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(499, 499, 3));
    assertThat(handler.create(create("HIDDEN", "1", "2", null))).isNotNull();
  }

  @Test
  void anotherAuthorsDraftIsInvisibleButTheirPublishedProblemIsForbidden() {
    author = UUID.randomUUID();
    assertThatThrownBy(() -> handler.create(create("HIDDEN", "1", "2", null)))
        .isInstanceOf(ResourceNotFoundException.class);

    status = "PUBLISHED";
    assertThatThrownBy(() -> handler.create(create("HIDDEN", "1", "2", null)))
        .isInstanceOf(PermissionDeniedException.class);
  }

  @Test
  void adminMayAuthorTestcasesOfAnyProblemAndMissingProblemIsNotFound() {
    author = UUID.randomUUID();
    when(testcases.touch(problemId, actor)).thenReturn(new ProblemTouch(STAMP, 1));
    var admin = new AuthoringScope(actor, true);

    assertThat(
            handler.create(new TestcaseCommands.Create(admin, problemId, "HIDDEN", "1", "2", null)))
        .isNotNull();

    UUID missing = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                handler.create(
                    new TestcaseCommands.Create(scope, missing, "HIDDEN", "1", "2", null)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // ---- update ----

  @Test
  void updateOfMissingTestcaseIsNotFound() {
    assertThatThrownBy(() -> handler.update(update(patch("SAMPLE", null, null, null))))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void anApprovedTestcaseCannotBeUnapproved() {
    existing(
        stored(true, "HIDDEN", new Content("INLINE", "a", null), new Content("INLINE", "b", null)));

    assertThatThrownBy(() -> handler.update(update(patch(null, null, null, false))))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("unapproved");
  }

  @Test
  void updateInvalidVisibilityIsRejected() {
    existing(
        stored(true, "HIDDEN", new Content("INLINE", "a", null), new Content("INLINE", "b", null)));

    assertThatThrownBy(() -> handler.update(update(patch("OTHER", null, null, null))))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void updateReplacesContentCategoryAndApprovalAndDiscardsOldObjectsOnCommit() {
    existing(
        stored(
            false,
            "HIDDEN",
            new Content("MINIO", null, "old/in"),
            new Content("MINIO", null, "old/out")));
    var change = new TestcaseCommands.Patch("SAMPLE", "new in", "new out", true, "EXTREME", true);

    var saved = handler.update(update(change));

    var updated = ArgumentCaptor.forClass(StoredTestcase.class);
    verify(testcases).update(updated.capture());
    var row = updated.getValue();
    assertThat(row.visibility()).isEqualTo("SAMPLE");
    assertThat(row.approved()).isTrue();
    assertThat(row.category()).isEqualTo("EXTREME");
    assertThat(row.input().inline()).isEqualTo("new in");
    assertThat(row.position()).isEqualTo(2);
    assertThat(saved.currentTestcaseSetVersion()).isEqualTo(7);
    runCallbacks(TransactionSynchronization.STATUS_COMMITTED);
    verify(store).delete("old/in");
    verify(store).delete("old/out");
  }

  @Test
  void updateKeepsStoredContentWhenSidesAreAbsentAndClearsCategoryWhenPresentAsNull() {
    var in = new Content("INLINE", "a", null);
    var out = new Content("INLINE", "b", null);
    existing(stored(false, "HIDDEN", in, out));

    handler.update(update(new TestcaseCommands.Patch(null, null, null, true, null, null)));

    var updated = ArgumentCaptor.forClass(StoredTestcase.class);
    verify(testcases).update(updated.capture());
    assertThat(updated.getValue().category()).isNull();
    assertThat(updated.getValue().input()).isEqualTo(in);
    assertThat(updated.getValue().expectedOutput()).isEqualTo(out);
    assertThat(updated.getValue().approved()).isFalse();
  }

  @Test
  void updateOfUnapprovedTestcaseDoesNotRepublishButApprovedChangeDoes() {
    status = "PUBLISHED";
    existing(
        stored(
            false, "HIDDEN", new Content("INLINE", "a", null), new Content("INLINE", "b", null)));
    handler.update(update(patch(null, "changed", null, null)));
    verify(snapshots, never()).publishSnapshot(any());

    existing(
        stored(true, "HIDDEN", new Content("INLINE", "a", null), new Content("INLINE", "b", null)));
    handler.update(update(patch(null, "changed", null, null)));
    verify(snapshots).publishSnapshot(problemId);
  }

  @Test
  void demotingTheLastRequiredSampleOfAPublishedProblemIsRefusedWithPrb054() {
    status = "PUBLISHED";
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(10, 10, 2));
    existing(
        stored(true, "SAMPLE", new Content("INLINE", "a", null), new Content("INLINE", "b", null)));

    assertThatThrownBy(() -> handler.update(update(patch("HIDDEN", null, null, null))))
        .isInstanceOf(DomainRuleViolationException.class)
        .hasMessageContaining("minimum");
    verify(testcases, never()).update(any());
  }

  @Test
  void demotingASampleOfAnUnpublishedProblemIsAllowed() {
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(10, 10, 2));
    existing(
        stored(true, "SAMPLE", new Content("INLINE", "a", null), new Content("INLINE", "b", null)));

    handler.update(update(patch("HIDDEN", null, null, null)));

    verify(testcases).update(any());
  }

  // ---- delete ----

  @Test
  void deleteOfMissingTestcaseIsNotFound() {
    assertThatThrownBy(
            () -> handler.delete(new TestcaseCommands.Delete(scope, problemId, testcaseId)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteRemovesRowAndObjectsAfterCommitAndRepublishesApprovedChange() {
    status = "PUBLISHED";
    existing(
        stored(
            true, "HIDDEN", new Content("MINIO", null, "k/in"), new Content("INLINE", "b", null)));

    var removed = handler.delete(new TestcaseCommands.Delete(scope, problemId, testcaseId));

    verify(testcases).delete(testcaseId);
    verify(snapshots).publishSnapshot(problemId);
    assertThat(removed.currentTestcaseSetVersion()).isEqualTo(7);
    runCallbacks(TransactionSynchronization.STATUS_COMMITTED);
    verify(store).delete("k/in");
  }

  @Test
  void deletingBelowTheChecklistMinimumOfAPublishedProblemIsRefused() {
    status = "PUBLISHED";
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(8, 8, 2));
    existing(
        stored(true, "HIDDEN", new Content("INLINE", "a", null), new Content("INLINE", "b", null)));

    assertThatThrownBy(
            () -> handler.delete(new TestcaseCommands.Delete(scope, problemId, testcaseId)))
        .isInstanceOf(DomainRuleViolationException.class);
    verify(testcases, never()).delete(any());
  }

  @Test
  void deletingAnUnapprovedTestcaseNeverChecksMinimumsNorRepublishes() {
    status = "PUBLISHED";
    existing(
        stored(
            false, "HIDDEN", new Content("INLINE", "a", null), new Content("INLINE", "b", null)));

    handler.delete(new TestcaseCommands.Delete(scope, problemId, testcaseId));

    verify(testcases, never()).counts(any());
    verify(snapshots, never()).publishSnapshot(any());
  }

  @Test
  void deletingASampleCountsItAgainstTheSampleMinimum() {
    status = "PUBLISHED";
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(20, 20, 2));
    existing(
        stored(true, "SAMPLE", new Content("INLINE", "a", null), new Content("INLINE", "b", null)));

    assertThatThrownBy(
            () -> handler.delete(new TestcaseCommands.Delete(scope, problemId, testcaseId)))
        .isInstanceOf(DomainRuleViolationException.class);
  }

  // ---- reorder ----

  @Test
  void reorderRejectsNullListAndNullElements() {
    assertThatThrownBy(() -> handler.reorder(new TestcaseCommands.Reorder(scope, problemId, null)))
        .isInstanceOf(InvalidRequestException.class);
    var withNull = new java.util.ArrayList<UUID>();
    withNull.add(null);
    assertThatThrownBy(
            () -> handler.reorder(new TestcaseCommands.Reorder(scope, problemId, withNull)))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void reorderMustContainEveryTestcaseExactlyOnce() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    when(testcases.lockOrder(problemId)).thenReturn(List.of(a, b));

    assertThatThrownBy(
            () -> handler.reorder(new TestcaseCommands.Reorder(scope, problemId, List.of(a, a))))
        .isInstanceOf(DomainRuleViolationException.class);
    assertThatThrownBy(
            () -> handler.reorder(new TestcaseCommands.Reorder(scope, problemId, List.of(a))))
        .isInstanceOf(DomainRuleViolationException.class);
    assertThatThrownBy(
            () ->
                handler.reorder(
                    new TestcaseCommands.Reorder(
                        scope, problemId, List.of(a, b, UUID.randomUUID()))))
        .isInstanceOf(DomainRuleViolationException.class);
    verify(testcases, never()).setPositions(any());
  }

  @Test
  void reorderToTheCurrentOrderWritesNothingAndReturnsStoredVersion() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    when(testcases.lockOrder(problemId)).thenReturn(List.of(a, b));
    when(testcases.state(problemId)).thenReturn(new ProblemTouch(NOW, 4));

    var result = handler.reorder(new TestcaseCommands.Reorder(scope, problemId, List.of(a, b)));

    assertThat(result.currentTestcaseSetVersion()).isEqualTo(4);
    assertThat(result.problemUpdatedAt()).isEqualTo(NOW);
    verify(testcases, never()).setPositions(any());
    verify(testcases, never()).touch(any(), any());
  }

  @Test
  void reorderWritesNewPositionsAndRepublishesOnPublishedProblem() {
    status = "PUBLISHED";
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    when(testcases.lockOrder(problemId)).thenReturn(List.of(a, b));

    var result = handler.reorder(new TestcaseCommands.Reorder(scope, problemId, List.of(b, a)));

    verify(testcases).setPositions(List.of(b, a));
    verify(snapshots).publishSnapshot(problemId);
    assertThat(result.items()).extracting("id").containsExactly(b, a);
    assertThat(result.items()).extracting("position").containsExactly(1, 2);
    assertThat(result.currentTestcaseSetVersion()).isEqualTo(7);
  }
}
