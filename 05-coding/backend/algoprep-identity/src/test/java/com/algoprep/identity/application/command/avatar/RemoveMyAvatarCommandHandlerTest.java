package com.algoprep.identity.application.command.avatar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import com.algoprep.identity.application.ports.out.AvatarRateLimitPort;
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

/** BR-26 #75: shared hourly limit, row first, object after commit, no avatar is a no-op. */
class RemoveMyAvatarCommandHandlerTest {
  private final AvatarRateLimitPort limiter = mock(AvatarRateLimitPort.class);
  private final AvatarObjectStorePort store = mock(AvatarObjectStorePort.class);
  private final UserAvatarRepository avatars = mock(UserAvatarRepository.class);
  private final RemoveMyAvatarCommandHandler handler =
      new RemoveMyAvatarCommandHandler(limiter, avatars, new AvatarObjectCleanup(store));
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void startTransaction() {
    TransactionSynchronizationManager.initSynchronization();
    when(limiter.tryAcquire(userId)).thenReturn(true);
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
  void theRowIsDeletedAndTheObjectGoesAfterCommit() {
    when(avatars.delete(userId))
        .thenReturn(
            Optional.of(new AvatarRecord(userId, "users/x/a.png", "image/png", 5, Instant.EPOCH)));

    handler.remove(userId);
    verify(store, never()).delete(anyString());
    commit();

    verify(store).delete("users/x/a.png");
  }

  @Test
  void noAvatarIsAQuietNoOpWithoutStorageCalls() {
    when(avatars.delete(userId)).thenReturn(Optional.empty());

    handler.remove(userId);
    commit();

    verify(store, never()).delete(anyString());
  }

  @Test
  void overTheLimitIs429AndNothingIsDeleted() {
    when(limiter.tryAcquire(userId)).thenReturn(false);

    assertThatThrownBy(() -> handler.remove(userId))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-452"));

    verify(avatars, never()).delete(any());
  }
}
