package com.algoprep.identity.application.command.avatar;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Object deletes follow the caller's transaction and never turn a committed change into an error.
 */
class AvatarObjectCleanupTest {
  private final AvatarObjectStorePort store = mock(AvatarObjectStorePort.class);
  private final AvatarObjectCleanup cleanup = new AvatarObjectCleanup(store);

  @AfterEach
  void endTransaction() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private static void finish(int status) {
    List<TransactionSynchronization> callbacks =
        List.copyOf(TransactionSynchronizationManager.getSynchronizations());
    if (status == TransactionSynchronization.STATUS_COMMITTED) {
      callbacks.forEach(TransactionSynchronization::afterCommit);
    }
    callbacks.forEach(c -> c.afterCompletion(status));
  }

  @Test
  void afterCommitWaitsForTheCommitAndIsSkippedOnRollback() {
    TransactionSynchronizationManager.initSynchronization();
    cleanup.deleteAfterCommit("k1");
    verify(store, never()).delete("k1");
    finish(TransactionSynchronization.STATUS_ROLLED_BACK);
    verify(store, never()).delete("k1");

    TransactionSynchronizationManager.clearSynchronization();
    TransactionSynchronizationManager.initSynchronization();
    cleanup.deleteAfterCommit("k2");
    finish(TransactionSynchronization.STATUS_COMMITTED);
    verify(store).delete("k2");
  }

  @Test
  void onRollbackDeletesOnlyWhenTheTransactionDoesNotCommit() {
    TransactionSynchronizationManager.initSynchronization();
    cleanup.deleteOnRollback("k1");
    finish(TransactionSynchronization.STATUS_COMMITTED);
    verify(store, never()).delete("k1");

    TransactionSynchronizationManager.clearSynchronization();
    TransactionSynchronizationManager.initSynchronization();
    cleanup.deleteOnRollback("k2");
    finish(TransactionSynchronization.STATUS_ROLLED_BACK);
    verify(store).delete("k2");
  }

  @Test
  void withoutATransactionAfterCommitRunsAtOnceAndOnRollbackDoesNothing() {
    cleanup.deleteAfterCommit("k1");
    verify(store).delete("k1");
    cleanup.deleteOnRollback("k2");
    verify(store, never()).delete("k2");
  }

  @Test
  void aStorageFailureIsSwallowed() {
    doThrow(new ServiceUnavailableException("IDT-511", "down", null)).when(store).delete("k1");
    assertThatCode(() -> cleanup.deleteQuietly("k1")).doesNotThrowAnyException();
    TransactionSynchronizationManager.initSynchronization();
    cleanup.deleteAfterCommit("k1");
    assertThatCode(() -> finish(TransactionSynchronization.STATUS_COMMITTED))
        .doesNotThrowAnyException();
  }
}
