package com.algoprep.identity.application.command.avatar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ServiceUnavailableException;
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
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** BR-25: limit, checks, object first, row second, previous object after commit. */
class UploadMyAvatarCommandHandlerTest {
  private final AvatarRateLimitPort limiter = mock(AvatarRateLimitPort.class);
  private final AvatarObjectStorePort store = mock(AvatarObjectStorePort.class);
  private final UserAvatarRepository avatars = mock(UserAvatarRepository.class);
  private final UploadMyAvatarCommandHandler handler =
      new UploadMyAvatarCommandHandler(limiter, store, avatars, new AvatarObjectCleanup(store));
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

  private static byte[] png() {
    byte[] d = new byte[40];
    byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(magic, 0, d, 0, 8);
    d[12] = 'I';
    d[13] = 'H';
    d[14] = 'D';
    d[15] = 'R';
    d[19] = 8;
    d[23] = 8;
    return d;
  }

  private static void commit() {
    List<TransactionSynchronization> callbacks =
        List.copyOf(TransactionSynchronizationManager.getSynchronizations());
    callbacks.forEach(TransactionSynchronization::afterCommit);
    callbacks.forEach(c -> c.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  private AvatarRecord row(String key, long millis) {
    return new AvatarRecord(userId, key, "image/png", 40, Instant.ofEpochMilli(millis));
  }

  @Test
  void aNewAvatarIsStoredBeforeTheRowAndNothingIsDeleted() {
    when(avatars.findForUpdate(userId)).thenReturn(Optional.empty());
    when(avatars.upsert(eq(userId), anyString(), eq("image/png"), eq(40)))
        .thenReturn(row("users/x/y.png", 1234L));

    long version = handler.upload(userId, png());
    commit();

    assertThat(version).isEqualTo(1234L);
    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    var order = inOrder(store, avatars);
    order.verify(store).put(key.capture(), any(), eq("image/png"));
    order.verify(avatars).upsert(eq(userId), eq(key.getValue()), eq("image/png"), eq(40));
    assertThat(key.getValue()).matches("users/" + userId + "/[0-9a-f-]{36}\\.png");
    verify(store, never()).delete(anyString());
  }

  @Test
  void aReplacedAvatarDeletesThePreviousObjectOnlyAfterCommit() {
    when(avatars.findForUpdate(userId)).thenReturn(Optional.of(row("users/x/old.png", 1000L)));
    when(avatars.upsert(any(), anyString(), anyString(), anyInt()))
        .thenReturn(row("users/x/new.png", 1001L));

    handler.upload(userId, png());
    verify(store, never()).delete(anyString());
    commit();

    verify(store).delete("users/x/old.png");
  }

  @Test
  void theHourlyLimitIs429AndNothingElseRuns() {
    when(limiter.tryAcquire(userId)).thenReturn(false);

    assertThatThrownBy(() -> handler.upload(userId, png()))
        .satisfies(e -> assertCode(e, "IDT-452"));

    verify(store, never()).put(anyString(), any(), anyString());
    verify(avatars, never()).upsert(any(), anyString(), anyString(), anyInt());
  }

  @Test
  void aBadFileIsRejectedBeforeStorageIsTouched() {
    assertThatThrownBy(() -> handler.upload(userId, "<svg/>".getBytes()))
        .satisfies(e -> assertCode(e, "IDT-V29"));
    assertThatThrownBy(() -> handler.upload(userId, new byte[600_000]))
        .satisfies(e -> assertCode(e, "IDT-V30"));

    verify(store, never()).put(anyString(), any(), anyString());
    verify(avatars, never()).findForUpdate(any());
  }

  @Test
  void aFailedStoreLeavesTheRowAlone() {
    doThrow(new ServiceUnavailableException("IDT-511", "down", null))
        .when(store)
        .put(anyString(), any(), anyString());

    assertThatThrownBy(() -> handler.upload(userId, png()))
        .satisfies(e -> assertCode(e, "IDT-511"));

    verify(avatars, never()).upsert(any(), anyString(), anyString(), anyInt());
    verify(store, never()).delete(anyString());
  }

  @Test
  void aDatabaseFailureDeletesTheNewObjectAndKeepsTheOldOne() {
    when(avatars.findForUpdate(userId)).thenReturn(Optional.of(row("users/x/old.png", 1L)));
    when(avatars.upsert(any(), anyString(), anyString(), anyInt()))
        .thenThrow(new IllegalStateException("db down"));

    assertThatThrownBy(() -> handler.upload(userId, png()))
        .isInstanceOf(IllegalStateException.class);

    ArgumentCaptor<String> put = ArgumentCaptor.forClass(String.class);
    verify(store).put(put.capture(), any(), anyString());
    verify(store).delete(put.getValue());
    verify(store, never()).delete("users/x/old.png");
  }

  @Test
  void aRolledBackTransactionDeletesTheNewObject() {
    when(avatars.findForUpdate(userId)).thenReturn(Optional.empty());
    when(avatars.upsert(any(), anyString(), anyString(), anyInt()))
        .thenReturn(row("users/x/new.png", 5L));

    handler.upload(userId, png());
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(c -> c.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

    ArgumentCaptor<String> put = ArgumentCaptor.forClass(String.class);
    verify(store).put(put.capture(), any(), anyString());
    verify(store).delete(put.getValue());
  }
}
