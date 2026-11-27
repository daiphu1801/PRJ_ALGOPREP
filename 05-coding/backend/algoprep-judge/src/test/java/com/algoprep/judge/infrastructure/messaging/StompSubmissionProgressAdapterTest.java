package com.algoprep.judge.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.judge.application.dto.SubmissionStatusProgress;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class StompSubmissionProgressAdapterTest {
  private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
  private final StompSubmissionProgressAdapter adapter =
      new StompSubmissionProgressAdapter(messaging);
  private final UUID owner = UUID.randomUUID();
  private final SubmissionStatusProgress progress =
      new SubmissionStatusProgress(UUID.randomUUID(), Instant.now(), "COMPILING", null);

  @AfterEach
  void clearTransaction() {
    TransactionSynchronizationManager.clear();
  }

  @Test
  void sendsToOnlyTheTrustedOwnerWhenAlreadyCommitted() {
    adapter.afterCommit(owner, progress);
    verify(messaging)
        .convertAndSendToUser(
            eq(owner.toString()), eq("/queue/submissions"), same(progress), anyMap());
  }

  @Test
  void defersUntilSuccessfulCommit() {
    begin();
    adapter.afterCommit(owner, progress);
    verifyNoInteractions(messaging);
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
    verify(messaging)
        .convertAndSendToUser(
            eq(owner.toString()), eq("/queue/submissions"), same(progress), anyMap());
  }

  @Test
  void rollbackDoesNotPublish() {
    begin();
    adapter.afterCommit(owner, progress);
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    verifyNoInteractions(messaging);
  }

  @Test
  void deliveryFailureNeverEscapesIncludingAfterCommit() {
    doThrow(new IllegalStateException("Sensitive transport failure"))
        .when(messaging)
        .convertAndSendToUser(
            eq(owner.toString()), eq("/queue/submissions"), same(progress), anyMap());
    assertThatCode(() -> adapter.afterCommit(owner, progress)).doesNotThrowAnyException();
    begin();
    adapter.afterCommit(owner, progress);
    assertThatCode(
            () ->
                TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit))
        .doesNotThrowAnyException();
  }

  private static void begin() {
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
  }
}
