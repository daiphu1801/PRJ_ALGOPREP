package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.QueuePauseChange;
import com.algoprep.judge.domain.model.WorkerRecords.Finished;
import com.algoprep.judge.domain.ports.out.JudgeMaintenanceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JudgeMaintenanceHandlersTest {
  private final JudgeMaintenanceRepository repository = mock(JudgeMaintenanceRepository.class);
  private final WorkerTransactions transactions = mock(WorkerTransactions.class);
  private final FinalizeSubmissionCommandHandler finalizer =
      mock(FinalizeSubmissionCommandHandler.class);

  @BeforeEach
  void transactionsExecuteTheirOperation() {
    when(transactions.execute(any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(0)).get());
  }

  @Test
  void sweepRechecksCandidatesAndUsesTheSharedFinalizer() {
    UUID stale = UUID.randomUUID();
    UUID live = UUID.randomUUID();
    UUID terminal = UUID.randomUUID();
    when(repository.staleCandidates(200, 300)).thenReturn(List.of(stale, live, terminal));
    when(repository.claimStale(stale, 300)).thenReturn(true);
    when(repository.claimStale(terminal, 300)).thenReturn(true);
    when(finalizer.finishLocked(any()))
        .thenReturn(Optional.of(mock(Finished.class)), Optional.empty());
    assertThat(
            new SweepStaleSubmissionsCommandHandler(repository, transactions, finalizer, 200, 300)
                .sweep())
        .isEqualTo(1);
    verify(finalizer)
        .finishLocked(
            new com.algoprep.judge.domain.model.WorkerRecords.FinishCommand(
                stale,
                com.algoprep.judge.domain.model.WorkerRecords.FinishKind.SYSTEM_ERROR,
                "WORKER_TIMEOUT",
                null));
  }

  @Test
  void cleanupCommitsEveryBatchAndStopsAfterTheShortBatch() {
    when(repository.tryJobLock("JOB-JD-03")).thenReturn(true);
    when(repository.deletePublished(90, 2)).thenReturn(2, 2, 1);
    assertThat(new PurgePublishedJudgeEventsCommandHandler(repository, transactions, 90, 2).purge())
        .isEqualTo(5);
    when(repository.tryJobLock("JOB-JD-04")).thenReturn(true);
    when(repository.deleteTestcaseDetails(90, 2)).thenReturn(2, 0);
    assertThat(new PurgeTestcaseDetailsCommandHandler(repository, transactions, 90, 2).purge())
        .isEqualTo(2);
  }

  @Test
  void cleanupSkipsWhenAnotherTransactionOwnsTheJob() {
    assertThat(
            new PurgePublishedJudgeEventsCommandHandler(repository, transactions, 90, 5000).purge())
        .isZero();
    assertThat(new PurgeTestcaseDetailsCommandHandler(repository, transactions, 90, 5000).purge())
        .isZero();
    verify(repository, never()).deletePublished(90, 5000);
    verify(repository, never()).deleteTestcaseDetails(90, 5000);
  }

  @Test
  void resumeRefreshesOnlyOnARealPausedToOpenTransition() {
    var handler = new ChangeQueuePauseCommandHandler(repository);
    when(repository.lockQueuePaused()).thenReturn(true, false, false);
    assertThat(handler.change(true, false)).isEqualTo(QueuePauseChange.CHANGED);
    assertThat(handler.change(false, false)).isEqualTo(QueuePauseChange.UNCHANGED);
    assertThat(handler.change(true, false)).isEqualTo(QueuePauseChange.CONFLICT);
    verify(repository).refreshPendingHeartbeat();
    verify(repository).changeQueuePaused(false);
  }

  @Test
  void pauseDoesNotRewritePendingHeartbeat() {
    assertThat(new ChangeQueuePauseCommandHandler(repository).change(false, true))
        .isEqualTo(QueuePauseChange.CHANGED);
    verify(repository, never()).refreshPendingHeartbeat();
  }

  @Test
  void invalidConfigurationIsRejectedBeforeScheduling() {
    assertThatThrownBy(
            () ->
                new SweepStaleSubmissionsCommandHandler(
                    repository, transactions, finalizer, 0, 300))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new SweepStaleSubmissionsCommandHandler(
                    repository, transactions, finalizer, 201, 300))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new SweepStaleSubmissionsCommandHandler(
                    repository, transactions, finalizer, 200, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> JudgeRetention.validate(0, 5000))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> JudgeRetention.validate(90, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> JudgeRetention.validate(90, 5001))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
