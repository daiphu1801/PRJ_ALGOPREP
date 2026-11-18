package com.algoprep.identity.application.command.avatar;

import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Best-effort object deletes tied to the caller's transaction (BR-25, BR-26). A storage failure is
 * logged with the object key and never thrown: an orphan object is accepted (BD storage F1 section
 * 4), a committed change turned into an error is not.
 */
@Service
class AvatarObjectCleanup {
  private static final Logger LOG = LoggerFactory.getLogger(AvatarObjectCleanup.class);

  private final AvatarObjectStorePort store;

  AvatarObjectCleanup(AvatarObjectStorePort store) {
    this.store = store;
  }

  /** Deletes once the transaction commits; at once when there is no transaction. */
  void deleteAfterCommit(String key) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      deleteQuietly(key);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            deleteQuietly(key);
          }
        });
  }

  /** Deletes when the transaction ends without a commit; nothing to do outside a transaction. */
  void deleteOnRollback(String key) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              deleteQuietly(key);
            }
          }
        });
  }

  void deleteQuietly(String key) {
    try {
      store.delete(key);
    } catch (RuntimeException failure) {
      LOG.warn("Avatar object {} was not deleted and is now orphaned", key, failure);
    }
  }
}
