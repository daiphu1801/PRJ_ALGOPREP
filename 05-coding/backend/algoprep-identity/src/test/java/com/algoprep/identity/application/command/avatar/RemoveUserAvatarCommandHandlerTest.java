package com.algoprep.identity.application.command.avatar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import com.algoprep.identity.domain.model.admin.AdminUserItem;
import com.algoprep.identity.domain.model.audit.AuditEntry;
import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
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

/** BR-26 #77: the target must exist; a real removal is audited once in the same transaction. */
class RemoveUserAvatarCommandHandlerTest {
  private final AdminUserRepository users = mock(AdminUserRepository.class);
  private final UserAvatarRepository avatars = mock(UserAvatarRepository.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final AvatarObjectStorePort store = mock(AvatarObjectStorePort.class);
  private final RemoveUserAvatarCommandHandler handler =
      new RemoveUserAvatarCommandHandler(users, avatars, audit, new AvatarObjectCleanup(store));
  private final UUID actor = UUID.randomUUID();
  private final UUID target = UUID.randomUUID();

  @BeforeEach
  void startTransaction() {
    TransactionSynchronizationManager.initSynchronization();
    when(users.lock(target)).thenReturn(Optional.of(mock(AdminUserItem.class)));
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

  private AvatarRecord existing(String key) {
    return new AvatarRecord(target, key, "image/png", 9, Instant.EPOCH);
  }

  @Test
  void aRemovalAuditsExactlyOnceAndDeletesTheObjectAfterCommit() {
    when(avatars.delete(target)).thenReturn(Optional.of(existing("users/t/a.png")));

    handler.remove(actor, target);
    verify(store, never()).delete(anyString());
    commit();

    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    AuditEntry e = entry.getValue();
    assertThat(e.actorUserId()).isEqualTo(actor);
    assertThat(e.actionType()).isEqualTo("USER_AVATAR_REMOVED");
    assertThat(e.targetType()).isEqualTo("USER");
    assertThat(e.targetId()).isEqualTo(target.toString());
    assertThat(e.beforeJson()).isEqualTo("{\"hadAvatar\":true}");
    assertThat(e.afterJson()).isNull();
    verify(store).delete("users/t/a.png");
  }

  @Test
  void noAvatarMeansNoAuditAndNoStorageCall() {
    when(avatars.delete(target)).thenReturn(Optional.empty());

    handler.remove(actor, target);
    commit();

    verify(audit, never()).record(any());
    verify(store, never()).delete(anyString());
  }

  @Test
  void anUnknownTargetIs404Idt450AndNothingIsDeleted() {
    when(users.lock(target)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.remove(actor, target))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-450"));

    verify(avatars, never()).delete(any());
    verify(audit, never()).record(any());
  }

  @Test
  void aFailingAuditPropagatesSoTheTransactionRollsBackAndTheObjectStays() {
    when(avatars.delete(target)).thenReturn(Optional.of(existing("users/t/a.png")));
    doThrow(new IllegalStateException("audit down")).when(audit).record(any());

    assertThatThrownBy(() -> handler.remove(actor, target))
        .isInstanceOf(IllegalStateException.class);
    // A rolled back transaction never runs afterCommit, so the object must survive.
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(c -> c.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

    verify(store, never()).delete(anyString());
  }
}
