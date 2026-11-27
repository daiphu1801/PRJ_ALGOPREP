package com.algoprep.identity.application.command.avatar;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** BR-08 and BR-26: account deletion drops the row now and the object after commit. */
class DiscardAvatarCommandHandlerTest {
  private final UserAvatarRepository avatars = mock(UserAvatarRepository.class);
  private final AvatarObjectStorePort store = mock(AvatarObjectStorePort.class);
  private final DiscardAvatarCommandHandler handler =
      new DiscardAvatarCommandHandler(avatars, new AvatarObjectCleanup(store));
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void startTransaction() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void endTransaction() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private static void commit() {
    List<TransactionSynchronization> callbacks =
        List.copyOf(TransactionSynchronizationManager.getSynchronizations());
    callbacks.forEach(TransactionSynchronization::afterCommit);
  }

  @Test
  void theRowGoesAtOnceAndTheObjectAfterCommit() {
    when(avatars.delete(userId))
        .thenReturn(
            Optional.of(new AvatarRecord(userId, "users/u/a.png", "image/png", 1, Instant.EPOCH)));

    handler.discard(userId);
    verify(store, never()).delete(anyString());
    commit();

    verify(store).delete("users/u/a.png");
  }

  @Test
  void noAvatarDoesNothing() {
    when(avatars.delete(userId)).thenReturn(Optional.empty());

    handler.discard(userId);
    commit();

    verify(store, never()).delete(anyString());
  }
}
