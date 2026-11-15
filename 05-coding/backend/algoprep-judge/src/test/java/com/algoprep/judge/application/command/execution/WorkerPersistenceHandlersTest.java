package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.model.WorkerRecords.Result;
import com.algoprep.judge.domain.model.WorkerRecords.State;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.ports.out.SubmissionWorkerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class WorkerPersistenceHandlersTest {
  private final SubmissionWorkerRepository repo = mock(SubmissionWorkerRepository.class);
  private final SubmissionProgressPort progress = mock(SubmissionProgressPort.class);
  private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
  private final IdGeneratorPort ids = UUID::randomUUID;
  private final WorkerTransactions transactions = new WorkerTransactions(manager);
  private final State state = SubmissionFinalizationPolicyTest.state("JUDGING", 1);

  WorkerPersistenceHandlersTest() {
    when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
  }

  @Test
  void transitionsNotifyOnlyWhenTheirGuardedUpdateWins() {
    var handler = new WorkerStateCommandHandler(repo, progress, transactions);
    when(repo.compiling(any(), anyInt())).thenReturn(Optional.empty());
    assertThat(handler.begin(state.id(), 1)).isEmpty();
    verifyNoInteractions(progress);
    var compiling = SubmissionFinalizationPolicyTest.state("COMPILING", null);
    when(repo.compiling(any(), anyInt())).thenReturn(Optional.of(compiling));
    assertThat(handler.begin(state.id(), 1)).contains(compiling);
    when(repo.judging(any(), anyInt())).thenReturn(Optional.of(state));
    assertThat(handler.beginJudging(state.id(), 1)).contains(state);
    assertThatThrownBy(() -> handler.begin(state.id(), 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> handler.beginJudging(state.id(), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tokenIssuanceAndResultPersistenceRespectStateAndDuplicateGuards() {
    var handler = new WorkerTestcaseCommandHandler(repo, progress, transactions, ids);
    when(repo.heartbeat(any())).thenReturn(Optional.empty());
    UUID testcase = UUID.randomUUID();
    assertThat(handler.issue(state.id(), testcase)).isEmpty();
    when(repo.heartbeat(any())).thenReturn(Optional.of(state));
    when(repo.issue(any(), any(), any())).thenReturn(true);
    assertThat(handler.issue(state.id(), testcase)).isPresent();
    var write =
        new TestcaseWrite(
            state.id(), testcase, UUID.randomUUID(), 1, false, "AC", 1, 1, null, null, false);
    when(repo.results(state.id())).thenReturn(List.of());
    assertThat(handler.record(write)).isTrue();
    verify(repo)
        .save(
            org.mockito.ArgumentMatchers.eq(write), org.mockito.ArgumentMatchers.eq(state), any());
    when(repo.resultExists(state.id(), testcase)).thenReturn(true);
    assertThat(handler.record(write)).isFalse();
    assertThat(handler.issue(state.id(), testcase)).isEmpty();
  }

  @Test
  void invalidOrderAndDatabaseErrorsHaveNoSensitiveCause() {
    var handler = new WorkerTestcaseCommandHandler(repo, progress, transactions, ids);
    when(repo.heartbeat(any())).thenReturn(Optional.of(state));
    when(repo.results(any())).thenReturn(List.of(new Result(1, "WA", 1, 1)));
    var write =
        new TestcaseWrite(
            state.id(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            false,
            "AC",
            1,
            1,
            null,
            null,
            false);
    assertThatThrownBy(() -> handler.record(write))
        .isInstanceOf(WorkerPersistenceException.class)
        .hasNoCause();
    when(repo.heartbeat(any())).thenThrow(new IllegalStateException("SQL private"));
    assertThatThrownBy(() -> handler.issue(state.id(), UUID.randomUUID()))
        .isInstanceOf(WorkerPersistenceException.class)
        .hasNoCause()
        .hasMessage("Worker persistence is unavailable");
    verifyNoInteractions(progress);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ACCEPTED",
        "WRONG_ANSWER",
        "TIME_LIMIT_EXCEEDED",
        "RUNTIME_ERROR",
        "COMPILE_ERROR",
        "SYSTEM_ERROR"
      })
  void terminalWinnerCannotGenerateAnotherEventOrPush(String status) {
    var handler = new FinalizeSubmissionCommandHandler(repo, progress, transactions, ids);
    when(repo.lock(any()))
        .thenReturn(Optional.of(SubmissionFinalizationPolicyTest.state(status, 1)));
    assertThat(
            handler.finish(
                new FinishCommand(state.id(), FinishKind.SYSTEM_ERROR, "WORKER_TIMEOUT", null)))
        .isEmpty();
    verifyNoInteractions(progress);
  }

  @Test
  void finalizationUsesPersistedResultsAndEmitsBothAcceptedAndGradedEvents() {
    var handler = new FinalizeSubmissionCommandHandler(repo, progress, transactions, ids);
    when(repo.lock(state.id())).thenReturn(Optional.of(state));
    when(repo.results(state.id())).thenReturn(List.of(new Result(1, "AC", 1, 1)));
    var saved =
        new State(
            state.id(),
            state.owner(),
            state.language(),
            state.model(),
            "ACCEPTED",
            1,
            1,
            1,
            1,
            state.submittedAt(),
            state.updatedAt());
    when(repo.finish(any(), any())).thenReturn(saved);
    assertThat(handler.finish(new FinishCommand(state.id(), FinishKind.COMPLETE, null, null)))
        .isPresent();
    verify(repo)
        .append(
            any(),
            org.mockito.ArgumentMatchers.eq(saved),
            org.mockito.ArgumentMatchers.eq("SubmissionAccepted"),
            any());
    verify(repo)
        .append(
            any(),
            org.mockito.ArgumentMatchers.eq(saved),
            org.mockito.ArgumentMatchers.eq("SubmissionGraded"),
            any());
    verify(progress).afterCommit(org.mockito.ArgumentMatchers.eq(state.owner().userId()), any());
  }

  @Test
  void missingSubmissionAndCommitFailureAreSafe() {
    var handler = new FinalizeSubmissionCommandHandler(repo, progress, transactions, ids);
    when(repo.lock(any())).thenReturn(Optional.empty());
    assertThat(
            handler.finish(
                new FinishCommand(state.id(), FinishKind.SYSTEM_ERROR, "SANDBOX_FAULT", null)))
        .isEmpty();
    doThrow(new IllegalStateException("SQL private")).when(manager).commit(any());
    assertThatThrownBy(
            () ->
                handler.finish(
                    new FinishCommand(state.id(), FinishKind.SYSTEM_ERROR, "SANDBOX_FAULT", null)))
        .isInstanceOf(WorkerPersistenceException.class)
        .hasNoCause();
  }
}
