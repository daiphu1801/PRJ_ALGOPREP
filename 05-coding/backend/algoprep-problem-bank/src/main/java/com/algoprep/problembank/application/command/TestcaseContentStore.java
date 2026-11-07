package com.algoprep.problembank.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * BR-12: a side of at most 8 KB is stored INLINE, a larger one in MinIO. An object written by a
 * call that then rolls back is removed again, and an object the committed row no longer points at
 * is removed after commit; both are best effort and only logged.
 */
@Component
class TestcaseContentStore {
  static final int INLINE_LIMIT_BYTES = 8192;
  static final int MAX_BYTES = 1_048_576;
  private static final Logger LOG = LoggerFactory.getLogger(TestcaseContentStore.class);

  private final TestcaseObjectStorePort store;
  private final IdGeneratorPort ids;

  TestcaseContentStore(TestcaseObjectStorePort store, IdGeneratorPort ids) {
    this.store = store;
    this.ids = ids;
  }

  UUID newTestcaseId() {
    return ids.newId();
  }

  static boolean tooLarge(String text) {
    return text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
  }

  /**
   * Stores one side. {@code side} is {@code input} or {@code expected_output}. A new object always
   * gets a fresh key so that a rolled-back rewrite can never damage the object the row still uses.
   */
  Content place(UUID problemId, UUID testcaseId, String side, String text) {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    if (bytes.length <= INLINE_LIMIT_BYTES) {
      return new Content("INLINE", text, null);
    }
    String key = newKey(problemId, testcaseId, side);
    try {
      store.put(key, bytes);
    } catch (RuntimeException unavailable) {
      throw new TestcaseStorageUnavailableException();
    }
    afterCompletion(key, false);
    return new Content("MINIO", null, key);
  }

  /**
   * Content of a side for a copy of a testcase in another problem. An INLINE side is shared as is;
   * a MINIO object is copied under a fresh key that the rollback of this call removes again.
   */
  Content copyOf(UUID problemId, UUID testcaseId, String side, Content source) {
    if (!"MINIO".equals(source.storage())) {
      return source;
    }
    String key = newKey(problemId, testcaseId, side);
    try {
      store.copy(source.objectKey(), key);
    } catch (RuntimeException unavailable) {
      throw new TestcaseStorageUnavailableException();
    }
    afterCompletion(key, false);
    return new Content("MINIO", null, key);
  }

  /** A fresh server-made object key; the bulk upload signs it before any row exists. */
  String newKey(UUID problemId, UUID testcaseId, String side) {
    return "problems/"
        + problemId
        + "/testcases/"
        + testcaseId
        + "/"
        + side
        + "-"
        + ids.newId()
        + ".txt";
  }

  /** Removes the object of a side the row no longer uses, once the change is committed. */
  void discardAfterCommit(Content old) {
    if ("MINIO".equals(old.storage()) && old.objectKey() != null) {
      afterCompletion(old.objectKey(), true);
    }
  }

  private void afterCompletion(String key, boolean onCommit) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            boolean committed = status == STATUS_COMMITTED;
            if (committed != onCommit) {
              return;
            }
            try {
              store.delete(key);
            } catch (RuntimeException failure) {
              LOG.warn("Testcase object cleanup failed for key {}", key);
            }
          }
        });
  }
}
