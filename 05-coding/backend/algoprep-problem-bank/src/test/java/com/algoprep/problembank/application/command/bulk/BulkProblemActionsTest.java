package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.UpdateProblemStatusCommand;
import com.algoprep.problembank.application.dto.UpdateProblemStatusResult;
import com.algoprep.problembank.application.ports.in.UpdateProblemPublishStatusUseCase;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.exception.PublishChecklistFailedException;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import com.algoprep.problembank.domain.ports.out.ProblemBulkRepository;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BulkProblemActionsTest {
  private final ProblemContentRepository content = mock(ProblemContentRepository.class);
  private final ProblemBulkRepository bulk = mock(ProblemBulkRepository.class);
  private final ProblemAuditPort audit = mock(ProblemAuditPort.class);
  private final ProblemListVersionPort cache = mock(ProblemListVersionPort.class);
  private final StatusChangeRecorder recorder = mock(StatusChangeRecorder.class);
  private final BulkProblemItemExecutor executor =
      new BulkProblemItemExecutor(content, bulk, audit, cache, recorder);
  private final UpdateProblemPublishStatusUseCase status =
      mock(UpdateProblemPublishStatusUseCase.class);
  private final ProblemAuthoringReadRepository reads = mock(ProblemAuthoringReadRepository.class);
  private final BulkProblemItemExecutor executorMock = mock(BulkProblemItemExecutor.class);
  private final BulkProblemCommandHandler handler =
      new BulkProblemCommandHandler(executorMock, status, reads, bulk);

  private final UUID actor = UUID.randomUUID();
  private final UUID author = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final UUID level = UUID.randomUUID();
  private final UUID otherLevel = UUID.randomUUID();
  private final AuthoringScope teacher = new AuthoringScope(actor, false);
  private final AuthoringScope admin = new AuthoringScope(actor, true);
  private final Instant stamp = Instant.parse("2026-11-05T01:00:00Z");

  // ---- orchestration

  private static String code(Throwable t) {
    return ((AlgoPrepException) t).errorCode();
  }

  @Test
  void aBatchOfNothingOrTooManyIsRefusedBeforeAnyWork() {
    var over = new ArrayList<UUID>();
    for (int i = 0; i < 201; i++) {
      over.add(UUID.randomUUID());
    }
    for (var call :
        List.<Runnable>of(
            () -> handler.delete(teacher, List.of()),
            () -> handler.delete(teacher, null),
            () -> handler.delete(teacher, over),
            () -> handler.unpublish(teacher, over),
            () -> handler.publish(teacher, over.subList(0, 21)))) {
      assertThatThrownBy(call::run).satisfies(e -> assertThat(code(e)).isEqualTo("PRB-001"));
    }
    verifyNoInteractions(executorMock, status);
  }

  @Test
  void publishTakesTwentyIdsAndTheOthersTwoHundred() {
    var twenty = new ArrayList<UUID>();
    for (int i = 0; i < 20; i++) {
      twenty.add(UUID.randomUUID());
    }
    for (UUID id : twenty) {
      when(reads.find(id, false)).thenReturn(Optional.of(record(id)));
    }
    assertThat(handler.publish(teacher, twenty).succeededIds()).hasSize(20);

    var twoHundred = new ArrayList<UUID>();
    for (int i = 0; i < 200; i++) {
      twoHundred.add(UUID.randomUUID());
    }
    assertThat(handler.delete(teacher, twoHundred).succeededIds()).hasSize(200);
  }

  @Test
  void oneRefusedProblemNeverStopsTheOthersAndCarriesItsErrorDetails() {
    UUID good = UUID.randomUUID();
    UUID bad = UUID.randomUUID();
    UUID crash = UUID.randomUUID();
    org.mockito.Mockito.doThrow(new PublishChecklistFailedException(List.of(Map.of("code", "X"))))
        .when(executorMock)
        .delete(teacher, bad);
    org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
        .when(executorMock)
        .delete(teacher, crash);

    var result = handler.delete(teacher, List.of(good, bad, crash, good));

    assertThat(result.succeededIds()).containsExactly(good); // the repeated id ran once
    assertThat(result.failedItems()).hasSize(2);
    var refused = result.failedItems().get(0);
    assertThat(refused.id()).isEqualTo(bad);
    assertThat(refused.code()).isEqualTo("PRB-030");
    assertThat(refused.details()).containsKey("items");
    var unexpected = result.failedItems().get(1);
    assertThat(unexpected.code()).isEqualTo("PRB-050");
    assertThat(unexpected.message()).doesNotContain("db down");
  }

  @Test
  void publishAndUnpublishReuseTheSingleProblemUseCaseWithTheCurrentTimestamp() {
    when(reads.find(problem, false)).thenReturn(Optional.of(record(problem)));
    when(status.update(any())).thenReturn(mock(UpdateProblemStatusResult.class));

    handler.publish(admin, List.of(problem));
    handler.unpublish(admin, List.of(problem));

    var sent = ArgumentCaptor.forClass(UpdateProblemStatusCommand.class);
    verify(status, org.mockito.Mockito.times(2)).update(sent.capture());
    assertThat(sent.getAllValues())
        .extracting(UpdateProblemStatusCommand::status)
        .containsExactly("PUBLISHED", "UNPUBLISHED");
    assertThat(sent.getAllValues())
        .allSatisfy(
            c -> {
              assertThat(c.expectedUpdatedAt()).isEqualTo(stamp);
              assertThat(c.admin()).isTrue();
              assertThat(c.actorId()).isEqualTo(actor);
            });
  }

  @Test
  void aMissingProblemIsAFailedItemNotAnError() {
    when(reads.find(problem, false)).thenReturn(Optional.empty());

    var result = handler.publish(teacher, List.of(problem));

    assertThat(result.succeededIds()).isEmpty();
    assertThat(result.failedItems().get(0).code()).isEqualTo("PRB-010");
    verifyNoInteractions(status);
  }

  @Test
  void anUnknownLevelOrTopicRefusesTheWholeBatchBeforeTouchingAnyProblem() {
    when(bulk.levelExists(level)).thenReturn(false);
    when(bulk.topicExists(level)).thenReturn(false);

    assertThatThrownBy(() -> handler.changeDifficulty(teacher, List.of(problem), level))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-039"));
    assertThatThrownBy(() -> handler.assignTopic(teacher, List.of(problem), level))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-039"));
    assertThatThrownBy(() -> handler.changeDifficulty(teacher, List.of(problem), null))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-001"));
    assertThatThrownBy(() -> handler.assignTopic(teacher, List.of(problem), null))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-001"));
    verifyNoInteractions(executorMock);
  }

  // ---- one problem

  private ProblemContentSnapshot locked(String status, UUID authorId, UUID levelId) {
    var data =
        new ProblemContentData(
            "T",
            "s",
            "",
            levelId,
            new ProblemContentData.Limits(1, 1, 1, null),
            List.of(),
            List.of(),
            List.of(),
            null);
    var snapshot = new ProblemContentSnapshot(problem, "CODE", authorId, status, data, stamp);
    when(content.lock(problem)).thenReturn(Optional.of(snapshot));
    return snapshot;
  }

  @Test
  void anInstructorMeetsTheSameAuthorRulesAsASingleWrite() {
    when(content.lock(problem)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> executor.delete(teacher, problem))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-010"));

    locked("UNPUBLISHED", author, level);
    assertThatThrownBy(() -> executor.delete(teacher, problem))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-010"));

    locked("PUBLISHED", author, level);
    assertThatThrownBy(() -> executor.delete(teacher, problem))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-003"));
    verify(bulk, never()).markDeleted(any(), any());

    locked("UNPUBLISHED", author, level);
    when(bulk.markDeleted(problem, actor)).thenReturn(stamp);
    executor.delete(admin, problem); // an admin is not bound to authorship
    verify(bulk).markDeleted(problem, actor);
  }

  @Test
  void deletingWritesAuditAndCacheAndWithdrawsOnlyAPublishedProblem() {
    locked("PUBLISHED", actor, level);
    when(bulk.markDeleted(problem, actor)).thenReturn(stamp);

    executor.delete(teacher, problem);

    var entry = ArgumentCaptor.forClass(CatalogAuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("PROBLEM_DELETED");
    verify(recorder).withdrawnByDelete(problem, actor, stamp);
    verify(cache).incrementAfterCommit();

    org.mockito.Mockito.reset(recorder, audit, cache);
    locked("UNPUBLISHED", actor, level);
    executor.delete(teacher, problem);
    verify(recorder, never()).withdrawnByDelete(any(), any(), any());
    verify(audit).record(any());
  }

  @Test
  void aSameLevelIsASilentSuccessAndADifferentOneIsAuditedWithBeforeAndAfter() {
    locked("UNPUBLISHED", actor, level);
    when(bulk.levelExists(level)).thenReturn(true);
    when(bulk.levelExists(otherLevel)).thenReturn(true);

    executor.changeDifficulty(teacher, problem, level);
    verify(bulk, never()).setLevel(any(), any(), any());
    verifyNoInteractions(audit, cache);

    executor.changeDifficulty(teacher, problem, otherLevel);
    verify(bulk).setLevel(problem, actor, otherLevel);
    var entry = ArgumentCaptor.forClass(CatalogAuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("PROBLEM_DIFFICULTY_CHANGED");
    assertThat(entry.getValue().beforeJson()).contains(level.toString());
    assertThat(entry.getValue().afterJson()).contains(otherLevel.toString());
    verify(cache).incrementAfterCommit();
  }

  @Test
  void aLevelOrTopicThatVanishedMeanwhileFailsThatProblemWith422() {
    locked("UNPUBLISHED", actor, level);
    when(bulk.levelExists(otherLevel)).thenReturn(false);
    when(bulk.topicExists(otherLevel)).thenReturn(false);

    assertThatThrownBy(() -> executor.changeDifficulty(teacher, problem, otherLevel))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-039"));
    assertThatThrownBy(() -> executor.assignTopic(teacher, problem, otherLevel))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-039"));
    verify(bulk, never()).setLevel(any(), any(), any());
  }

  @Test
  void aTopicAlreadyThereChangesAndAuditsNothing() {
    locked("UNPUBLISHED", actor, level);
    when(bulk.topicExists(otherLevel)).thenReturn(true);
    when(bulk.addTopic(problem, actor, otherLevel)).thenReturn(false);

    executor.assignTopic(teacher, problem, otherLevel);
    verifyNoInteractions(audit, cache);

    when(bulk.addTopic(problem, actor, otherLevel)).thenReturn(true);
    executor.assignTopic(teacher, problem, otherLevel);
    var entry = ArgumentCaptor.forClass(CatalogAuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("PROBLEM_TOPIC_ASSIGNED");
    verify(cache).incrementAfterCommit();
  }

  @Test
  void anAuditFailureIs503Prb050SoThatProblemRollsBack() {
    locked("UNPUBLISHED", actor, level);
    when(bulk.markDeleted(problem, actor)).thenReturn(stamp);
    doThrow(new IllegalStateException("audit down")).when(audit).record(any());

    assertThatThrownBy(() -> executor.delete(teacher, problem))
        .satisfies(e -> assertThat(code(e)).isEqualTo("PRB-050"));
    verify(cache, never()).incrementAfterCommit();
  }

  @Test
  void errorDetailsStayNullWhenTheErrorCarriesNone() {
    org.mockito.Mockito.doThrow(
            new com.algoprep.common.exception.ResourceNotFoundException(
                "PRB-010", "Problem not found"))
        .when(executorMock)
        .delete(teacher, problem);

    var failed = handler.delete(teacher, List.of(problem)).failedItems().get(0);

    assertThat(failed.details()).isNull();
    assertThat(failed.message()).isEqualTo("Problem not found");
    assertThat(ClientErrorDetails.class).isNotNull();
  }

  private AuthoringProblemRecord record(UUID id) {
    return new AuthoringProblemRecord(
        id,
        "CODE",
        "T",
        "s",
        "",
        level,
        "UNPUBLISHED",
        true,
        new ProblemContentData.Limits(1, 1, 1, null),
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
        null,
        List.of(),
        List.of(),
        null);
  }
}
