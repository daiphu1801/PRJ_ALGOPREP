package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.LogRetentionSettings;
import java.util.UUID;

/**
 * The two retention keys of {@code identity.system_settings} and the purge of expired audit rows.
 * This is the only writer of the retention keys (BD 1.13).
 */
public interface LogRetentionRepository {

  /** The stored settings; a missing row falls back to the seed default (90 days, DELETE). */
  LogRetentionSettings read();

  /** Like {@link #read()} but locks the rows until the caller's transaction ends. */
  LogRetentionSettings readForUpdate();

  /** Writes both keys in the caller's transaction, stamping {@code updated_by} with the actor. */
  void save(LogRetentionSettings settings, UUID actorUserId);

  /**
   * Deletes up to {@code limit} audit rows older than {@code retentionDays}, measured on the
   * database clock, in one atomic statement. Rows locked by another run are skipped.
   *
   * @return rows deleted
   */
  int purgeExpiredBatch(int retentionDays, int limit);
}
